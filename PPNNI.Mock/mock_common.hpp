#pragma once
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <cstring>
#include <arpa/inet.h>
#include <unistd.h>

// per-model rounds
inline int get_rounds(const std::string& model) {
    if (model.find("resnet50") != std::string::npos) return 50;
    if (model.find("sqnet") != std::string::npos) return 18;
    return 10;
}

// helper to read binary .inp file
inline std::vector<uint8_t> read_inp(const std::string& path) {
    std::ifstream f(path, std::ios::binary);
    if (!f.good()) throw std::runtime_error("Cannot open input file: " + path);
    return { std::istreambuf_iterator<char>(f), {} };
}

// simple send/recv
inline ssize_t send_all(int sock, const void* buf, size_t len) {
    const uint8_t* p = static_cast<const uint8_t*>(buf);
    size_t sent = 0;
    while (sent < len) {
        ssize_t s = send(sock, p + sent, len - sent, 0);
        if (s <= 0) return s;
        sent += s;
    }
    return sent;
}

inline ssize_t recv_all(int sock, void* buf, size_t len) {
    uint8_t* p = static_cast<uint8_t*>(buf);
    size_t recvd = 0;
    while (recvd < len) {
        ssize_t r = recv(sock, p + recvd, len - recvd, 0);
        if (r <= 0) return r;
        recvd += r;
    }
    return recvd;
}
