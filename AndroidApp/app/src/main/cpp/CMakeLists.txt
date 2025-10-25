cmake_minimum_required(VERSION 3.10)
project(client_jni_project)

add_library(client_jni SHARED client_jni.cpp)

find_library(log-lib log)

target_link_libraries(client_jni
        ${log-lib}
        m
        atomic
)
