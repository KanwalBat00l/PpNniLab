# Demo 

- In this demo, Id like to demonstrate a representative application for PPNNI, this is a proof of concept and prototype so instead of using the actual implementation of SecFePAS, we are presenting a PPNNI dummy, which essentially perform similar communication and partly similar computation.
- Directory Structure,
    - PPNNI.Dummy
    - ServerManager
    - AndroidApp

- The PPNNI.Dummy contains client server application where client initiate the tcp commutation, with hello message, followed it up with TLS handshake, and eventually calculated image brightness using secret sharing. 

## Step 1: PoC PPNNI Dummy

**Running Server:**
```bash
cd PPNNI.Dummy/build
./server 8000 # Running Server at Port 8000
```
**Running Client:** In another terminal

```bash
cd PPNNI.Dummy
./build/client 127.0.0.1 8000 hs0.png
```

## Step 2: PoC ServerManager

**Running Server:**
```bash
cd ServerManager
./gradlew run
```
**Testing http request :** using rest client

```rest
GET http://192.168.1.249:8080/getServer?model=default
###
```
**Running Client:** In another terminal

```bash
cd PPNNI.Dummy
./build/client 127.0.0.1 6000 hs0.png
```

## Step 3: PoC AndroidApp

*Pre-requisite*, **Running Server**:
```bash
cd ServerManager
./gradlew run
```

**Running Android App**
```bash
cd AndroidApp
./gradlew clean
./gradlew assembleDebug
```
-Provide Port, IP, and Model for http get request, 
-Select the image and perform the secure computation


- ![PPNNI (Server and Client)](/media/PPNNI-Demo.mov)
