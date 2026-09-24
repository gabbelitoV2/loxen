set(LIBRIST_DIR ${CMAKE_CURRENT_SOURCE_DIR}/librist)
set(LIBRIST_GENERATED_DIR ${CMAKE_CURRENT_BINARY_DIR}/librist)

find_package(Threads REQUIRED)

file(STRINGS ${LIBRIST_DIR}/meson.build LIBRIST_LINE REGEX "^[ \t]*version: '[0-9.]+',")
string(REGEX MATCH "[0-9]+\\.[0-9]+\\.[0-9]+" LIBRIST_VERSION "${LIBRIST_LINE}")
foreach(PART MAJOR MINOR PATCH)
    string(TOLOWER ${PART} PART_LOWER)
    file(STRINGS ${LIBRIST_DIR}/meson.build LIBRIST_LINE REGEX "^librist_api_version_${PART_LOWER} = [0-9]+$")
    string(REGEX MATCH "[0-9]+$" LIBRIST_API_VERSION_${PART} "${LIBRIST_LINE}")
endforeach()
math(EXPR LIBRIST_API_VERSION_NUMBER
    "${LIBRIST_API_VERSION_MAJOR} * 10000 + ${LIBRIST_API_VERSION_MINOR} * 100 + ${LIBRIST_API_VERSION_PATCH}")
set(LIBRIST_API_VERSION 0x${LIBRIST_API_VERSION_NUMBER})
configure_file(${LIBRIST_DIR}/include/librist/version.h.in
    ${LIBRIST_GENERATED_DIR}/include/librist/version.h @ONLY)

set(HAVE_SRP_SUPPORT 1)
configure_file(${LIBRIST_DIR}/include/librist/config.h.in
    ${LIBRIST_GENERATED_DIR}/include/librist/librist_config.h @ONLY)

execute_process(
    COMMAND ${GIT_EXECUTABLE} -C ${LIBRIST_DIR} describe --tags --dirty --match v?.* --always
    OUTPUT_VARIABLE VCS_TAG
    OUTPUT_STRIP_TRAILING_WHITESPACE
    RESULT_VARIABLE LIBRIST_VCS_RESULT
    ERROR_QUIET)
if(NOT LIBRIST_VCS_RESULT EQUAL 0)
    set(VCS_TAG ${LIBRIST_VERSION})
endif()
configure_file(${LIBRIST_DIR}/include/vcs_version.h.in ${LIBRIST_GENERATED_DIR}/include/vcs_version.h @ONLY)

file(CONFIGURE OUTPUT ${LIBRIST_GENERATED_DIR}/config.h CONTENT [[
#pragma once

#define ALLOW_INSECURE_IV_FALLBACK 0
#define HAVE_CLOCK_GETTIME 1
#define HAVE_PTHREADS 1
#define HAVE_SOCK_UN_H 1
#define HAVE_LIBMICROHTTPD 0
#define HAVE_PROMETHEUS_SUPPORT 1
#define MBEDTLS_HAS_MPI_RANDOM 1
#define HAVE_MBEDTLS 1
#define HAVE_NETTLE 0
]])

file(STRINGS ${LIBRIST_DIR}/contrib/mbedtls/meson.build LIBRIST_MBEDCRYPTO_LINES REGEX "'library/[a-z0-9_]+\\.c'")
set(LIBRIST_MBEDCRYPTO_SOURCES)
foreach(LIBRIST_LINE ${LIBRIST_MBEDCRYPTO_LINES})
    string(REGEX MATCH "library/[a-z0-9_]+\\.c" LIBRIST_SOURCE "${LIBRIST_LINE}")
    list(APPEND LIBRIST_MBEDCRYPTO_SOURCES ${LIBRIST_DIR}/contrib/mbedtls/${LIBRIST_SOURCE})
endforeach()

