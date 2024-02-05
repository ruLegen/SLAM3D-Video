cmake_minimum_required(VERSION 3.22)
project(orbvideoslam)


add_subdirectory(thirdparty/orb3slam)


get_property(dirs
        TARGET ORB_SLAM3
        PROPERTY INCLUDE_DIRECTORIES)

MESSAGE("AAAAAAAAAAA ${dirs}")
include_directories(
        include
        thirdparty/orb3slam/include
        ${dirs}
)

add_library(${PROJECT_NAME} SHARED
        src/jni/test.cpp
        )

target_link_libraries(${PROJECT_NAME}
        ORB_SLAM3
        )
