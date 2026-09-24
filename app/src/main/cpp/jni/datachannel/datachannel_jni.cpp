#include <android/log.h>
#include <jni.h>
#include <pthread.h>
#include <sys/prctl.h>

#include <cstdint>
#include <cstring>
#include <optional>
#include <string>
#include <vector>

#include "rtc/rtc.h"

static_assert(RTC_NEW == 0, "RTC_NEW");
static_assert(RTC_CONNECTING == 1, "RTC_CONNECTING");
static_assert(RTC_CONNECTED == 2, "RTC_CONNECTED");
static_assert(RTC_DISCONNECTED == 3, "RTC_DISCONNECTED");
static_assert(RTC_FAILED == 4, "RTC_FAILED");
static_assert(RTC_CLOSED == 5, "RTC_CLOSED");
static_assert(RTC_ICE_NEW == 0, "RTC_ICE_NEW");
static_assert(RTC_ICE_CHECKING == 1, "RTC_ICE_CHECKING");
static_assert(RTC_ICE_CONNECTED == 2, "RTC_ICE_CONNECTED");
static_assert(RTC_ICE_COMPLETED == 3, "RTC_ICE_COMPLETED");
static_assert(RTC_ICE_FAILED == 4, "RTC_ICE_FAILED");
static_assert(RTC_ICE_DISCONNECTED == 5, "RTC_ICE_DISCONNECTED");
static_assert(RTC_ICE_CLOSED == 6, "RTC_ICE_CLOSED");
static_assert(RTC_GATHERING_NEW == 0, "RTC_GATHERING_NEW");
static_assert(RTC_GATHERING_INPROGRESS == 1, "RTC_GATHERING_INPROGRESS");
static_assert(RTC_GATHERING_COMPLETE == 2, "RTC_GATHERING_COMPLETE");
static_assert(RTC_SIGNALING_STABLE == 0, "RTC_SIGNALING_STABLE");
static_assert(RTC_SIGNALING_HAVE_LOCAL_OFFER == 1, "RTC_SIGNALING_HAVE_LOCAL_OFFER");
static_assert(RTC_SIGNALING_HAVE_REMOTE_OFFER == 2, "RTC_SIGNALING_HAVE_REMOTE_OFFER");
static_assert(RTC_SIGNALING_HAVE_LOCAL_PRANSWER == 3, "RTC_SIGNALING_HAVE_LOCAL_PRANSWER");
static_assert(RTC_SIGNALING_HAVE_REMOTE_PRANSWER == 4, "RTC_SIGNALING_HAVE_REMOTE_PRANSWER");
static_assert(RTC_LOG_NONE == 0, "RTC_LOG_NONE");
static_assert(RTC_LOG_FATAL == 1, "RTC_LOG_FATAL");
static_assert(RTC_LOG_ERROR == 2, "RTC_LOG_ERROR");
static_assert(RTC_LOG_WARNING == 3, "RTC_LOG_WARNING");
static_assert(RTC_LOG_INFO == 4, "RTC_LOG_INFO");
static_assert(RTC_LOG_DEBUG == 5, "RTC_LOG_DEBUG");
static_assert(RTC_LOG_VERBOSE == 6, "RTC_LOG_VERBOSE");
static_assert(RTC_CERTIFICATE_DEFAULT == 0, "RTC_CERTIFICATE_DEFAULT");
static_assert(RTC_CERTIFICATE_ECDSA == 1, "RTC_CERTIFICATE_ECDSA");
static_assert(RTC_CERTIFICATE_RSA == 2, "RTC_CERTIFICATE_RSA");
static_assert(RTC_CODEC_H264 == 0, "RTC_CODEC_H264");
static_assert(RTC_CODEC_VP8 == 1, "RTC_CODEC_VP8");
static_assert(RTC_CODEC_VP9 == 2, "RTC_CODEC_VP9");
static_assert(RTC_CODEC_H265 == 3, "RTC_CODEC_H265");
static_assert(RTC_CODEC_AV1 == 4, "RTC_CODEC_AV1");
static_assert(RTC_CODEC_OPUS == 128, "RTC_CODEC_OPUS");
static_assert(RTC_CODEC_PCMU == 129, "RTC_CODEC_PCMU");
static_assert(RTC_CODEC_PCMA == 130, "RTC_CODEC_PCMA");
static_assert(RTC_CODEC_AAC == 131, "RTC_CODEC_AAC");
static_assert(RTC_CODEC_G722 == 132, "RTC_CODEC_G722");
static_assert(RTC_DIRECTION_UNKNOWN == 0, "RTC_DIRECTION_UNKNOWN");
static_assert(RTC_DIRECTION_SENDONLY == 1, "RTC_DIRECTION_SENDONLY");
static_assert(RTC_DIRECTION_RECVONLY == 2, "RTC_DIRECTION_RECVONLY");
static_assert(RTC_DIRECTION_SENDRECV == 3, "RTC_DIRECTION_SENDRECV");
static_assert(RTC_DIRECTION_INACTIVE == 4, "RTC_DIRECTION_INACTIVE");
static_assert(RTC_TRANSPORT_POLICY_ALL == 0, "RTC_TRANSPORT_POLICY_ALL");
static_assert(RTC_TRANSPORT_POLICY_RELAY == 1, "RTC_TRANSPORT_POLICY_RELAY");
static_assert(RTC_ERR_SUCCESS == 0, "RTC_ERR_SUCCESS");
static_assert(RTC_ERR_INVALID == -1, "RTC_ERR_INVALID");
static_assert(RTC_ERR_FAILURE == -2, "RTC_ERR_FAILURE");
static_assert(RTC_ERR_NOT_AVAIL == -3, "RTC_ERR_NOT_AVAIL");
static_assert(RTC_ERR_TOO_SMALL == -4, "RTC_ERR_TOO_SMALL");
static_assert(RTC_NAL_SEPARATOR_LENGTH == 0, "RTC_NAL_SEPARATOR_LENGTH");
static_assert(RTC_NAL_SEPARATOR_LONG_START_SEQUENCE == 1, "RTC_NAL_SEPARATOR_LONG_START_SEQUENCE");
static_assert(RTC_NAL_SEPARATOR_SHORT_START_SEQUENCE == 2, "RTC_NAL_SEPARATOR_SHORT_START_SEQUENCE");
static_assert(RTC_NAL_SEPARATOR_START_SEQUENCE == 3, "RTC_NAL_SEPARATOR_START_SEQUENCE");
static_assert(RTC_OBU_PACKETIZED_OBU == 0, "RTC_OBU_PACKETIZED_OBU");
static_assert(RTC_OBU_PACKETIZED_TEMPORAL_UNIT == 1, "RTC_OBU_PACKETIZED_TEMPORAL_UNIT");
static_assert(RTC_DEFAULT_MTU == 1280, "RTC_DEFAULT_MTU");
static_assert(RTC_DEFAULT_MAX_FRAGMENT_SIZE == 1220, "RTC_DEFAULT_MAX_FRAGMENT_SIZE");
static_assert(RTC_DEFAULT_MAX_STORED_PACKET_COUNT == 512, "RTC_DEFAULT_MAX_STORED_PACKET_COUNT");
static_assert(sizeof(void*) <= sizeof(jlong), "user pointer handle");

