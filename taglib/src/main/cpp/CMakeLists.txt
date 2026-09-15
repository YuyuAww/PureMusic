cmake_minimum_required(VERSION 3.22.1)
project(puremusic_taglib)

find_library(log-lib log)

include(FetchContent)
FetchContent_Declare(
    taglib
    GIT_REPOSITORY https://github.com/taglib/taglib.git
    GIT_TAG v2.3.2
)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_BINDINGS OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(taglib)

add_library(puremusic_taglib SHARED taglib_jni.cpp)
target_link_libraries(puremusic_taglib PRIVATE tag ${log-lib})
target_compile_features(puremusic_taglib PRIVATE cxx_std_17)
