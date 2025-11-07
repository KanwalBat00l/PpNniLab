#!/usr/bin/env bash
# scripts/run-client.sh

. scripts/common.sh

if [ $# -ne 4 ]; then
  echo -e "${RED}Usage: run-client.sh [cheetah|SCI_HE] [sqnet|sqnet_quantized|resnet50|resnet50_quantized] <server_ip> <port>${NC}"
  exit 1
fi

MODE=$1
NET=$2
SERVER_IP=$3
PORT=$4

if ! contains "cheetah SCI_HE" "$MODE"; then
  echo -e "Usage: run-client.sh ${RED}[cheetah|SCI_HE]${NC} [sqnet|sqnet_quantized|resnet50|resnet50_quantized] <server_ip> <port>"
  exit 1
fi

if ! contains "sqnet sqnet_quantized resnet50 resnet50_quantized" "$NET"; then
  echo -e "Usage: run-client.sh [cheetah|SCI_HE] ${RED}[sqnet|sqnet_quantized|resnet50|resnet50_quantized]${NC} <server_ip> <port>"
  exit 1
fi

# pick up the corresponding mock input file
INPUT_FILE="pretrained/${NET}_mock_input.inp"
if [ ! -f "$INPUT_FILE" ]; then
  echo -e "${RED}Error: Input file not found: ${INPUT_FILE}${NC}"
  exit 1
fi

mkdir -p data

echo -e "Using client input file: ${GREEN}$INPUT_FILE${NC}"
echo -e "Running ${GREEN}build/bin/${NET}-${MODE}${NC} (client)..."

cat "$INPUT_FILE" \
  | build/bin/${NET}-${MODE} \
      r=2 \
      ell=${SS_BITLEN} \
      nt=${NUM_THREADS} \
      ip=${SERVER_IP} \
      port=${PORT} \
  2>&1 | tee "${MODE}-${NET}_client.log"

echo -e "Client log → ${GREEN}${MODE}-${NET}_client.log${NC}"
