#ifndef MOCK_COMMON_HPP
#define MOCK_COMMON_HPP

#include <string>
#include <vector>
#include <cstdint>
#include <arpa/inet.h>
#include <unistd.h>
#include <iostream>
#include <fstream>
#include <cstring>

struct ModelConfig {
    std::string name;
    int rounds;
    int delay_ms;
    bool is_quantized;
    bool valid;
};

// Strict Validation Rules
inline bool is_valid_protocol(const std::string& p) {
    return (p == "cheetah" || p == "SCI_HE");
}

// File Loader for .inp files
inline std::vector<uint64_t> load_inp_file(const std::string& path) {
    std::vector<uint64_t> data;
    std::ifstream f(path);
    if (!f.is_open()) return data;
    uint64_t val;
    while (f >> val) data.push_back(val);
    return data;
}

// Logic Engine for Model Properties
inline ModelConfig get_model_config(const std::string& name, const std::string& protocol) {
    ModelConfig cfg{"unknown", 0, 0, false, false};

    if (name.find("resnet50") != std::string::npos) {
        cfg.rounds = 52; cfg.delay_ms = 150; cfg.valid = true;
    } else if (name.find("sqnet") != std::string::npos) {
        cfg.rounds = 18; cfg.delay_ms = 50; cfg.valid = true;
    }

    if (name.find("_quantized") != std::string::npos) {
        cfg.is_quantized = true;
        cfg.delay_ms /= 2; // Quantized is faster
    }

    if (cfg.valid && protocol == "SCI_HE") {
        cfg.delay_ms *= 2; // SCI_HE is significantly more expensive
    }

    cfg.name = name;
    return cfg;
}

// Robust Networking Helpers
inline bool send_exact(int fd, const void* buf, size_t len) {
    const char* p = (const char*)buf;
    while (len > 0) {
        ssize_t s = send(fd, p, len, 0);
        if (s <= 0) return false;
        p += s; len -= s;
    }
    return true;
}

inline bool recv_exact(int fd, void* buf, size_t len) {
    char* p = (char*)buf;
    while (len > 0) {
        ssize_t r = recv(fd, p, len, 0);
        if (r <= 0) return false;
        p += r; len -= r;
    }
    return true;
}

// Portable Endian Helpers
#ifdef __APPLE__
    #include <libkern/OSByteOrder.h>
    #define host_to_net64(x) OSSwapHostToBigInt64(x)
    #define net_to_host64(x) OSSwapBigToHostInt64(x)
#else
    #include <endian.h>
    #define host_to_net64(x) htobe64(x)
    #define net_to_host64(x) be64toh(x)
#endif

#endif