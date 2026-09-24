cmake_minimum_required(VERSION 3.22.1)
project(moblin_native C CXX)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(Git REQUIRED)

set(ENABLE_PROGRAMS OFF CACHE BOOL "" FORCE)
set(ENABLE_TESTING OFF CACHE BOOL "" FORCE)
set(GEN_FILES OFF CACHE BOOL "" FORCE)
set(MBEDTLS_FATAL_WARNINGS OFF CACHE BOOL "" FORCE)
set(USE_SHARED_MBEDTLS_LIBRARY OFF CACHE BOOL "" FORCE)
set(USE_STATIC_MBEDTLS_LIBRARY ON CACHE BOOL "" FORCE)
set(DISABLE_PACKAGE_CONFIG_AND_INSTALL OFF CACHE BOOL "" FORCE)
set(MBEDTLS_USER_CONFIG_FILE ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls_user_config.h CACHE FILEPATH "" FORCE)
add_subdirectory(mbedtls EXCLUDE_FROM_ALL)

set(USE_ENCLIB mbedtls CACHE STRING "" FORCE)
set(SSL_INCLUDE_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/mbedtls/include)
set(SSL_LIBRARY_DIRS ${CMAKE_CURRENT_BINARY_DIR}/mbedtls/library)
set(SSL_LIBRARIES mbedtls mbedx509 mbedcrypto)
set(ENABLE_APPS OFF CACHE BOOL "" FORCE)
set(ENABLE_SHARED OFF CACHE BOOL "" FORCE)
set(ENABLE_STATIC ON CACHE BOOL "" FORCE)
set(ENABLE_MAXREXMITBW ON CACHE BOOL "" FORCE)
set(ENABLE_PKTINFO ON CACHE BOOL "" FORCE)
set(ENABLE_LOGGING OFF CACHE BOOL "" FORCE)
set(ENABLE_ENCRYPTION ON CACHE BOOL "" FORCE)
add_subdirectory(srt EXCLUDE_FROM_ALL)
target_compile_definitions(srt_virtual PRIVATE MBEDTLS_USER_CONFIG_FILE="${MBEDTLS_USER_CONFIG_FILE}")

set(LIBSRTP_DIR ${CMAKE_CURRENT_SOURCE_DIR}/libdatachannel/deps/libsrtp)
set(LIBSRTP_PATCH ${CMAKE_CURRENT_SOURCE_DIR}/patches/libsrtp.patch)
if(NOT EXISTS ${LIBSRTP_DIR}/srtp/srtp.c)
    message(FATAL_ERROR "${LIBSRTP_DIR} is missing, run git submodule update --init --recursive")
endif()
execute_process(
    COMMAND ${GIT_EXECUTABLE} rev-parse --absolute-git-dir
    WORKING_DIRECTORY ${LIBSRTP_DIR}
    OUTPUT_VARIABLE LIBSRTP_GIT_DIR
    OUTPUT_STRIP_TRAILING_WHITESPACE
    COMMAND_ERROR_IS_FATAL ANY)
file(LOCK ${LIBSRTP_GIT_DIR}/moblin-patch.lock)
execute_process(
    COMMAND ${GIT_EXECUTABLE} apply --reverse --check ${LIBSRTP_PATCH}
    WORKING_DIRECTORY ${LIBSRTP_DIR}
    RESULT_VARIABLE LIBSRTP_PATCHED
    OUTPUT_QUIET
    ERROR_QUIET)
if(NOT LIBSRTP_PATCHED EQUAL 0)
    execute_process(
        COMMAND ${GIT_EXECUTABLE} apply ${LIBSRTP_PATCH}
        WORKING_DIRECTORY ${LIBSRTP_DIR}
        COMMAND_ERROR_IS_FATAL ANY)
endif()
file(LOCK ${LIBSRTP_GIT_DIR}/moblin-patch.lock RELEASE)

list(APPEND CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/cmake)
add_library(MbedTLS::MbedTLS ALIAS mbedtls)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_DEPS_LIBS OFF CACHE BOOL "" FORCE)
set(USE_MBEDTLS ON CACHE BOOL "" FORCE)
set(NO_WEBSOCKET ON CACHE BOOL "" FORCE)
set(NO_EXAMPLES ON CACHE BOOL "" FORCE)
set(NO_TESTS ON CACHE BOOL "" FORCE)
set(LIBSRTP_TEST_APPS OFF CACHE BOOL "" FORCE)
add_subdirectory(libdatachannel EXCLUDE_FROM_ALL)

include(cmake/librist.cmake)

file(GLOB MOBLIN_SRT_JNI ${CMAKE_CURRENT_SOURCE_DIR}/jni/*.cpp)
if(MOBLIN_SRT_JNI)
    add_library(moblin_srt SHARED ${MOBLIN_SRT_JNI})
    target_compile_definitions(moblin_srt PRIVATE ENABLE_MAXREXMITBW=1)
    target_include_directories(moblin_srt PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/srt/srtcore
        ${CMAKE_CURRENT_BINARY_DIR}/srt)
    target_link_libraries(moblin_srt srt_static mbedtls mbedx509 mbedcrypto log)
endif()

file(GLOB MOBLIN_DATACHANNEL_JNI ${CMAKE_CURRENT_SOURCE_DIR}/jni/datachannel/*.cpp)
if(MOBLIN_DATACHANNEL_JNI)
    add_library(moblin_datachannel SHARED ${MOBLIN_DATACHANNEL_JNI})
    target_link_libraries(moblin_datachannel datachannel-static log)
    target_link_options(moblin_datachannel PRIVATE -Wl,--exclude-libs,ALL)
endif()

file(GLOB MOBLIN_RIST_JNI ${CMAKE_CURRENT_SOURCE_DIR}/jni/rist/*.cpp)
if(MOBLIN_RIST_JNI)
    add_library(moblin_rist SHARED ${MOBLIN_RIST_JNI})
    set_target_properties(moblin_rist PROPERTIES CXX_VISIBILITY_PRESET hidden VISIBILITY_INLINES_HIDDEN ON)
    target_link_libraries(moblin_rist rist android log)
    target_link_options(moblin_rist PRIVATE -Wl,--exclude-libs,ALL -Wl,--wrap=setsockopt)
endif()
