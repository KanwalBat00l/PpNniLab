#include <fstream>
#include <vector>
#include "mock_common.hpp"

extern "C" int client_run(const char* protocol, const char* model_name, 
                         const char* ip, int port, const char* input_path) {
    
    auto config = get_model_config(model_name, protocol);
    std::vector<uint64_t> input_data = load_inp_file(input_path);
    if (input_data.empty()) { std::cerr << "❌ Missing input: " << input_path << std::endl; return -1; }

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    struct sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    inet_pton(AF_INET, ip, &addr.sin_addr);
    if (connect(sock, (struct sockaddr*)&addr, sizeof(addr)) < 0) return -1;

    // 1. SIMULATED TLS
    send(sock, "TLS_HELLO", 9, 0);
    char shake[16] = {0};
    recv(sock, shake, 15, 0);

    // 2. CONFIGURATION HANDSHAKE
    send(sock, protocol, strlen(protocol), 0);
    usleep(1000); // Prevent bundling
    send(sock, model_name, strlen(model_name), 0);

    char cfg_status[16] = {0};
    recv(sock, cfg_status, 15, 0);
    if (strcmp(cfg_status, "CFG_OK") != 0) {
        std::cerr << "❌ Configuration rejected by server." << std::endl;
        return -1;
    }

    // 3. RUN ROUNDS
    std::cout << "[Client] Running " << config.rounds << " rounds..." << std::endl;
    for (int r = 0; r < config.rounds; ++r) {
        uint64_t val = (r < input_data.size()) ? input_data[r] : r;
        uint64_t net_val = host_to_net64(val);
        send_exact(sock, &net_val, 8);
        
        uint64_t net_resp;
        recv_exact(sock, &net_resp, 8);
        std::cout << "  Round " << (r+1) << ": Sent(" << val << "), Recv(" << net_to_host64(net_resp) << ")" << std::endl;
    }

    // 4. FINAL SHARES
    std::cout << "[Client] Shares Received:" << std::endl;
    for (int i = 0; i < 4; ++i) {
        uint64_t share;
        recv_exact(sock, &share, 8);
        std::cout << "  " << net_to_host64(share) << std::endl;
    }

    close(sock);
    return 0;
}

#ifndef ANDROID_JNI
int main(int argc, char** argv) {
    if (argc < 6) {
        std::cout << "Usage: ./mock_client [protocol] [model] [ip] [port] [input_file]" << std::endl;
        return 1;
    }
    return client_run(argv[1], argv[2], argv[3], std::stoi(argv[4]), argv[5]);
}
#endif