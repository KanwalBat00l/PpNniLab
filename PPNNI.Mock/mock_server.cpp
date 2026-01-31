#include <iostream>
#include <thread>
#include <vector>
#include "mock_common.hpp"

int main(int argc, char** argv) {
    if (argc < 4) {
        std::cerr << "Usage: ./mock_server [protocol] [model] [port]" << std::endl;
        return 1;
    }

    std::string protocol = argv[1];
    std::string model = argv[2];
    int port = std::stoi(argv[3]);

    auto config = get_model_config(model, protocol);
    if (!config.valid || !is_valid_protocol(protocol)) {
        std::cerr << "❌ Invalid arguments." << std::endl;
        return 1;
    }

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1; setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    struct sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = INADDR_ANY;

    if (bind(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) { perror("Bind failed"); return 1; }
    listen(sock, 1);

    std::cout << "[Server] Waiting for " << protocol << "/" << model << " on port " << port << "..." << std::endl;
    int client_fd = accept(sock, nullptr, nullptr);

    // 1. SIMULATED TLS HANDSHAKE
    char shake_in[16] = {0};
    recv(client_fd, shake_in, 15, 0);
    send(client_fd, "TLS_OK", 6, 0);

    // 2. CONFIGURATION VALIDATION
    char c_proto[32] = {0};
    char c_model[64] = {0};
    recv(client_fd, c_proto, 31, 0);
    recv(client_fd, c_model, 63, 0);

    if (protocol != c_proto || model != c_model) {
        std::cerr << "❌ Config Mismatch! Client requested: " << c_proto << "/" << c_model << std::endl;
        send(client_fd, "CFG_FAIL", 8, 0);
        close(client_fd); close(sock);
        return 1;
    }
    send(client_fd, "CFG_OK", 6, 0);
    std::cout << "[Server] Handshake & Config Verified." << std::endl;

    // 3. LOAD WEIGHTS & RUN ROUNDS
    std::string weight_path = "pretrained/" + model + "_mock_weights.inp";
    std::vector<uint64_t> weights = load_inp_file(weight_path);
    if (weights.empty()) weights.push_back(1234); // Fallback

    std::vector<uint64_t> history;
    for (int r = 1; r <= config.rounds; ++r) {
        uint64_t net_in;
        if (!recv_exact(client_fd, &net_in, 8)) break;
        uint64_t val = net_to_host64(net_in);
        history.push_back(val);

        uint64_t resp = (val + weights[r % weights.size()]) % 10000;
        uint64_t net_out = host_to_net64(resp);
        
        std::this_thread::sleep_for(std::chrono::milliseconds(config.delay_ms));
        send_exact(client_fd, &net_out, 8);
        std::cout << "  Round " << r << " processed: Recv(" << val << ") -> Sent(" << resp << ")" << std::endl;
    }

    // 4. FINAL SHARES
    for (int i = 0; i < 4; ++i) {
        uint64_t share = host_to_net64(777 + i + (history.empty() ? 0 : history[0] % 100));
        send_exact(client_fd, &share, 8);
    }

    std::cout << "[Server] Session Complete." << std::endl;
    close(client_fd); close(sock);
    return 0;
}