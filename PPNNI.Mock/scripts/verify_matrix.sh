#!/bin/bash

# Configuration
PROTOCOLS=("cheetah" "SCI_HE")
MODELS=("resnet50" "resnet50_quantized" "sqnet" "sqnet_quantized")
PORT=9500
INPUT="pretrained/mock_input.inp"
PASS_COUNT=0
TOTAL_TESTS=8

# Function to get time in milliseconds (Portable for Mac/Linux)
get_time_ms() {
    python3 -c 'import time; print(int(time.time() * 1000))'
}

echo "===================================================="
echo "      PPNNI MOCK SYSTEM: MATRIX VERIFICATION        "
echo "===================================================="

for PROTO in "${PROTOCOLS[@]}"; do
    for MODEL in "${MODELS[@]}"; do
        echo "Testing: [Protocol: $PROTO] [Model: $MODEL]"
        
        # 1. Start Server in background
        ./build/mock_server "$PROTO" "$MODEL" "$PORT" > /dev/null 2>&1 &
        SERVER_PID=$!
        
        # 2. Wait a moment for server to bind
        sleep 0.5
        
        # 3. Capture start time
        start_time=$(get_time_ms)
        
        # 4. Run Client and capture output
        ./build/mock_client "$PROTO" "$MODEL" "127.0.0.1" "$PORT" "$INPUT" > client_temp.log 2>&1
        CLIENT_EXIT=$?
        
        # 5. Capture end time
        end_time=$(get_time_ms)
        duration=$((end_time - start_time))
        
        # 6. Cleanup Server
        kill $SERVER_PID > /dev/null 2>&1
        wait $SERVER_PID 2>/dev/null

        # 7. Verify Result
        if [ $CLIENT_EXIT -eq 0 ]; then
            echo "   --> RESULT: PASS | Duration: ${duration}ms"
            ((PASS_COUNT++))
        else
            echo "   --> RESULT: FAIL | Exit Code: $CLIENT_EXIT"
            echo "       Log Snippet:"
            tail -n 3 client_temp.log
        fi
        echo "----------------------------------------------------"
        
        ((PORT++))
    done
done

rm client_temp.log

echo "===================================================="
echo " Verification Complete: $PASS_COUNT / $TOTAL_TESTS Passed"
echo "===================================================="

if [ $PASS_COUNT -eq $TOTAL_TESTS ]; then
    exit 0
else
    exit 1
fi