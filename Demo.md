# Demo 

- In this demo, Id like to demonstrate a represntative application for PPNNI, this is a proof of concept and prtotype so instead of using the acutal implementation of SecFePAS, we are presenting a PPNNI dummy, which essnetially perform similar communication and partly similar computation.
- The PPNNI.Dummy contains client server application where client intiate the tcp commucation, with hello message, followed it up with TLS handshake, and eventually calculated image brightness using secret sharing. 

## Step 1: PoC PPNNI Dummy

**Running Server:**
```bash
cd PPNNI.Dummy/build
./server 8000 # Running Server at Port 8000
```
**Running Client:** In another termainal

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
**Running Client:** In another termainal

```bash
cd PPNNI.Dummy
./build/client 127.0.0.1 6000 hs0.png
```

## Step 3: PoC AndroidApp

*Pre-requisit*, **Running Server**:
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
-Proivde Port, IP, and Model for http get request, 
-Select the image and perform the secure computation