namespace {

const char* const logTag = "MoblinDataChannel";
const char* const nativeClassName = "com/moblin/android/platform/datachannel/DataChannelNative";

enum PacketizerKind {
    packetizerH264 = 0,
    packetizerH265 = 1,
    packetizerAv1 = 2,
    packetizerVp8 = 3,
    packetizerVp9 = 4,
    packetizerOpus = 5,
    packetizerAac = 6,
    packetizerPcmu = 7,
    packetizerPcma = 8,
    packetizerG722 = 9,
};

enum PacketizerField {
    fieldSsrc = 0,
    fieldPayloadType,
    fieldClockRate,
    fieldSequenceNumber,
    fieldTimestamp,
    fieldMaxFragmentSize,
    fieldNalSeparator,
    fieldObuPacketization,
    fieldPlayoutDelayId,
    fieldPlayoutDelayMin,
    fieldPlayoutDelayMax,
    fieldColorSpaceId,
    fieldColorChromaSitingHorz,
    fieldColorChromaSitingVert,
    fieldColorRange,
    fieldColorPrimaries,
    fieldColorTransfer,
    fieldColorMatrix,
    numberOfPacketizerFields,
};

enum ConfigurationField {
    configCertificateType = 0,
    configIceTransportPolicy,
    configEnableIceTcp,
    configEnableIceUdpMux,
    configDisableAutoNegotiation,
    configForceMediaTransport,
    configPortRangeBegin,
    configPortRangeEnd,
    configMtu,
    configMaxMessageSize,
    configDisableFingerprintVerification,
    numberOfConfigurationFields,
};

JavaVM* javaVm = nullptr;
pthread_key_t detachKey;
jclass nativeClass = nullptr;
jmethodID onLogMethod = nullptr;
jmethodID onStateChangeMethod = nullptr;
jmethodID onGatheringStateChangeMethod = nullptr;
jmethodID onTrackMethod = nullptr;
jmethodID onOpenMethod = nullptr;
jmethodID onClosedMethod = nullptr;
jmethodID onErrorMethod = nullptr;
jmethodID onFrameMethod = nullptr;
jmethodID onPliMethod = nullptr;
jmethodID onRembMethod = nullptr;

void clearPendingException(JNIEnv* env)
{
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

void detachCurrentThread(void*)
{
    if (javaVm != nullptr) {
        javaVm->DetachCurrentThread();
    }
}

JNIEnv* getAttachedEnv()
{
    if (javaVm == nullptr) {
        return nullptr;
    }
    JNIEnv* env = nullptr;
    jint status = javaVm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (status == JNI_OK) {
        return env;
    }
    if (status != JNI_EDETACHED) {
        return nullptr;
    }
    char name[17];
    memset(name, 0, sizeof(name));
    if (prctl(PR_GET_NAME, reinterpret_cast<unsigned long>(name), 0, 0, 0) != 0 || name[0] == 0) {
        strncpy(name, logTag, sizeof(name) - 1);
    }
    name[sizeof(name) - 1] = 0;
    for (size_t i = 0; name[i] != 0; i++) {
        if (static_cast<unsigned char>(name[i]) >= 0x80) {
            name[i] = '?';
        }
    }
    JavaVMAttachArgs args;
    args.version = JNI_VERSION_1_6;
    args.name = name;
    args.group = nullptr;
    if (javaVm->AttachCurrentThreadAsDaemon(&env, &args) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Failed to attach thread %s", name);
        return nullptr;
    }
    pthread_setspecific(detachKey, env);
    return env;
}

jlong toHandle(void* ptr)
{
    return static_cast<jlong>(reinterpret_cast<intptr_t>(ptr));
}

void* fromHandle(jlong handle)
{
    return reinterpret_cast<void*>(static_cast<intptr_t>(handle));
}

std::optional<std::string> readString(JNIEnv* env, jbyteArray bytes)
{
    if (bytes == nullptr) {
        return std::nullopt;
    }
    jsize length = env->GetArrayLength(bytes);
    std::string value(static_cast<size_t>(length), '\0');
    if (length > 0) {
        env->GetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte*>(value.data()));
    }
    size_t end = value.find('\0');
    if (end != std::string::npos) {
        value.resize(end);
    }
    return value;
}

const char* cString(const std::optional<std::string>& value)
{
    return value ? value->c_str() : nullptr;
}

jbyteArray newByteArray(JNIEnv* env, const char* data, size_t size)
{
    if (data == nullptr) {
        return nullptr;
    }
    jbyteArray array = env->NewByteArray(static_cast<jsize>(size));
    if (array == nullptr) {
        clearPendingException(env);
        return nullptr;
    }
    if (size > 0) {
        env->SetByteArrayRegion(array, 0, static_cast<jsize>(size), reinterpret_cast<const jbyte*>(data));
    }
    return array;
}

jbyteArray newCStringByteArray(JNIEnv* env, const char* value)
{
    if (value == nullptr) {
        return nullptr;
    }
    return newByteArray(env, value, strlen(value));
}

template <typename Function>
jint withOutputBuffer(JNIEnv* env, jbyteArray buffer, jint size, Function function)
{
    if (buffer == nullptr) {
        return function(nullptr, size);
    }
    if (size < 0 || size > env->GetArrayLength(buffer)) {
        return RTC_ERR_INVALID;
    }
    std::vector<char> data(static_cast<size_t>(size));
    if (size > 0) {
        env->GetByteArrayRegion(buffer, 0, size, reinterpret_cast<jbyte*>(data.data()));
    }
    jint result = function(data.data(), size);
    if (size > 0) {
        env->SetByteArrayRegion(buffer, 0, size, reinterpret_cast<const jbyte*>(data.data()));
    }
    return result;
}

void RTC_API logTrampoline(rtcLogLevel level, const char* message)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onLogMethod == nullptr) {
        return;
    }
    jbyteArray bytes = newCStringByteArray(env, message);
    env->CallStaticVoidMethod(nativeClass, onLogMethod, static_cast<jint>(level), bytes);
    clearPendingException(env);
    if (bytes != nullptr) {
        env->DeleteLocalRef(bytes);
    }
}

