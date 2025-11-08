#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <thread>
#include <chrono>
#include <cstdint>
#include <cstring>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <algorithm> // for std::clamp

#ifdef __APPLE__
    #include <libkern/OSByteOrder.h>
    #define htobe64(x) OSSwapHostToBigInt64(x)
    #define be64toh(x) OSSwapBigToHostInt64(x)
#else
    #include <endian.h>
#endif

// --- calculate per-round delay ---
double calculate_delay_ms(const std::string &model, const std::string &mode, const std::string &precision) {
    double delay_ms = 0.0;
    if (model == "resnet50") delay_ms = 300.0;
    else if (model == "resnet50_quantized") delay_ms = 150.0;
    else if (model == "sqnet") delay_ms = 100.0;
    else if (model == "sqnet_quantized") delay_ms = 150.0;

    if (mode == "SCI_HE") delay_ms *= 1.5;
    else if (mode == "cheetah") delay_ms *= 0.8;

    return std::clamp(delay_ms, 100.0, 500.0);
}

// --- determine rounds per model ---
int get_rounds_for_model(const std::string &model) {
    if (model == "resnet50" || model == "resnet50_quantized") return 52;
    if (model == "sqnet" || model == "sqnet_quantized") return 18;
    return 10;
}

// --- load weights from file ---
std::vector<uint64_t> load_weights(const std::string &path) {
    std::ifstream wf(path);
    if (!wf.good()) {
        std::cerr << "[Error] Weight file not found: " << path << std::endl;
        exit(1);
    }
    std::vector<uint64_t> weights;
    uint64_t val;
    while (wf >> val) weights.push_back(val);
    return weights;
}

int main(int argc, char** argv) {
    if (argc != 4) {
        std::cerr << "Usage: " << argv[0]
                  << " [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <port>\n";
        return 1;
    }

    std::string mode = argv[1];
    std::string model = argv[2];
    int port = std::stoi(argv[3]);

    std::string weight_file = "pretrained/" + model + "_mock_weights.inp";
    std::vector<uint64_t> weights = load_weights(weight_file);

    // --- Setup socket ---
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) { perror("socket"); return 1; }

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    if (bind(sock, (sockaddr*)&addr, sizeof(addr)) < 0) { perror("bind"); close(sock); return 1; }
    if (listen(sock, 1) < 0) { perror("listen"); close(sock); return 1; }

    std::cout << "==============================" << std::endl;
    std::cout << " PPNNI Mock Server Started" << std::endl;
    std::cout << " Model: " << model << std::endl;
    std::cout << " Mode:  " << mode << std::endl;
    std::cout << "==============================" << std::endl;
    std::cout << "Listening on port: " << port << std::endl;
    std::cout << "Using weights: " << weight_file << std::endl;

    sockaddr_in client_addr{};
    socklen_t len = sizeof(client_addr);
    int client_fd = accept(sock, (sockaddr*)&client_addr, &len);
    if (client_fd < 0) { perror("accept"); close(sock); return 1; }

    // --- receive model name from client ---
    char model_buf[64] = {0};
    recv(client_fd, model_buf, sizeof(model_buf), 0);
    std::string client_model(model_buf);
    if (client_model != model) {
        std::cerr << "[Error] Model mismatch: client=" << client_model
                  << " server=" << model << std::endl;
        close(client_fd);
        return 1;
    }

    int rounds = get_rounds_for_model(model);
    std::cout << "Connected to client. Starting " << rounds << " communication rounds..." << std::endl;

    double per_round_delay = calculate_delay_ms(model, mode, "fp16");

    // --- store client input for deterministic final values ---
    std::vector<uint64_t> client_input;

    for (int r = 1; r <= rounds; ++r) {
        uint64_t net_data;
        ssize_t recvd = recv(client_fd, &net_data, sizeof(net_data), 0);
        if (recvd <= 0) {
            std::cerr << "Client disconnected unexpectedly at round " << r << std::endl;
            break;
        }
        uint64_t data = be64toh(net_data);
        client_input.push_back(data);

        // deterministic per-round response
        uint64_t resp = data + (weights.size() ? weights[r % weights.size()] : 1);
        if (resp > 9999) resp %= 10000;  // clamp output
        uint64_t net_resp = htobe64(resp);

        send(client_fd, &net_resp, sizeof(net_resp), 0);

        std::this_thread::sleep_for(std::chrono::milliseconds(static_cast<int>(per_round_delay)));
        std::cout << "  Round " << r << "/" << rounds << " completed." << std::endl;
    }

    // --- generate 4 deterministic final output shares based on input ---
    std::vector<uint64_t> final_vals(4, 0);
    size_t N = client_input.size();
    for (size_t i = 0; i < 4; ++i) {
        uint64_t val = 0;
        for (size_t j = i; j < N; j += 4) {
            val += client_input[j];
        }
        final_vals[i] = val % 10000; // clamp to 0-9999
    }

    for (auto &v : final_vals) {
        uint64_t net_val = htobe64(v);
        send(client_fd, &net_val, sizeof(net_val), 0);
    }

    std::cout << "Simulation completed.\n";
    std::cout << "Mock server finished processing request.\n";
    std::cout << "Output shares sent to client.\n";

    close(client_fd);
    close(sock);
    return 0;
}
