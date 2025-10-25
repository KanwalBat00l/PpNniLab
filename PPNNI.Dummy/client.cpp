#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>
#include <arpa/inet.h>
#include <unistd.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <random>

// macOS does not provide htobe64/be64toh by default
#ifndef htobe64
#define htobe64(x) __builtin_bswap64(x)
#endif
#ifndef be64toh
#define be64toh(x) __builtin_bswap64(x)
#endif

int main(int argc, char** argv) {
    if(argc != 4) { std::cerr << "Usage: " << argv[0] << " <server_ip> <port> <image_file>\n"; return 1; }

    std::string server_ip = argv[1];
    int port = std::stoi(argv[2]);
    std::string img_file = argv[3];

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    sockaddr_in addr{}; addr.sin_family = AF_INET; addr.sin_port = htons(port);
    inet_pton(AF_INET, server_ip.c_str(), &addr.sin_addr);

    std::cout << "[Client] Connecting to " << server_ip << ":" << port << "...\n";
    if(connect(sock, (sockaddr*)&addr, sizeof(addr)) < 0) { perror("connect"); return 1; }
    std::cout << "[Client] Connected (non-secure)\n";

    write(sock, "HELLO", 5);
    char buf[16]; int n = read(sock, buf, sizeof(buf)-1); buf[n]='\0';
    std::cout << "[Client] Handshake reply: " << buf << "\n";

    // --- TLS setup ---
    SSL_library_init(); OpenSSL_add_all_algorithms(); SSL_load_error_strings();
    SSL_CTX* ctx = SSL_CTX_new(TLS_client_method());
    SSL_CTX_set_min_proto_version(ctx, TLS1_3_VERSION);
    SSL_CTX_set_max_proto_version(ctx, TLS1_3_VERSION);
    SSL* ssl = SSL_new(ctx); SSL_set_fd(ssl, sock);

    std::cout << "[Client] Starting TLS handshake...\n";
    
    int ret = SSL_connect(ssl); 
    if (ret <= 0) {
#if defined(__ANDROID__)
        // Android build: OpenSSL compiled with no-stdio → use BIO_new_fd()
        BIO *bio_out = BIO_new_fd(fileno(stdout), BIO_NOCLOSE);
#else
        // macOS or other platforms: standard OpenSSL build
        BIO *bio_out = BIO_new_fp(stdout, BIO_NOCLOSE);
#endif
        if (bio_out) {
            ERR_print_errors(bio_out);
            BIO_free(bio_out);
        }
        return 1;
    }
    //{ ERR_print_errors_fp(stderr); return 1; }
    std::cout << "[Client] TLS handshake done.\n";

    // --- read image ---
    std::ifstream file(img_file, std::ios::binary);
    std::vector<uint8_t> img((std::istreambuf_iterator<char>(file)), {});

    // --- additive secret sharing ---

    // generate shares as uint64_t
    std::vector<uint64_t> share1(img.size()), share2(img.size());
    std::mt19937_64 gen(std::random_device{}());
    std::uniform_int_distribution<uint64_t> dist(0, 255);

    for(size_t i = 0; i < img.size(); i++) {
        share1[i] = dist(gen);
        share2[i] = static_cast<uint64_t>(img[i]) - share1[i]; // no modulo!
    }


    // std::vector<uint8_t> share1(img.size()), share2(img.size());
    // std::random_device rd; std::mt19937 gen(rd());
    // std::uniform_int_distribution<int> dist(0,255);
    // for(size_t i=0;i<img.size();i++){
    //     share1[i] = dist(gen);
    //     share2[i] = (img[i] - share1[i] + 256) % 256;
    // }

    // --- send one share ---
    uint32_t net_size = htonl(share2.size());
    SSL_write(ssl, &net_size, sizeof(net_size));
    SSL_write(ssl, share2.data(), share2.size());
    std::cout << "[Client] Sent share (" << share2.size() << " bytes)\n";

    // --- receive server sum ---
    uint64_t net_sum; SSL_read(ssl, &net_sum, sizeof(net_sum));
    uint64_t sum_server = be64toh(net_sum);

    // --- compute client sum ---
    uint64_t sum_client = 0;
    for(uint8_t b : share1) sum_client += b;

    uint64_t avg_brightness = (sum_client + sum_server) / img.size();
    std::cout << "[Client] Average brightness (secure) = " << avg_brightness << "\n";

    SSL_shutdown(ssl); SSL_free(ssl); close(sock); SSL_CTX_free(ctx);
    return 0;
}