void RTC_API stateChangeTrampoline(int pc, rtcState state, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onStateChangeMethod == nullptr) {
        return;
    }
    env->CallStaticVoidMethod(nativeClass, onStateChangeMethod, pc, static_cast<jint>(state), toHandle(ptr));
    clearPendingException(env);
}

void RTC_API gatheringStateChangeTrampoline(int pc, rtcGatheringState state, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onGatheringStateChangeMethod == nullptr) {
        return;
    }
    env->CallStaticVoidMethod(nativeClass,
                              onGatheringStateChangeMethod,
                              pc,
                              static_cast<jint>(state),
                              toHandle(ptr));
    clearPendingException(env);
}

void RTC_API trackTrampoline(int pc, int tr, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onTrackMethod == nullptr) {
        return;
    }
    env->CallStaticVoidMethod(nativeClass, onTrackMethod, pc, tr, toHandle(ptr));
    clearPendingException(env);
}

void RTC_API openTrampoline(int id, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onOpenMethod == nullptr) {
        return;
    }
    env->CallStaticVoidMethod(nativeClass, onOpenMethod, id, toHandle(ptr));
    clearPendingException(env);
}

void RTC_API closedTrampoline(int id, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onClosedMethod == nullptr) {
        return;
    }
    env->CallStaticVoidMethod(nativeClass, onClosedMethod, id, toHandle(ptr));
    clearPendingException(env);
}

void RTC_API errorTrampoline(int id, const char* error, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onErrorMethod == nullptr) {
        return;
    }
    jbyteArray bytes = newCStringByteArray(env, error);
    env->CallStaticVoidMethod(nativeClass, onErrorMethod, id, bytes, toHandle(ptr));
    clearPendingException(env);
    if (bytes != nullptr) {
        env->DeleteLocalRef(bytes);
    }
}

void RTC_API frameTrampoline(int tr, const char* data, int size, const rtcFrameInfo* info, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onFrameMethod == nullptr) {
        return;
    }
    jbyteArray bytes = size >= 0 ? newByteArray(env, data, static_cast<size_t>(size)) : nullptr;
    env->CallStaticVoidMethod(nativeClass,
                              onFrameMethod,
                              tr,
                              bytes,
                              size,
                              static_cast<jboolean>(info != nullptr),
                              static_cast<jint>(info != nullptr ? info->timestamp : 0),
                              static_cast<jint>(info != nullptr ? info->payloadType : 0),
                              static_cast<jdouble>(info != nullptr ? info->timestampSeconds : -1.0),
                              toHandle(ptr));
    clearPendingException(env);
    if (bytes != nullptr) {
        env->DeleteLocalRef(bytes);
    }
}

void RTC_API pliTrampoline(int tr, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onPliMethod == nullptr) {
        return;
    }
    env->CallStaticVoidMethod(nativeClass, onPliMethod, tr, toHandle(ptr));
    clearPendingException(env);
}

void RTC_API rembTrampoline(int tr, unsigned int bitrate, void* ptr)
{
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr || onRembMethod == nullptr) {
        return;
    }
    env->CallStaticVoidMethod(nativeClass, onRembMethod, tr, static_cast<jint>(bitrate), toHandle(ptr));
    clearPendingException(env);
}

void initLogger(JNIEnv*, jclass, jint level, jboolean enabled)
{
    rtcInitLogger(static_cast<rtcLogLevel>(level), enabled ? logTrampoline : nullptr);
}

void setUserPointer(JNIEnv*, jclass, jint id, jlong handle)
{
    rtcSetUserPointer(id, fromHandle(handle));
}

jlong getUserPointer(JNIEnv*, jclass, jint id)
{
    return toHandle(rtcGetUserPointer(id));
}

