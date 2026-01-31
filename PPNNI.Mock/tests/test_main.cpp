#define CATCH_CONFIG_MAIN  
#include "catch.hpp"
#include "../mock_common.hpp"
#include <fstream>
#include <vector>

// ========================================================
// TEST SUITE: PPNNI CORE LOGIC
// ========================================================

TEST_CASE("Model Configuration and Scaling Logic", "[config]") {
    
    SECTION("ResNet50: Full vs Quantized Performance") {
        INFO("Validating ResNet50 Configuration on Cheetah Protocol");
        auto full = get_model_config("resnet50", "cheetah");
        
        REQUIRE(full.valid == true); // Stop if config is invalid
        CHECK(full.rounds == 52);
        CHECK(full.delay_ms == 150);

        INFO("Validating Quantization Speed Boost (Expected 2x Faster)");
        auto quant = get_model_config("resnet50_quantized", "cheetah");
        
        REQUIRE(quant.valid == true);
        CHECK(quant.is_quantized == true);
        CHECK(quant.delay_ms == 75); // 150 / 2
    }

    SECTION("Protocol Overhead Analysis") {
        INFO("Comparing SCI_HE (High Overhead) vs Cheetah (Standard)");
        auto cheetah = get_model_config("sqnet", "cheetah");
        auto sci = get_model_config("sqnet", "SCI_HE");
        
        REQUIRE(cheetah.valid == true);
        REQUIRE(sci.valid == true);
        
        CHECK(sci.delay_ms == (cheetah.delay_ms * 2));
    }
}

TEST_CASE("Input Validation & Security Handshake", "[security]") {
    
    SECTION("Whitelist of Approved Protocols") {
        INFO("Verifying that only approved MPC protocols are allowed");
        CHECK(is_valid_protocol("cheetah") == true);
        CHECK(is_valid_protocol("SCI_HE") == true);
        CHECK(is_valid_protocol("abc") == false);
        CHECK(is_valid_protocol("") == false);
    }

    SECTION("Model String Sanity Checks") {
        INFO("Ensuring unknown or malicious model strings are rejected");
        auto cfg = get_model_config("invalid_model_name", "cheetah");
        CHECK(cfg.valid == false);
    }
}

TEST_CASE("Cross-Platform Data Integrity", "[network]") {
    SECTION("64-bit Endianness Transformation") {
        INFO("Testing Big-Endian conversion for Mobile-to-Server communication");
        uint64_t original = 0x1122334455667788;
        uint64_t networked = host_to_net64(original);
        uint64_t restored = net_to_host64(networked);
        
        REQUIRE(original == restored);
        // Capture specific values in case of failure
        CAPTURE(original);
        CAPTURE(networked);
    }
}

TEST_CASE("File System Resilience and I/O", "[io]") {
    
    SECTION("Error Handling: Missing .inp files") {
        INFO("Checking if system handles missing weight/input files without crashing");
        auto data = load_inp_file("missing_test_file.inp");
        CHECK(data.empty());
    }

    SECTION("Success Path: Valid .inp parsing") {
        INFO("Verifying correct parsing of mock fixed-point data files");
        std::ofstream tmp("unit_test_temp.inp");
        tmp << "10 20 30";
        tmp.close();

        auto data = load_inp_file("unit_test_temp.inp");
        REQUIRE(data.size() == 3);
        CHECK(data[0] == 10);
        CHECK(data[1] == 20);
        CHECK(data[2] == 30);

        std::remove("unit_test_temp.inp");
    }
}