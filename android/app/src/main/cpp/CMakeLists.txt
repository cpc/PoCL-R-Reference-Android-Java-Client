# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)
include(ExternalProject)

# find the number of processor cores of the build system
# and otherwise default to 8
include(ProcessorCount)
ProcessorCount(CORES)
if(CORES EQUAL 0 )
    set(EXTERNAL_PROJECT_CORES 8)
else(CORES EQUAL 0)
    set(EXTERNAL_PROJECT_CORES  ${CORES})
endif(CORES EQUAL 0)

set(EXTERNAL_PATH ${CMAKE_CURRENT_LIST_DIR}/../../../../../external/)

# Declares and names the project.

project("pocl_rreferenceandroidjavaclient")

# some interesting parameters set during init.
# Useful when compiling JOCL for android
message(WARNING "-DANDROID_NDK=${ANDROID_NDK} ")
message(WARNING "-DANDROID_PLATFORM=${ANDROID_PLATFORM_LEVEL}")
message(WARNING "-DANDROID_ABI=${ANDROID_ABI}")
message(WARNING "-DANDROID_NATIVE_API_LEVEL=${ANDROID_PLATFORM_LEVEL}")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        pocl_rreferenceandroidjavaclient

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

set(BUILD_POCL_ARGS
        -DENABLE_LLVM=0
        -DENABLE_LOADABLE_DRIVERS=0
        -DENABLE_HOST_CPU_DEVICES=0
        -DENABLE_ICD=0
        -DENABLE_PROXY_DEVICE=NO
        -DENABLE_REMOTE_CLIENT=YES
        -DENABLE_HWLOC=0
        -DHOST_DEVICE_BUILD_HASH=00000000
        -DENABLE_POCLCC=0
        -DENABLE_TESTS=0
        -DENABLE_EXAMPLES=0
#        STILL NEEDED?
        -DENABLE_LOCAL_INTEROP=0
        -DCMAKE_BUILD_TYPE=Debug
        -DVISIBILITY_HIDDEN=NO
        -DBUILD_SHARED_LIBS=YES
        -DCMAKE_INSTALL_PREFIX=${CMAKE_CURRENT_BINARY_DIR}/pocl
#        -DCMAKE_MAKE_PROGRAM=${ANDROID_NDK}/prebuilt/linux-x86_64/make
        -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK}/build/cmake/android.toolchain.cmake
        -DANDROID_NDK=${ANDROID_NDK}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM_LEVEL}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_NATIVE_API_LEVEL=${ANDROID_PLATFORM_LEVEL}
        )

ExternalProject_Add(pocl
        SOURCE_DIR ${EXTERNAL_PATH}/pocl
        CMAKE_ARGS ${BUILD_POCL_ARGS}
        CMAKE_GENERATOR "Ninja"
        BUILD_ALWAYS True
        BUILD_BYPRODUCTS ${CMAKE_CURRENT_BINARY_DIR}/pocl/lib/libpocl.so
        )

add_library(libpocl SHARED IMPORTED)
set_target_properties(libpocl PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_BINARY_DIR}/pocl/lib/libpocl.so)

add_dependencies(pocl_rreferenceandroidjavaclient pocl)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        pocl_rreferenceandroidjavaclient

#        ${CMAKE_CURRENT_BINARY_DIR}/pocl/lib/libpocl.so
        libpocl
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})