#!/usr/bin/env bash
# scripts/run-server.sh

. scripts/common.sh

if [ $# -ne 3 ]; then
  echo -e "${RED}Usage: run-server.sh [cheetah|SCI_HE] [sqnet|sqnet_quantized|resnet50|resnet50_quantized] <port>${NC}"
  exit 1
fi

MODE=$1      # cheetah or SCI_HE
NET=$2       # sqnet, sqnet_quantized, resnet50, resnet50_quantized
PORT=$3      # port to listen on

if ! contains "cheetah SCI_HE" "$MODE"; then
  echo -e "Usage: run-server.sh ${RED}[cheetah|SCI_HE]${NC} [sqnet|sqnet_quantized|resnet50|resnet50_quantized] <port>"
  exit 1
fi

if ! contains "sqnet sqnet_quantized resnet50 resnet50_quantized" "$NET"; then
  echo -e "Usage: run-server.sh [cheetah|SCI_HE] ${RED}[sqnet|sqnet_quantized|resnet50|resnet50_quantized]${NC} <port>"
  exit 1
fi

# pick up the corresponding weight file (mock or real)
WEIGHT_FILE="pretrained/${NET}_mock_weights.inp"
if [ ! -f "$WEIGHT_FILE" ]; then
  echo -e "${RED}Error: Weight file not found: ${WEIGHT_FILE}${NC}"
  exit 1
fi

mkdir -p data

echo -e "Using weights file: ${GREEN}$WEIGHT_FILE${NC}"
echo -e "Running ${GREEN}build/bin/${NET}-${MODE}${NC} (server)..."

cat "$WEIGHT_FILE" \
  | build/bin/${NET}-${MODE} \
      r=1 \
      ell=${SS_BITLEN} \
      nt=${NUM_THREADS} \
      port=${PORT} \
  2>&1 | tee "${MODE}-${NET}_server.log"

echo -e "Server log → ${GREEN}${MODE}-${NET}_server.log${NC}"