jint createPeerConnection(JNIEnv* env,
                          jclass,
                          jobjectArray iceServers,
                          jint iceServersCount,
                          jbyteArray proxyServer,
                          jbyteArray bindAddress,
                          jbyteArray certificatePemFile,
                          jbyteArray keyPemFile,
                          jbyteArray keyPemPass,
                          jintArray fields)
{
    if (fields == nullptr || env->GetArrayLength(fields) < numberOfConfigurationFields) {
        return RTC_ERR_INVALID;
    }
    jint values[numberOfConfigurationFields];
    env->GetIntArrayRegion(fields, 0, numberOfConfigurationFields, values);
    std::vector<std::optional<std::string>> iceServerStrings;
    std::vector<const char*> iceServerPointers;
    if (iceServers != nullptr) {
        jsize length = env->GetArrayLength(iceServers);
        if (iceServersCount < 0 || iceServersCount > length) {
            return RTC_ERR_INVALID;
        }
        iceServerStrings.reserve(static_cast<size_t>(iceServersCount));
        for (jsize i = 0; i < iceServersCount; i++) {
            auto element = static_cast<jbyteArray>(env->GetObjectArrayElement(iceServers, i));
            if (element == nullptr) {
                return RTC_ERR_INVALID;
            }
            iceServerStrings.push_back(readString(env, element));
            env->DeleteLocalRef(element);
        }
        for (const auto& value : iceServerStrings) {
            iceServerPointers.push_back(cString(value));
        }
    } else if (iceServersCount != 0) {
        return RTC_ERR_INVALID;
    }
    auto proxyServerString = readString(env, proxyServer);
    auto bindAddressString = readString(env, bindAddress);
    auto certificatePemFileString = readString(env, certificatePemFile);
    auto keyPemFileString = readString(env, keyPemFile);
    auto keyPemPassString = readString(env, keyPemPass);
    rtcConfiguration config;
    memset(&config, 0, sizeof(config));
    config.iceServers = iceServerPointers.empty() ? nullptr : iceServerPointers.data();
    config.iceServersCount = static_cast<int>(iceServerPointers.size());
    config.proxyServer = cString(proxyServerString);
    config.bindAddress = cString(bindAddressString);
    config.certificateType = static_cast<rtcCertificateType>(values[configCertificateType]);
    config.certificatePemFile = cString(certificatePemFileString);
    config.keyPemFile = cString(keyPemFileString);
    config.keyPemPass = cString(keyPemPassString);
    config.iceTransportPolicy = static_cast<rtcTransportPolicy>(values[configIceTransportPolicy]);
    config.enableIceTcp = values[configEnableIceTcp] != 0;
    config.enableIceUdpMux = values[configEnableIceUdpMux] != 0;
    config.disableAutoNegotiation = values[configDisableAutoNegotiation] != 0;
    config.forceMediaTransport = values[configForceMediaTransport] != 0;
    config.portRangeBegin = static_cast<uint16_t>(values[configPortRangeBegin]);
    config.portRangeEnd = static_cast<uint16_t>(values[configPortRangeEnd]);
    config.mtu = values[configMtu];
    config.maxMessageSize = values[configMaxMessageSize];
    config.disableFingerprintVerification = values[configDisableFingerprintVerification] != 0;
    return rtcCreatePeerConnection(&config);
}

jint closePeerConnection(JNIEnv*, jclass, jint pc)
{
    return rtcClosePeerConnection(pc);
}

jint deletePeerConnection(JNIEnv*, jclass, jint pc)
{
    return rtcDeletePeerConnection(pc);
}

jint setStateChangeCallback(JNIEnv*, jclass, jint pc, jboolean enabled)
{
    return rtcSetStateChangeCallback(pc, enabled ? stateChangeTrampoline : nullptr);
}

jint setGatheringStateChangeCallback(JNIEnv*, jclass, jint pc, jboolean enabled)
{
    return rtcSetGatheringStateChangeCallback(pc, enabled ? gatheringStateChangeTrampoline : nullptr);
}

jint setLocalDescription(JNIEnv* env, jclass, jint pc, jbyteArray type)
{
    auto typeString = readString(env, type);
    return rtcSetLocalDescription(pc, cString(typeString));
}

jint setRemoteDescription(JNIEnv* env, jclass, jint pc, jbyteArray sdp, jbyteArray type)
{
    auto sdpString = readString(env, sdp);
    auto typeString = readString(env, type);
    return rtcSetRemoteDescription(pc, cString(sdpString), cString(typeString));
}

jint addRemoteCandidate(JNIEnv* env, jclass, jint pc, jbyteArray cand, jbyteArray mid)
{
    auto candString = readString(env, cand);
    auto midString = readString(env, mid);
    return rtcAddRemoteCandidate(pc, cString(candString), cString(midString));
}

jint getLocalDescription(JNIEnv* env, jclass, jint pc, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [pc](char* data, int length) {
        return rtcGetLocalDescription(pc, data, length);
    });
}

jint getRemoteDescription(JNIEnv* env, jclass, jint pc, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [pc](char* data, int length) {
        return rtcGetRemoteDescription(pc, data, length);
    });
}

jint getLocalDescriptionType(JNIEnv* env, jclass, jint pc, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [pc](char* data, int length) {
        return rtcGetLocalDescriptionType(pc, data, length);
    });
}

jint getRemoteDescriptionType(JNIEnv* env, jclass, jint pc, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [pc](char* data, int length) {
        return rtcGetRemoteDescriptionType(pc, data, length);
    });
}

jint getLocalAddress(JNIEnv* env, jclass, jint pc, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [pc](char* data, int length) {
        return rtcGetLocalAddress(pc, data, length);
    });
}

jint getRemoteAddress(JNIEnv* env, jclass, jint pc, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [pc](char* data, int length) {
        return rtcGetRemoteAddress(pc, data, length);
    });
}

jint getSelectedCandidatePair(JNIEnv* env,
                              jclass,
                              jint pc,
                              jbyteArray local,
                              jint localSize,
                              jbyteArray remote,
                              jint remoteSize)
{
    return withOutputBuffer(env, local, localSize, [&](char* localData, int localLength) {
        return withOutputBuffer(env, remote, remoteSize, [&](char* remoteData, int remoteLength) {
            return rtcGetSelectedCandidatePair(pc, localData, localLength, remoteData, remoteLength);
        });
    });
}

jint setOpenCallback(JNIEnv*, jclass, jint id, jboolean enabled)
{
    return rtcSetOpenCallback(id, enabled ? openTrampoline : nullptr);
}

jint setClosedCallback(JNIEnv*, jclass, jint id, jboolean enabled)
{
    return rtcSetClosedCallback(id, enabled ? closedTrampoline : nullptr);
}

jint setErrorCallback(JNIEnv*, jclass, jint id, jboolean enabled)
{
    return rtcSetErrorCallback(id, enabled ? errorTrampoline : nullptr);
}

