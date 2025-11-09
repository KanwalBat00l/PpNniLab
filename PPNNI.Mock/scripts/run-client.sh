#!/usr/bin/env bash
set -e
MODE=$1
MODEL=$2
SERVER_IP=$3
PORT=$4

if [ $# -ne 4 ]; then
  echo "Usage: $0 [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <server_ip> <port>"
  exit 1
fi

BIN="build/bin/${MODEL}-${MODE}_client"

if [ ! -f "$BIN" ]; then
  echo "[Error] Binary not found: $BIN"
  exit 1
fi

echo "Using input file: pretrained/${MODEL}_mock_input.inp"
echo "Running $BIN (client)..."
"$BIN" "$MODE" "$MODEL" "$SERVER_IP" "$PORT"
