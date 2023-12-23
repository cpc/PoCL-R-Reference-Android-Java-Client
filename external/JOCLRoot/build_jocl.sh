#!/usr/bin/env bash

## A script to build JOCL for Android devices.
## To use this script, first set the parameters below
## to match your application. As you can see, it
## assumes you have the android ndk installed in the default
## location.
## Once parameters match your case, run this script
## the directory it is found.


ANDROID_NDK="${HOME}/Android/Sdk/ndk/25.1.8937393"
ANDROID_PLATFORM=24
ANDROID_ABI="arm64-v8a"
ANDROID_NATIVE_API_LEVEL=24
# todo: set this to Release when ready
BUILD_TYPE="Debug"

cd JOCL || exit 1
rm -rf build/
mkdir build/
cd build/ || exit 1

cmake -G "Ninja" \
    -DANDROID_NDK="${ANDROID_NDK}" \
    -DANDROID_ABI=${ANDROID_ABI} \
    -DANDROID_PLATFORM=${ANDROID_PLATFORM} \
    -DANDROID_NATIVE_API_LEVEL=${ANDROID_NATIVE_API_LEVEL} \
    -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
    -DCMAKE_BUILD_TYPE=${BUILD_TYPE} \
    ..

ninja

cd ..

# make and copy jars to destination
mvn clean install -DskipTests

cp target/*.jar ../../../android/app/libs/