jint sendMessage(JNIEnv* env, jclass, jint id, jbyteArray data, jint size)
{
    if (data == nullptr) {
        return rtcSendMessage(id, nullptr, size);
    }
    if (size < 0 || size > env->GetArrayLength(data)) {
        return RTC_ERR_INVALID;
    }
    thread_local std::vector<char> buffer;
    buffer.resize(static_cast<size_t>(size));
    if (size > 0) {
        env->GetByteArrayRegion(data, 0, size, reinterpret_cast<jbyte*>(buffer.data()));
    }
    return rtcSendMessage(id, buffer.data(), size);
}

jint closeId(JNIEnv*, jclass, jint id)
{
    return rtcClose(id);
}

jint deleteId(JNIEnv*, jclass, jint id)
{
    return rtcDelete(id);
}

jboolean isOpen(JNIEnv*, jclass, jint id)
{
    return static_cast<jboolean>(rtcIsOpen(id));
}

jboolean isClosed(JNIEnv*, jclass, jint id)
{
    return static_cast<jboolean>(rtcIsClosed(id));
}

jint setTrackCallback(JNIEnv*, jclass, jint pc, jboolean enabled)
{
    return rtcSetTrackCallback(pc, enabled ? trackTrampoline : nullptr);
}

jint addTrack(JNIEnv* env, jclass, jint pc, jbyteArray mediaDescriptionSdp)
{
    auto sdp = readString(env, mediaDescriptionSdp);
    return rtcAddTrack(pc, cString(sdp));
}

jint addTrackEx(JNIEnv* env,
                jclass,
                jint pc,
                jint direction,
                jint codec,
                jint payloadType,
                jint ssrc,
                jbyteArray mid,
                jbyteArray name,
                jbyteArray msid,
                jbyteArray trackId,
                jbyteArray profile)
{
    auto midString = readString(env, mid);
    auto nameString = readString(env, name);
    auto msidString = readString(env, msid);
    auto trackIdString = readString(env, trackId);
    auto profileString = readString(env, profile);
    rtcTrackInit init;
    memset(&init, 0, sizeof(init));
    init.direction = static_cast<rtcDirection>(direction);
    init.codec = static_cast<rtcCodec>(codec);
    init.payloadType = payloadType;
    init.ssrc = static_cast<uint32_t>(ssrc);
    init.mid = cString(midString);
    init.name = cString(nameString);
    init.msid = cString(msidString);
    init.trackId = cString(trackIdString);
    init.profile = cString(profileString);
    return rtcAddTrackEx(pc, &init);
}

jint deleteTrack(JNIEnv*, jclass, jint tr)
{
    return rtcDeleteTrack(tr);
}

jint getTrackDescription(JNIEnv* env, jclass, jint tr, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [tr](char* data, int length) {
        return rtcGetTrackDescription(tr, data, length);
    });
}

jint getTrackMid(JNIEnv* env, jclass, jint tr, jbyteArray buffer, jint size)
{
    return withOutputBuffer(env, buffer, size, [tr](char* data, int length) {
        return rtcGetTrackMid(tr, data, length);
    });
}

jint getTrackDirection(JNIEnv* env, jclass, jint tr, jintArray direction)
{
    rtcDirection value = RTC_DIRECTION_UNKNOWN;
    jint result = rtcGetTrackDirection(tr, &value);
    if (result >= 0 && direction != nullptr && env->GetArrayLength(direction) > 0) {
        jint element = static_cast<jint>(value);
        env->SetIntArrayRegion(direction, 0, 1, &element);
    }
    return result;
}

jint requestKeyframe(JNIEnv*, jclass, jint tr)
{
    return rtcRequestKeyframe(tr);
}

jint requestBitrate(JNIEnv*, jclass, jint tr, jint bitrate)
{
    return rtcRequestBitrate(tr, static_cast<unsigned int>(bitrate));
}

jint setFrameCallback(JNIEnv*, jclass, jint tr, jboolean enabled)
{
    return rtcSetFrameCallback(tr, enabled ? frameTrampoline : nullptr);
}

jint callPacketizer(jint kind, jint tr, const rtcPacketizerInit* init)
{
    switch (kind) {
    case packetizerH264:
        return rtcSetH264Packetizer(tr, init);
    case packetizerH265:
        return rtcSetH265Packetizer(tr, init);
    case packetizerAv1:
        return rtcSetAV1Packetizer(tr, init);
    case packetizerVp8:
        return rtcSetVP8Packetizer(tr, init);
    case packetizerVp9:
        return rtcSetVP9Packetizer(tr, init);
    case packetizerOpus:
        return rtcSetOpusPacketizer(tr, init);
    case packetizerAac:
        return rtcSetAACPacketizer(tr, init);
    case packetizerPcmu:
        return rtcSetPCMUPacketizer(tr, init);
    case packetizerPcma:
        return rtcSetPCMAPacketizer(tr, init);
    case packetizerG722:
        return rtcSetG722Packetizer(tr, init);
    default:
        return RTC_ERR_INVALID;
    }
}

