# --- STAGE 1: Build Stage ---
# We use a full GCC image to compile the C++ code
FROM gcc:13 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy all the C++ source files and the Makefile
COPY . .

# Run the Makefile to compile the binaries
# This creates the ./build/mock_server binary
RUN make all

# --- STAGE 2: Runner Stage ---
# We use a much smaller, clean image to actually run the server
FROM debian:bookworm-slim

WORKDIR /root/

# Install the runtime libraries needed for C++
RUN apt-get update && apt-get install -y \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

# Copy only the built server binary from the builder stage
COPY --from=builder /app/build/mock_server .

# Copy the pretrained weights folder (server needs these to "simulate" inference)
COPY --from=builder /app/pretrained/ ./pretrained/

# Expose the port the mock server listens on (default 8000 as per your README)
EXPOSE 8000

# Set the default command to run the server
# Format: ./mock_server [protocol] [model] [port]
# We use 'cheetah' and 'resnet50' as defaults
ENTRYPOINT ["./mock_server", "cheetah", "resnet50", "8000"]