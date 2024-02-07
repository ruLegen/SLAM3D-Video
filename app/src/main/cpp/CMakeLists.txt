cmake_minimum_required(VERSION 3.22)
project(orbvideoslam)


add_subdirectory(thirdparty/orb3slam)


get_property(dirs
        TARGET ORB_SLAM3
        PROPERTY INCLUDE_DIRECTORIES)

include_directories(
        include
        thirdparty/orb3slam/include
        ${dirs}
)
file(GLOB SOURCE_FILES src/jni/*.cpp)
add_library(${PROJECT_NAME} SHARED ${SOURCE_FILES})

target_link_libraries(${PROJECT_NAME}
        ORB_SLAM3
        )