jint setPacketizer(JNIEnv* env, jclass, jint kind, jint tr, jintArray fields, jbyteArray cname)
{
    if (fields == nullptr) {
        return callPacketizer(kind, tr, nullptr);
    }
    if (env->GetArrayLength(fields) < numberOfPacketizerFields) {
        return RTC_ERR_INVALID;
    }
    jint values[numberOfPacketizerFields];
    env->GetIntArrayRegion(fields, 0, numberOfPacketizerFields, values);
    auto cnameString = readString(env, cname);
    rtcPacketizerInit init;
    memset(&init, 0, sizeof(init));
    init.ssrc = static_cast<uint32_t>(values[fieldSsrc]);
    init.cname = cString(cnameString);
    init.payloadType = static_cast<uint8_t>(values[fieldPayloadType]);
    init.clockRate = static_cast<uint32_t>(values[fieldClockRate]);
    init.sequenceNumber = static_cast<uint16_t>(values[fieldSequenceNumber]);
    init.timestamp = static_cast<uint32_t>(values[fieldTimestamp]);
    init.maxFragmentSize = static_cast<uint16_t>(values[fieldMaxFragmentSize]);
    init.nalSeparator = static_cast<rtcNalUnitSeparator>(values[fieldNalSeparator]);
    init.obuPacketization = static_cast<rtcObuPacketization>(values[fieldObuPacketization]);
    init.playoutDelayId = static_cast<uint8_t>(values[fieldPlayoutDelayId]);
    init.playoutDelayMin = static_cast<uint16_t>(values[fieldPlayoutDelayMin]);
    init.playoutDelayMax = static_cast<uint16_t>(values[fieldPlayoutDelayMax]);
    init.colorSpaceId = static_cast<uint8_t>(values[fieldColorSpaceId]);
    init.colorChromaSitingHorz = static_cast<uint8_t>(values[fieldColorChromaSitingHorz]);
    init.colorChromaSitingVert = static_cast<uint8_t>(values[fieldColorChromaSitingVert]);
    init.colorRange = static_cast<uint8_t>(values[fieldColorRange]);
    init.colorPrimaries = static_cast<uint8_t>(values[fieldColorPrimaries]);
    init.colorTransfer = static_cast<uint8_t>(values[fieldColorTransfer]);
    init.colorMatrix = static_cast<uint8_t>(values[fieldColorMatrix]);
    return callPacketizer(kind, tr, &init);
}

jint setH264Depacketizer(JNIEnv*, jclass, jint tr, jint nalSeparator)
{
    return rtcSetH264Depacketizer(tr, static_cast<rtcNalUnitSeparator>(nalSeparator));
}

jint setH265Depacketizer(JNIEnv*, jclass, jint tr, jint nalSeparator)
{
    return rtcSetH265Depacketizer(tr, static_cast<rtcNalUnitSeparator>(nalSeparator));
}

jint setAv1Depacketizer(JNIEnv*, jclass, jint tr, jint obuPacketization)
{
    return rtcSetAV1Depacketizer(tr, static_cast<rtcObuPacketization>(obuPacketization));
}

jint setVp8Depacketizer(JNIEnv*, jclass, jint tr)
{
    return rtcSetVP8Depacketizer(tr);
}

jint setVp9Depacketizer(JNIEnv*, jclass, jint tr)
{
    return rtcSetVP9Depacketizer(tr);
}

jint setOpusDepacketizer(JNIEnv*, jclass, jint tr)
{
    return rtcSetOpusDepacketizer(tr);
}

jint setAacDepacketizer(JNIEnv*, jclass, jint tr)
{
    return rtcSetAACDepacketizer(tr);
}

jint setPcmuDepacketizer(JNIEnv*, jclass, jint tr)
{
    return rtcSetPCMUDepacketizer(tr);
}

jint setPcmaDepacketizer(JNIEnv*, jclass, jint tr)
{
    return rtcSetPCMADepacketizer(tr);
}

jint setG722Depacketizer(JNIEnv*, jclass, jint tr)
{
    return rtcSetG722Depacketizer(tr);
}

jint chainRtcpReceivingSession(JNIEnv*, jclass, jint tr)
{
    return rtcChainRtcpReceivingSession(tr);
}

jint chainRtcpSrReporter(JNIEnv*, jclass, jint tr)
{
    return rtcChainRtcpSrReporter(tr);
}

jint chainRtcpNackResponder(JNIEnv*, jclass, jint tr, jint maxStoredPacketsCount)
{
    return rtcChainRtcpNackResponder(tr, static_cast<unsigned int>(maxStoredPacketsCount));
}

jint chainPliHandler(JNIEnv*, jclass, jint tr)
{
    return rtcChainPliHandler(tr, pliTrampoline);
}

jint chainRembHandler(JNIEnv*, jclass, jint tr)
{
    return rtcChainRembHandler(tr, rembTrampoline);
}

jint chainPacingHandler(JNIEnv*, jclass, jint tr, jdouble bitsPerSecond, jint sendIntervalMs)
{
    return rtcChainPacingHandler(tr, bitsPerSecond, sendIntervalMs);
}

uint32_t readTimestamp(JNIEnv* env, jintArray timestamp)
{
    jint element = 0;
    if (timestamp != nullptr && env->GetArrayLength(timestamp) > 0) {
        env->GetIntArrayRegion(timestamp, 0, 1, &element);
    }
    return static_cast<uint32_t>(element);
}

void writeTimestamp(JNIEnv* env, jintArray timestamp, uint32_t value)
{
    if (timestamp != nullptr && env->GetArrayLength(timestamp) > 0) {
        jint element = static_cast<jint>(value);
        env->SetIntArrayRegion(timestamp, 0, 1, &element);
    }
}

jint transformSecondsToTimestamp(JNIEnv* env, jclass, jint id, jdouble seconds, jintArray timestamp)
{
    uint32_t value = readTimestamp(env, timestamp);
    jint result = rtcTransformSecondsToTimestamp(id, seconds, &value);
    writeTimestamp(env, timestamp, value);
    return result;
}

jint transformTimestampToSeconds(JNIEnv* env, jclass, jint id, jint timestamp, jdoubleArray seconds)
{
    bool hasSeconds = seconds != nullptr && env->GetArrayLength(seconds) > 0;
    double value = 0;
    if (hasSeconds) {
        env->GetDoubleArrayRegion(seconds, 0, 1, &value);
    }
    jint result = rtcTransformTimestampToSeconds(id, static_cast<uint32_t>(timestamp), &value);
    if (hasSeconds) {
        env->SetDoubleArrayRegion(seconds, 0, 1, &value);
    }
    return result;
}

jint getCurrentTrackTimestamp(JNIEnv* env, jclass, jint id, jintArray timestamp)
{
    uint32_t value = readTimestamp(env, timestamp);
    jint result = rtcGetCurrentTrackTimestamp(id, &value);
    writeTimestamp(env, timestamp, value);
    return result;
}

