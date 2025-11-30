# llama.cpp Prebuilt Libraries Placeholder

This directory is a placeholder for prebuilt llama.cpp native libraries for the armeabi-v7a ABI.

## To Complete Integration:

### Option 1: Download Prebuilt Libraries
1. Download prebuilt llama.cpp Android libraries from:
   - https://github.com/ggerganov/llama.cpp/releases
   - Or build them yourself (see Option 2)

2. Place `libllama.so` in this directory

3. Update `src/main/cpp/CMakeLists.txt` to link the library

### Option 2: Build llama.cpp for Android
1. Clone llama.cpp:
   ```bash
   git clone https://github.com/ggerganov/llama.cpp.git
   cd llama.cpp
   ```

2. Build for Android:
   ```bash
   mkdir build-android-armv7
   cd build-android-armv7
   cmake .. \
     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
     -DANDROID_ABI=armeabi-v7a \
     -DANDROID_PLATFORM=android-26 \
     -DCMAKE_BUILD_TYPE=Release \
     -DLLAMA_NATIVE=OFF
   cmake --build . --config Release
   ```

3. Copy `libllama.so` to this directory

## Notes:
- armeabi-v7a is for older 32-bit ARM devices
- Performance will be slower than arm64-v8a
- Smaller models (1B) recommended for this ABI
