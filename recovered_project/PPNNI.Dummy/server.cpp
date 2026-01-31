#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>
#include <arpa/inet.h>
#include <unistd.h>
#include <openssl/ssl.h>
#include <openssl/err.h>

// macOS does not provide htobe64/be64toh by default
#ifndef htobe64
#define htobe64(x) __builtin_bswap64(x)
#endif
#ifndef be64toh
#define be64toh(x) __builtin_bswap64(x)
#endif

// Additive Secret Sharing server-side computation
uint64_t compute_sum_share(const std::vector<uint8_t>& share) {
    uint64_t sum = 0;
    for(uint8_t b : share) sum += b;
    return sum;
}

int main(int argc, char** argv) {
    if(argc != 2) { std::cerr << "Usage: " << argv[0] << " <port>\n"; return 1; }
    int port = std::stoi(argv[1]);

    // --- non-secure socket ---
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    if(bind(sock, (sockaddr*)&addr, sizeof(addr)) < 0) { perror("bind"); return 1; }
    if(listen(sock, 1) < 0) { perror("listen"); return 1; }

    std::cout << "[Server] Listening on port " << port << "...\n";
    int client_sock = accept(sock, nullptr, nullptr);
    if(client_sock < 0) { perror("accept"); return 1; }
    std::cout << "[Server] Client connected (non-secure)\n";

    // --- handshake ---
    char buf[16]; int n = read(client_sock, buf, sizeof(buf)-1);
    buf[n] = '\0'; std::cout << "[Server] Handshake received: " << buf << "\n";
    write(client_sock, "HELLO", 5);

    // --- TLS setup ---
    SSL_library_init(); OpenSSL_add_all_algorithms(); SSL_load_error_strings();
    SSL_CTX* ctx = SSL_CTX_new(TLS_server_method());
    SSL_CTX_set_min_proto_version(ctx, TLS1_3_VERSION);
    SSL_CTX_set_max_proto_version(ctx, TLS1_3_VERSION);
    if(!SSL_CTX_use_certificate_file(ctx, "server.crt", SSL_FILETYPE_PEM) ||
       !SSL_CTX_use_PrivateKey_file(ctx, "server.key", SSL_FILETYPE_PEM)) { ERR_print_errors_fp(stderr); return 1; }

    SSL* ssl = SSL_new(ctx); SSL_set_fd(ssl, client_sock);
    std::cout << "[Server] Starting TLS handshake...\n";
    int ret = SSL_accept(ssl); if(ret <= 0) { ERR_print_errors_fp(stderr); return 1; }
    std::cout << "[Server] TLS handshake done.\n";

    // --- read image share ---
    uint32_t img_size_net; SSL_read(ssl, &img_size_net, sizeof(img_size_net));
    uint32_t img_size = ntohl(img_size_net);
    std::cout << "[Server] Expecting " << img_size << " bytes of share\n";

    std::vector<uint8_t> share(img_size);
    size_t total = 0;
    while(total < img_size) {
        int r = SSL_read(ssl, share.data() + total, img_size - total);
        if(r <= 0) break;
        total += r;
    }
    std::cout << "[Server] Received share (" << total << " bytes)\n";

    uint64_t sum_share = compute_sum_share(share);
    std::cout << "[Server] Local sum share: " << sum_share << "\n";

    // --- send sum share back ---
    uint64_t net_sum = htobe64(sum_share);
    SSL_write(ssl, &net_sum, sizeof(net_sum));

    SSL_shutdown(ssl); SSL_free(ssl); close(client_sock); close(sock); SSL_CTX_free(ctx);
    std::cout << "[Server] Done.\n";
    return 0;
}