jint setTrackRtpTimestamp(JNIEnv*, jclass, jint id, jint timestamp)
{
    return rtcSetTrackRtpTimestamp(id, static_cast<uint32_t>(timestamp));
}

jint getLastTrackSenderReportTimestamp(JNIEnv* env, jclass, jint id, jintArray timestamp)
{
    uint32_t value = readTimestamp(env, timestamp);
    jint result = rtcGetLastTrackSenderReportTimestamp(id, &value);
    writeTimestamp(env, timestamp, value);
    return result;
}

jint getTrackRtcpSyncTimestamps(JNIEnv* env, jclass, jint tr, jlongArray rtpTimestamp, jlongArray ntpTimestamp)
{
    bool hasRtp = rtpTimestamp != nullptr && env->GetArrayLength(rtpTimestamp) > 0;
    bool hasNtp = ntpTimestamp != nullptr && env->GetArrayLength(ntpTimestamp) > 0;
    jlong rtp = 0;
    jlong ntp = 0;
    if (hasRtp) {
        env->GetLongArrayRegion(rtpTimestamp, 0, 1, &rtp);
    }
    if (hasNtp) {
        env->GetLongArrayRegion(ntpTimestamp, 0, 1, &ntp);
    }
    auto rtpValue = static_cast<uint64_t>(rtp);
    auto ntpValue = static_cast<uint64_t>(ntp);
    jint result = rtcGetTrackRtcpSyncTimestamps(tr, &rtpValue, &ntpValue);
    rtp = static_cast<jlong>(rtpValue);
    ntp = static_cast<jlong>(ntpValue);
    if (hasRtp) {
        env->SetLongArrayRegion(rtpTimestamp, 0, 1, &rtp);
    }
    if (hasNtp) {
        env->SetLongArrayRegion(ntpTimestamp, 0, 1, &ntp);
    }
    return result;
}

jint setThreadPoolSize(JNIEnv*, jclass, jint count)
{
    return rtcSetThreadPoolSize(static_cast<unsigned int>(count));
}

jboolean preload(JNIEnv*, jclass)
{
    return static_cast<jboolean>(rtcPreload());
}

void cleanup(JNIEnv*, jclass)
{
    rtcCleanup();
}

