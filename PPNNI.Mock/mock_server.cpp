#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>
#include <arpa/inet.h>
#include <unistd.h>
#include <random>

#ifndef htobe64
#define htobe64(x) __builtin_bswap64(x)
#endif
#ifndef be64toh
#define be64toh(x) __builtin_bswap64(x)
#endif

int main(int argc, char** argv) {
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0] << " [cheetah|SCI_HE] [resnet50|sqnet] <port>\n";
        return 1;
    }

    std::string mode = argv[1];        // cheetah / SCI_HE
    std::string model = argv[2];       // resnet50 / sqnet
    int port = std::stoi(argv[3]);

    std::string weight_file = "pretrained/" + model + "_mock_weights.inp";
    std::ifstream wf(weight_file, std::ios::binary);
    if (!wf.good()) {
        std::cerr << "[Error] Weight file not found: " << weight_file << "\n";
        return 1;
    }

    // Load weights (for demonstration we just read as bytes)
    std::vector<uint8_t> weights((std::istreambuf_iterator<char>(wf)), {});
    wf.close();

    // --- setup socket ---
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) { perror("socket"); return 1; }

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    if (bind(sock, (sockaddr*)&addr, sizeof(addr)) < 0) { perror("bind"); return 1; }
    if (listen(sock, 1) < 0) { perror("listen"); return 1; }

    std::cout << "==============================\n"
              << " PPNNI Mock Server Started\n"
              << " Model: " << model << "  Mode: " << mode << "\n"
              << " Listening on port: " << port << "\n"
              << " Using weights: " << weight_file << "\n"
              << "==============================\n";

    int client_sock = accept(sock, nullptr, nullptr);
    if (client_sock < 0) { perror("accept"); return 1; }
    std::cout << "Client connected.\n";

    // Determine rounds
    int rounds = (model == "resnet50") ? 50 : 18;

    // --- read input size from client ---
    uint32_t input_size_net;
    int r = read(client_sock, &input_size_net, sizeof(input_size_net));
    if (r != sizeof(input_size_net)) { std::cerr << "Failed to read input size\n"; return 1; }
    uint32_t input_size = ntohl(input_size_net);
    std::vector<uint8_t> client_data(input_size);

    size_t total = 0;
    while (total < input_size) {
        int n = read(client_sock, client_data.data() + total, input_size - total);
        if (n <= 0) break;
        total += n;
    }

    std::cout << "Received " << total << " bytes from client.\n";

    // --- round-based communication ---
    for (int i = 0; i < rounds; i++) {
        // Example: simple additive computation using part of weights
        uint8_t send_byte = weights[i % weights.size()] + client_data[i % client_data.size()];
        if (write(client_sock, &send_byte, 1) != 1) {
            std::cerr << "Failed to send data to client\n";
            break;
        }
        std::cout << "Round " << i+1 << "/" << rounds << " completed.\n";
    }

    close(client_sock);
    close(sock);
    std::cout << "Server finished.\n";
    return 0;
}
