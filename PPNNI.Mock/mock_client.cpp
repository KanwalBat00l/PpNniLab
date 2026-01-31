#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>
#include <cstring>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>

#ifdef __APPLE__
    #include <libkern/OSByteOrder.h>
    #define htobe64(x) OSSwapHostToBigInt64(x)
    #define be64toh(x) OSSwapBigToHostInt64(x)
#else
    #include <endian.h>
#endif


#ifndef CLIENT_MODEL
#define CLIENT_MODEL ""
#endif

#ifndef CLIENT_PROTOCOL
#define CLIENT_PROTOCOL ""
#endif


// ---------------------- MODEL ROUND COUNTS -------------------------
int get_rounds_for_model(const std::string &model) {
    if (model == "resnet50" || model == "resnet50_quantized") return 52;
    if (model == "sqnet" || model == "sqnet_quantized") return 18;
    return 10;
}


// ---------------------- LOAD INPUT -------------------------
std::vector<uint64_t> load_input(const std::string &path) {
    std::ifstream in(path);
    if (!in.good()) {
        std::cerr << "[Error] Input file not found: " << path << std::endl;
        return {};        // JNI-safe, prevents crash
    }

    std::vector<uint64_t> data;
    uint64_t val;
    while (in >> val) data.push_back(val);

    if (data.empty()) {
        std::cerr << "[Error] Input file empty or unreadable: " << path << std::endl;
    }
    return data;
}


// ---------------------- READ EXACT -------------------------
ssize_t read_exact(int sock, void* buffer, size_t size) {
    size_t total = 0;

    while (total < size) {
        ssize_t n = recv(sock, (char*)buffer + total, size - total, 0);

        if (n <= 0) {
            return n;  // Error or disconnect
        }

        total += n;
    }

    return total;
}




// ---------------------- MAIN -------------------------
int main(int argc, char** argv) {

    if (argc != 5) {
        std::cerr << "Usage: " << argv[0]
                  << " [cheetah|SCI_HE] "
                  << "[resnet50|sqnet|resnet50_quantized|sqnet_quantized] "
                  << "<server_ip> <port>\n";
        return 1;
    }

    std::string protocol = argv[1];           // cheetah or SCI_HE
    std::string model = argv[2];              // model
    std::string server_ip = argv[3];
    int port = std::stoi(argv[4]);

    // ---------------------- INPUT FILE -------------------------
    std::string input_file =
        "pretrained/" + model +  "_mock_input.inp";

    std::vector<uint64_t> input_data = load_input(input_file);
    if (input_data.empty()) {
        std::cerr << "[Fatal] Could not load input. Aborting.\n";
        return 1;
    }

    int rounds = get_rounds_for_model(model);

    // ---------------------- CONNECT -------------------------
    std::cout << "[Client] Connecting to " << server_ip
              << ":" << port << "..." << std::endl;

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) {
        perror("[Client] socket");
        return 1;
    }

    sockaddr_in serv_addr{};
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);

    if (inet_pton(AF_INET, server_ip.c_str(), &serv_addr.sin_addr) <= 0) {
        perror("[Client] inet_pton");
        close(sock);
        return 1;
    }

    if (connect(sock, (sockaddr*)&serv_addr, sizeof(serv_addr)) < 0) {
        perror("[Client] connect");
        close(sock);
        return 1;
    }

    std::cout << "[Client] Connected! Running " << rounds << " rounds..." << std::endl;


    // ---------------------- SEND MODEL NAME -------------------------
    ssize_t s = send(sock, model.c_str(), model.size(), 0);
    if (s <= 0) {
        std::cerr << "[Client] Failed to send model name.\n";
        close(sock);
        return 1;
    }

    usleep(100 * 1000);


    // ---------------------- ROUNDS -------------------------
    for (int r = 0; r < rounds; ++r) {

        uint64_t data =
            (r < input_data.size() ? input_data[r] : (uint64_t)(r+1));

        uint64_t net_data = htobe64(data);

        if (send(sock, &net_data, sizeof(net_data), 0) != sizeof(net_data)) {
            std::cerr << "[Client] Send failure at round " << (r+1) << "\n";
            break;
        }

        uint64_t net_resp = 0;
        ssize_t recvd = read_exact(sock, &net_resp, sizeof(net_resp));

        if (recvd <= 0) {
            std::cerr << "[Client] Server disconnected at round "
                      << (r+1) << std::endl;
            break;
        }

        uint64_t resp = be64toh(net_resp) % 10000;

        std::cout << "  Round " << (r+1) << "/" << rounds
                  << " completed. Server response: " << resp << std::endl;
    }


    // ---------------------- FINAL VALUES -------------------------
    std::vector<uint64_t> final_vals(4);

    for (int i = 0; i < 4; i++) {
        uint64_t net_val = 0;
        ssize_t r = read_exact(sock, &net_val, sizeof(net_val));

        if (r <= 0) {
            std::cerr << "[Error] Failed to receive final value " << i << std::endl;
            break;
        }

        final_vals[i] = be64toh(net_val) % 10000;
    }

    for (auto v : final_vals) {
        std::cout << v << std::endl;
    }

    close(sock);

    std::cout << "[Client] Done." << std::endl;
    return 0;
}
