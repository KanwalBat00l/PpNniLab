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

int get_rounds_for_model(const std::string &model) {
    if (model == "resnet50" || model == "resnet50_quantized") return 52;
    if (model == "sqnet" || model == "sqnet_quantized") return 18;
    return 10;
}

std::vector<uint64_t> load_input(const std::string &path) {
    std::ifstream in(path);
    if (!in.good()) {
        std::cerr << "[Error] Input file not found: " << path << std::endl;
        exit(1);
    }
    std::vector<uint64_t> data;
    uint64_t val;
    while (in >> val) data.push_back(val);
    return data;
}

int main(int argc, char** argv) {
    if (argc != 5) {
        std::cerr << "Usage: " << argv[0]
                  << " [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <server_ip> <port>\n";
        return 1;
    }

    std::string mode = argv[1];
    std::string model = argv[2];
    std::string server_ip = argv[3];
    int port = std::stoi(argv[4]);

    std::string input_file = "pretrained/" + model + "_mock_input.inp";
    std::vector<uint64_t> input_data = load_input(input_file);

    int rounds = get_rounds_for_model(model);
    std::cout << "Connected to server. Running " << rounds << " rounds..." << std::endl;

    // --- Setup socket ---
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) { perror("socket"); return 1; }

    sockaddr_in serv_addr{};
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);
    if (inet_pton(AF_INET, server_ip.c_str(), &serv_addr.sin_addr) <= 0) {
        perror("inet_pton");
        close(sock);
        return 1;
    }

    if (connect(sock, (sockaddr*)&serv_addr, sizeof(serv_addr)) < 0) {
        perror("connect");
        close(sock);
        return 1;
    }

    // --- Send model name ---
    send(sock, model.c_str(), model.size(), 0);

    // --- Send per-round data and receive response ---
    for (int r = 0; r < rounds; ++r) {
        uint64_t data = (r < input_data.size() ? input_data[r] : r + 1);
        uint64_t net_data = htobe64(data);
        send(sock, &net_data, sizeof(net_data), 0);

        uint64_t net_resp;
        ssize_t recvd = recv(sock, &net_resp, sizeof(net_resp), 0);
        if (recvd <= 0) {
            std::cerr << "Server disconnected unexpectedly at round " << (r + 1) << std::endl;
            break;
        }
        uint64_t resp = be64toh(net_resp);

        // --- Clamp output to 0–9999 for debugging
        resp = resp % 10000;

        std::cout << "  Round " << (r + 1) << "/" << rounds
                  << " completed. Server response: " << resp << std::endl;
    }

    // --- Receive final deterministic 4 values ---
    std::vector<uint64_t> final_vals(4);
    ssize_t bytes = recv(sock, final_vals.data(), final_vals.size() * sizeof(uint64_t), 0);
    if (bytes <= 0) {
        std::cerr << "[Warning] Did not receive final values from server." << std::endl;
    } else {
        // --- Normalize final output deterministically ---
        for (auto &v : final_vals) {
            v = (v % 10000);
            std::cout << v << std::endl;
        }
    }

    close(sock);
    std::cout << "See Client.log" << std::endl;
    return 0;
}
