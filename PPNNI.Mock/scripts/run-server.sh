#!/bin/bash
# Usage: ./run-server.sh [protocol] [model] [port]
PROTOCOL=$1
MODEL=$2
PORT=$3
./build/mock_server $PROTOCOL $MODEL $PORT