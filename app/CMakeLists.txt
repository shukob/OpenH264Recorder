# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

set(openh264_DIR ${CMAKE_SOURCE_DIR}/../openh264)
add_library(libopenh264 SHARED IMPORTED)
set_target_properties(libopenh264 PROPERTIES IMPORTED_LOCATION
    ${openh264_DIR}/lib/${ANDROID_ABI}/libopenh264.so)

add_subdirectory(${CMAKE_SOURCE_DIR}/src/main/cpp/android_aac_enc)
add_subdirectory(${CMAKE_SOURCE_DIR}/src/main/cpp/libyuv)

add_library( # Sets the name of the library.


             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.

             native-lib
             SHARED
             src/main/cpp/exports.cpp
             src/main/cpp/aac-enc.c
             )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

                # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_include_directories(native-lib PRIVATE
                           ${openh264_DIR}/include
                           src/main/cpp/libyuv/include
                           src/main/cpp/android_aac_enc)

target_link_libraries( # Specifies the target library.
                       native-lib
                       libopenh264
                       aac_enc
                       libyuv
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )

SET( CMAKE_EXE_LINKER FLAGS "-Wl,--build-id ${CMAKE_EXE_LINKER_FLAGS}")
