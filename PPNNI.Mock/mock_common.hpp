#pragma once
#include <iostream>
#include <thread>
#include <chrono>
#include <string>
#include <random>

inline void simulate_rounds(const std::string& model, const std::string& mode) {
    int rounds = (model.find("resnet") != std::string::npos) ? 50 : 18;
    int round_time_ms = (mode == "SCI_HE") ? 200 : 100; // SCI_HE slower

    std::cout << "Simulating " << rounds << " rounds for " << model 
              << " using mode " << mode << "...\n";

    for (int i = 1; i <= rounds; ++i) {
        std::this_thread::sleep_for(std::chrono::milliseconds(round_time_ms));
        std::cout << "  Round " << i << "/" << rounds << " completed.\n";
    }
    std::cout << "Simulation completed.\n";
}

inline std::string random_label() {
    const std::string labels[] = {
        "cat", "dog", "car", "person", "airplane", "tree", "building"
    };
    static std::mt19937 rng{std::random_device{}()};
    std::uniform_int_distribution<int> dist(0, 6);
    return labels[dist(rng)];
}

inline void print_header(const std::string& role, const std::string& model, const std::string& mode) {
    std::cout << "==============================\n";
    std::cout << " PPNNI Mock " << role << " Started\n";
    std::cout << " Model: " << model << "\n";
    std::cout << " Mode:  " << mode << "\n";
    std::cout << "==============================\n";
}
