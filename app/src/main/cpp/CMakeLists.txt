cmake_minimum_required(VERSION 3.10.2)

project("whisper_task")

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -pthread")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -pthread")

add_library(
        whisper_task
        SHARED
        native-lib.cpp
        whisper.cpp
        ggml.c
)

find_library(log-lib log)

target_link_libraries(whisper_task ${log-lib})