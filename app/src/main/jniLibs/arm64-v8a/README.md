# llama.cpp Prebuilt Libraries Placeholder

This directory is a placeholder for prebuilt llama.cpp native libraries for the arm64-v8a ABI.

## To Complete Integration:

### Option 1: Download Prebuilt Libraries
1. Download prebuilt llama.cpp Android libraries from:
   - https://github.com/ggerganov/llama.cpp/releases
   - Or build them yourself (see Option 2)

2. Place `libllama.so` in this directory

3. Update `src/main/cpp/CMakeLists.txt` to link the library:
   ```cmake
   add_library(llama SHARED IMPORTED)
   set_target_properties(llama PROPERTIES 
       IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama.so)
   target_link_libraries(llamainference PRIVATE llama)
   ```

### Option 2: Build llama.cpp for Android
1. Clone llama.cpp:
   ```bash
   git clone https://github.com/ggerganov/llama.cpp.git
   cd llama.cpp
   ```

2. Build for Android:
   ```bash
   mkdir build-android
   cd build-android
   cmake .. \
     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
     -DANDROID_ABI=arm64-v8a \
     -DANDROID_PLATFORM=android-26 \
     -DCMAKE_BUILD_TYPE=Release \
     -DLLAMA_NATIVE=OFF
   cmake --build . --config Release
   ```

3. Copy `libllama.so` to this directory

## Required Files:
- `libllama.so` - Main llama.cpp library

## Notes:
- arm64-v8a is recommended for best performance
- Q4_K_M quantized models work well on mobile devices
- Minimum 4GB RAM recommended for 1B+ parameter models
