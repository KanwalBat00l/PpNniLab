#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <cstdint>
#include <arpa/inet.h>
#include <unistd.h>

int main(int argc, char** argv) {
    if (argc != 5) {
        std::cerr << "Usage: " << argv[0] << " <server_ip> <port> [resnet50|sqnet] <mode>\n";
        return 1;
    }

    std::string server_ip = argv[1];
    int port = std::stoi(argv[2]);
    std::string model = argv[3];
    std::string mode = argv[4];

    std::string input_file = "pretrained/" + model + "_mock_input.inp";
    std::ifstream inf(input_file, std::ios::binary);
    if (!inf.good()) {
        std::cerr << "[Error] Input file not found: " << input_file << "\n";
        return 1;
    }

    std::vector<uint8_t> input_data((std::istreambuf_iterator<char>(inf)), {});
    inf.close();

    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) { perror("socket"); return 1; }

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    inet_pton(AF_INET, server_ip.c_str(), &addr.sin_addr);

    if (connect(sock, (sockaddr*)&addr, sizeof(addr)) < 0) { perror("connect"); return 1; }

    // Send input size + data
    uint32_t net_size = htonl(input_data.size());
    write(sock, &net_size, sizeof(net_size));
    write(sock, input_data.data(), input_data.size());

    // Receive partial results for each round
    int rounds = (model == "resnet50") ? 50 : 18;
    std::vector<uint8_t> server_output(rounds);
    for (int i = 0; i < rounds; i++) {
        if (read(sock, &server_output[i], 1) != 1) {
            std::cerr << "Failed to read data from server\n";
            break;
        }
        std::cout << "Round " << i+1 << "/" << rounds << " received byte: "
                  << (int)server_output[i] << "\n";
    }

    close(sock);
    std::cout << "Client finished.\n";
    return 0;
}