const JNINativeMethod nativeMethods[] = {
    {"rtcInitLogger", "(IZ)V", reinterpret_cast<void*>(initLogger)},
    {"rtcSetUserPointer", "(IJ)V", reinterpret_cast<void*>(setUserPointer)},
    {"rtcGetUserPointer", "(I)J", reinterpret_cast<void*>(getUserPointer)},
    {"rtcCreatePeerConnection", "([[BI[B[B[B[B[B[I)I", reinterpret_cast<void*>(createPeerConnection)},
    {"rtcClosePeerConnection", "(I)I", reinterpret_cast<void*>(closePeerConnection)},
    {"rtcDeletePeerConnection", "(I)I", reinterpret_cast<void*>(deletePeerConnection)},
    {"rtcSetStateChangeCallback", "(IZ)I", reinterpret_cast<void*>(setStateChangeCallback)},
    {"rtcSetGatheringStateChangeCallback", "(IZ)I", reinterpret_cast<void*>(setGatheringStateChangeCallback)},
    {"rtcSetLocalDescription", "(I[B)I", reinterpret_cast<void*>(setLocalDescription)},
    {"rtcSetRemoteDescription", "(I[B[B)I", reinterpret_cast<void*>(setRemoteDescription)},
    {"rtcAddRemoteCandidate", "(I[B[B)I", reinterpret_cast<void*>(addRemoteCandidate)},
    {"rtcGetLocalDescription", "(I[BI)I", reinterpret_cast<void*>(getLocalDescription)},
    {"rtcGetRemoteDescription", "(I[BI)I", reinterpret_cast<void*>(getRemoteDescription)},
    {"rtcGetLocalDescriptionType", "(I[BI)I", reinterpret_cast<void*>(getLocalDescriptionType)},
    {"rtcGetRemoteDescriptionType", "(I[BI)I", reinterpret_cast<void*>(getRemoteDescriptionType)},
    {"rtcGetLocalAddress", "(I[BI)I", reinterpret_cast<void*>(getLocalAddress)},
    {"rtcGetRemoteAddress", "(I[BI)I", reinterpret_cast<void*>(getRemoteAddress)},
    {"rtcGetSelectedCandidatePair", "(I[BI[BI)I", reinterpret_cast<void*>(getSelectedCandidatePair)},
    {"rtcSetOpenCallback", "(IZ)I", reinterpret_cast<void*>(setOpenCallback)},
    {"rtcSetClosedCallback", "(IZ)I", reinterpret_cast<void*>(setClosedCallback)},
    {"rtcSetErrorCallback", "(IZ)I", reinterpret_cast<void*>(setErrorCallback)},
    {"rtcSendMessage", "(I[BI)I", reinterpret_cast<void*>(sendMessage)},
    {"rtcClose", "(I)I", reinterpret_cast<void*>(closeId)},
    {"rtcDelete", "(I)I", reinterpret_cast<void*>(deleteId)},
    {"rtcIsOpen", "(I)Z", reinterpret_cast<void*>(isOpen)},
    {"rtcIsClosed", "(I)Z", reinterpret_cast<void*>(isClosed)},
    {"rtcSetTrackCallback", "(IZ)I", reinterpret_cast<void*>(setTrackCallback)},
    {"rtcAddTrack", "(I[B)I", reinterpret_cast<void*>(addTrack)},
    {"rtcAddTrackEx", "(IIIII[B[B[B[B[B)I", reinterpret_cast<void*>(addTrackEx)},
    {"rtcDeleteTrack", "(I)I", reinterpret_cast<void*>(deleteTrack)},
    {"rtcGetTrackDescription", "(I[BI)I", reinterpret_cast<void*>(getTrackDescription)},
    {"rtcGetTrackMid", "(I[BI)I", reinterpret_cast<void*>(getTrackMid)},
    {"rtcGetTrackDirection", "(I[I)I", reinterpret_cast<void*>(getTrackDirection)},
    {"rtcRequestKeyframe", "(I)I", reinterpret_cast<void*>(requestKeyframe)},
    {"rtcRequestBitrate", "(II)I", reinterpret_cast<void*>(requestBitrate)},
    {"rtcSetFrameCallback", "(IZ)I", reinterpret_cast<void*>(setFrameCallback)},
    {"rtcSetPacketizer", "(II[I[B)I", reinterpret_cast<void*>(setPacketizer)},
    {"rtcSetH264Depacketizer", "(II)I", reinterpret_cast<void*>(setH264Depacketizer)},
    {"rtcSetH265Depacketizer", "(II)I", reinterpret_cast<void*>(setH265Depacketizer)},
    {"rtcSetAV1Depacketizer", "(II)I", reinterpret_cast<void*>(setAv1Depacketizer)},
    {"rtcSetVP8Depacketizer", "(I)I", reinterpret_cast<void*>(setVp8Depacketizer)},
    {"rtcSetVP9Depacketizer", "(I)I", reinterpret_cast<void*>(setVp9Depacketizer)},
    {"rtcSetOpusDepacketizer", "(I)I", reinterpret_cast<void*>(setOpusDepacketizer)},
    {"rtcSetAACDepacketizer", "(I)I", reinterpret_cast<void*>(setAacDepacketizer)},
    {"rtcSetPCMUDepacketizer", "(I)I", reinterpret_cast<void*>(setPcmuDepacketizer)},
    {"rtcSetPCMADepacketizer", "(I)I", reinterpret_cast<void*>(setPcmaDepacketizer)},
    {"rtcSetG722Depacketizer", "(I)I", reinterpret_cast<void*>(setG722Depacketizer)},
    {"rtcChainRtcpReceivingSession", "(I)I", reinterpret_cast<void*>(chainRtcpReceivingSession)},
    {"rtcChainRtcpSrReporter", "(I)I", reinterpret_cast<void*>(chainRtcpSrReporter)},
    {"rtcChainRtcpNackResponder", "(II)I", reinterpret_cast<void*>(chainRtcpNackResponder)},
    {"rtcChainPliHandler", "(I)I", reinterpret_cast<void*>(chainPliHandler)},
    {"rtcChainRembHandler", "(I)I", reinterpret_cast<void*>(chainRembHandler)},
    {"rtcChainPacingHandler", "(IDI)I", reinterpret_cast<void*>(chainPacingHandler)},
    {"rtcTransformSecondsToTimestamp", "(ID[I)I", reinterpret_cast<void*>(transformSecondsToTimestamp)},
    {"rtcTransformTimestampToSeconds", "(II[D)I", reinterpret_cast<void*>(transformTimestampToSeconds)},
    {"rtcGetCurrentTrackTimestamp", "(I[I)I", reinterpret_cast<void*>(getCurrentTrackTimestamp)},
    {"rtcSetTrackRtpTimestamp", "(II)I", reinterpret_cast<void*>(setTrackRtpTimestamp)},
    {"rtcGetLastTrackSenderReportTimestamp", "(I[I)I", reinterpret_cast<void*>(getLastTrackSenderReportTimestamp)},
    {"rtcGetTrackRtcpSyncTimestamps", "(I[J[J)I", reinterpret_cast<void*>(getTrackRtcpSyncTimestamps)},
    {"rtcSetThreadPoolSize", "(I)I", reinterpret_cast<void*>(setThreadPoolSize)},
    {"rtcPreload", "()Z", reinterpret_cast<void*>(preload)},
    {"rtcCleanup", "()V", reinterpret_cast<void*>(cleanup)},
};

bool cacheMethod(JNIEnv* env, jmethodID* method, const char* name, const char* signature)
{
    *method = env->GetStaticMethodID(nativeClass, name, signature);
    if (*method == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Method %s%s of %s not found", name, signature, nativeClassName);
        return false;
    }
    return true;
}

bool cacheMethods(JNIEnv* env)
{
    bool ok = true;
    ok = cacheMethod(env, &onLogMethod, "onLog", "(I[B)V") && ok;
    ok = cacheMethod(env, &onStateChangeMethod, "onStateChange", "(IIJ)V") && ok;
    ok = cacheMethod(env, &onGatheringStateChangeMethod, "onGatheringStateChange", "(IIJ)V") && ok;
    ok = cacheMethod(env, &onTrackMethod, "onTrack", "(IIJ)V") && ok;
    ok = cacheMethod(env, &onOpenMethod, "onOpen", "(IJ)V") && ok;
    ok = cacheMethod(env, &onClosedMethod, "onClosed", "(IJ)V") && ok;
    ok = cacheMethod(env, &onErrorMethod, "onError", "(I[BJ)V") && ok;
    ok = cacheMethod(env, &onFrameMethod, "onFrame", "(I[BIZIIDJ)V") && ok;
    ok = cacheMethod(env, &onPliMethod, "onPli", "(IJ)V") && ok;
    ok = cacheMethod(env, &onRembMethod, "onRemb", "(IIJ)V") && ok;
    return ok;
}

}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*)
{
    javaVm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    if (pthread_key_create(&detachKey, detachCurrentThread) != 0) {
        return JNI_ERR;
    }
    jclass localClass = env->FindClass(nativeClassName);
    if (localClass == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Class %s not found", nativeClassName);
        return JNI_ERR;
    }
    nativeClass = static_cast<jclass>(env->NewGlobalRef(localClass));
    env->DeleteLocalRef(localClass);
    if (!cacheMethods(env)) {
        return JNI_ERR;
    }
    jint count = static_cast<jint>(sizeof(nativeMethods) / sizeof(nativeMethods[0]));
    if (env->RegisterNatives(nativeClass, nativeMethods, count) != JNI_OK) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Failed to register natives of %s", nativeClassName);
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}
