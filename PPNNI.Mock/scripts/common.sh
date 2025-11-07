#!/usr/bin/env bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Check if a value exists in a list
contains() {
    local list="$1"
    local value="$2"
    for item in $list; do
        if [ "$item" == "$value" ]; then
            return 0
        fi
    done
    return 1
}

# Example defaults (adjust if needed)
SS_BITLEN=64
NUM_THREADS=4
SERVER_PORT=6000
SERVER_IP="127.0.0.1"
