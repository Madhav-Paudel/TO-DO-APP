# CMakeLists.txt for llama.cpp JNI wrapper
# This builds libllamainference.so for on-device LLM inference

cmake_minimum_required(VERSION 3.10)
project(llamainference)

# Use C++17 standard (required by llama.cpp)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for better performance
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -DNDEBUG")

# Enable NEON for ARM processors (significant performance boost)
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon -mfloat-abi=softfp")
endif()

# Build the JNI library
add_library(llamainference SHARED
    llama_jni.cpp
)

# TODO: When llama.cpp prebuilt libraries are available, uncomment and configure:
# Option 1: If using prebuilt llama.cpp libraries placed in jniLibs/
# set(LLAMA_CPP_LIB_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
# add_library(llama SHARED IMPORTED)
# set_target_properties(llama PROPERTIES IMPORTED_LOCATION ${LLAMA_CPP_LIB_DIR}/libllama.so)
# target_link_libraries(llamainference PRIVATE llama)

# Option 2: If building llama.cpp from source (requires llama.cpp submodule)
# set(LLAMA_CPP_DIR ${CMAKE_SOURCE_DIR}/llama_cpp)
# add_subdirectory(${LLAMA_CPP_DIR} llama_cpp_build)
# target_include_directories(llamainference PRIVATE ${LLAMA_CPP_DIR})
# target_link_libraries(llamainference PRIVATE llama)

# Include directories
target_include_directories(llamainference PRIVATE
    ${CMAKE_SOURCE_DIR}
    # TODO: Add llama.cpp include path when available:
    # ${CMAKE_SOURCE_DIR}/llama_cpp/include
)

# Find and link Android log library
find_library(log-lib log)
target_link_libraries(llamainference
    ${log-lib}
)

# Print build info for debugging
message(STATUS "Building llamainference for ABI: ${ANDROID_ABI}")
message(STATUS "C++ Compiler: ${CMAKE_CXX_COMPILER}")
message(STATUS "C++ Flags: ${CMAKE_CXX_FLAGS}")
