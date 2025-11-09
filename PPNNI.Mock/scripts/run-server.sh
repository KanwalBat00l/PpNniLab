#!/usr/bin/env bash
set -e
MODE=$1
MODEL=$2
PORT=$3

if [ $# -ne 3 ]; then
  echo "Usage: $0 [cheetah|SCI_HE] [resnet50|sqnet|resnet50_quantized|sqnet_quantized] <port>"
  exit 1
fi

BIN="build/bin/${MODEL}-${MODE}"

if [ ! -f "$BIN" ]; then
  echo "[Error] Binary not found: $BIN"
  exit 1
fi

echo "Using weights file: pretrained/${MODEL}_mock_weights.inp"
echo "Running $BIN (server)..."
"$BIN" "$MODE" "$MODEL" "$PORT"