add_library(rist_mbedcrypto STATIC EXCLUDE_FROM_ALL ${LIBRIST_MBEDCRYPTO_SOURCES})
target_include_directories(rist_mbedcrypto PRIVATE
    ${LIBRIST_DIR}/contrib/mbedtls/library
    ${LIBRIST_DIR}/contrib/mbedtls/include)

add_library(rist STATIC EXCLUDE_FROM_ALL
    ${LIBRIST_DIR}/src/crypto/crypto.c
    ${LIBRIST_DIR}/src/crypto/psk.c
    ${LIBRIST_DIR}/src/proto/gre.c
    ${LIBRIST_DIR}/src/proto/rtp.c
    ${LIBRIST_DIR}/src/proto/rist_time.c
    ${LIBRIST_DIR}/src/flow.c
    ${LIBRIST_DIR}/src/rist_pacer.c
    ${LIBRIST_DIR}/src/logging.c
    ${LIBRIST_DIR}/src/network.c
    ${LIBRIST_DIR}/src/adv.c
    ${LIBRIST_DIR}/src/adv_ctrl.c
    ${LIBRIST_DIR}/src/rist.c
    ${LIBRIST_DIR}/src/rist-common.c
    ${LIBRIST_DIR}/src/rist_ref.c
    ${LIBRIST_DIR}/src/rist-thread.c
    ${LIBRIST_DIR}/src/mpegts.c
    ${LIBRIST_DIR}/src/peer.c
    ${LIBRIST_DIR}/src/udp.c
    ${LIBRIST_DIR}/src/transport.c
    ${LIBRIST_DIR}/src/stats.c
    ${LIBRIST_DIR}/src/udpsocket.c
    ${LIBRIST_DIR}/src/libevsocket.c
    ${LIBRIST_DIR}/src/tun_cidr.c
    ${LIBRIST_DIR}/contrib/stdio-shim.c
    ${LIBRIST_DIR}/contrib/time-shim.c
    ${LIBRIST_DIR}/contrib/pthread-shim.c
    ${LIBRIST_DIR}/src/tun_linux.c
    ${LIBRIST_DIR}/contrib/linux-crypto.c
    ${LIBRIST_DIR}/src/crypto/random.c
    ${LIBRIST_DIR}/src/proto/eap.c
    ${LIBRIST_DIR}/src/crypto/srp_constants.c
    ${LIBRIST_DIR}/src/crypto/srp.c
    ${LIBRIST_DIR}/src/crypto/eap_v4_crypto.c
    ${LIBRIST_DIR}/contrib/contrib_cJSON/cjson/cJSON.c
    ${LIBRIST_DIR}/contrib/lz4/lz4.c)
target_compile_definitions(rist PRIVATE CJSON_HIDE_SYMBOLS)
target_include_directories(rist
    PUBLIC
    ${LIBRIST_DIR}/include
    ${LIBRIST_GENERATED_DIR}/include
    ${LIBRIST_GENERATED_DIR}/include/librist
    PRIVATE
    ${LIBRIST_GENERATED_DIR}
    ${LIBRIST_DIR}
    ${LIBRIST_DIR}/src
    ${LIBRIST_DIR}/include/librist
    ${LIBRIST_DIR}/contrib
    ${LIBRIST_DIR}/contrib/mbedtls/library
    ${LIBRIST_DIR}/contrib/mbedtls/include
    ${LIBRIST_DIR}/contrib/contrib_cJSON
    ${LIBRIST_DIR}/contrib/lz4)
target_link_libraries(rist PRIVATE rist_mbedcrypto Threads::Threads)

foreach(LIBRIST_TARGET rist_mbedcrypto rist)
    set_target_properties(${LIBRIST_TARGET} PROPERTIES
        C_STANDARD 99
        C_EXTENSIONS OFF
        C_VISIBILITY_PRESET hidden)
    target_compile_definitions(${LIBRIST_TARGET} PRIVATE _GNU_SOURCE _FILE_OFFSET_BITS=64 LINUX_CRYPTO)
endforeach()
