#include <android/log.h>
#include <android/multinetwork.h>
#include <jni.h>
#include <pthread.h>
#include <sys/prctl.h>
#include <sys/socket.h>

#include <cerrno>
#include <cstdint>
#include <cstring>
#include <memory>

#include "librist/librist.h"

static_assert(RIST_PROFILE_SIMPLE == 0, "RIST_PROFILE_SIMPLE");
static_assert(RIST_PROFILE_MAIN == 1, "RIST_PROFILE_MAIN");
static_assert(RIST_PROFILE_ADVANCED == 2, "RIST_PROFILE_ADVANCED");
static_assert(RIST_CONNECTION_ESTABLISHED == 0, "RIST_CONNECTION_ESTABLISHED");
static_assert(RIST_CONNECTION_TIMED_OUT == 1, "RIST_CONNECTION_TIMED_OUT");
static_assert(RIST_CLIENT_CONNECTED == 2, "RIST_CLIENT_CONNECTED");
static_assert(RIST_CLIENT_TIMED_OUT == 3, "RIST_CLIENT_TIMED_OUT");

namespace {

const char* const logTag = "MoblinRist";
const char* const ristNativeClassName = "com/moblin/android/platform/rist/RistNative";
const char* const senderCallbacksClassName = "com/moblin/android/platform/rist/RistSenderCallbacks";
const char* const receiverCallbacksClassName = "com/moblin/android/platform/rist/RistReceiverCallbacks";

JavaVM* javaVm = nullptr;
pthread_key_t detachKey;
jmethodID senderOnStatsMethod = nullptr;
jmethodID senderOnConnectionStatusMethod = nullptr;
jmethodID receiverOnConnectionStatusMethod = nullptr;
jmethodID receiverOnDataMethod = nullptr;
thread_local net_handle_t createPeerNetwork = 0;

struct Sender {
    rist_ctx* ctx = nullptr;
    jobject callbacks = nullptr;
};

struct Receiver {
    rist_ctx* ctx = nullptr;
    rist_peer* peer = nullptr;
    jobject callbacks = nullptr;
};

class NativeBuffer {
public:
    explicit NativeBuffer(size_t size)
        : heap(size > sizeof(stack) ? new char[size] : nullptr)
    {
    }

    char* data()
    {
        return heap ? heap.get() : stack;
    }

private:
    char stack[2048];
    std::unique_ptr<char[]> heap;
};

template <typename T>
T* fromHandle(jlong handle)
{
    return reinterpret_cast<T*>(static_cast<intptr_t>(handle));
}

template <typename T>
jlong toHandle(T* pointer)
{
    return static_cast<jlong>(reinterpret_cast<intptr_t>(pointer));
}

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

int senderStatsCallback(void* arg, const rist_stats* stats)
{
    auto* sender = static_cast<Sender*>(arg);
    if (sender == nullptr || stats == nullptr || stats->stats_type != RIST_STATS_SENDER_PEER) {
        return 0;
    }
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr) {
        return 0;
    }
    const rist_stats_sender_peer& peer = stats->stats.sender_peer;
    env->CallVoidMethod(sender->callbacks,
                        senderOnStatsMethod,
                        static_cast<jint>(peer.peer_id),
                        static_cast<jlong>(peer.bandwidth),
                        static_cast<jlong>(peer.retry_bandwidth),
                        static_cast<jlong>(peer.sent),
                        static_cast<jlong>(peer.received),
                        static_cast<jlong>(peer.retransmitted),
                        static_cast<jdouble>(peer.quality),
                        static_cast<jint>(peer.rtt));
    clearPendingException(env);
    return 0;
}

void senderConnectionStatusCallback(void* arg, rist_peer* peer, rist_connection_status status)
{
    auto* sender = static_cast<Sender*>(arg);
    if (sender == nullptr || peer == nullptr) {
        return;
    }
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr) {
        return;
    }
    env->CallVoidMethod(sender->callbacks,
                        senderOnConnectionStatusMethod,
                        static_cast<jint>(rist_peer_get_id(peer)),
                        static_cast<jint>(status));
    clearPendingException(env);
}

void receiverConnectionStatusCallback(void* arg, rist_peer* peer, rist_connection_status status)
{
    auto* receiver = static_cast<Receiver*>(arg);
    if (receiver == nullptr) {
        return;
    }
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr) {
        return;
    }
    env->CallVoidMethod(receiver->callbacks,
                        receiverOnConnectionStatusMethod,
                        toHandle(peer),
                        static_cast<jint>(status));
    clearPendingException(env);
}

int receiverDataCallback(void* arg, rist_data_block* block)
{
    auto* receiver = static_cast<Receiver*>(arg);
    if (receiver == nullptr || block == nullptr) {
        rist_receiver_data_block_free2(&block);
        return -1;
    }
    JNIEnv* env = getAttachedEnv();
    if (env != nullptr && (block->payload != nullptr || block->payload_len == 0) &&
        block->payload_len <= INT32_MAX) {
        auto length = static_cast<jsize>(block->payload_len);
        jbyteArray payload = env->NewByteArray(length);
        if (payload != nullptr) {
            if (length > 0) {
                env->SetByteArrayRegion(payload, 0, length, static_cast<const jbyte*>(block->payload));
            }
            env->CallVoidMethod(receiver->callbacks,
                                receiverOnDataMethod,
                                static_cast<jint>(block->virt_dst_port),
                                toHandle(block->peer),
                                payload);
            env->DeleteLocalRef(payload);
        }
        clearPendingException(env);
    }
    rist_receiver_data_block_free2(&block);
    return 0;
}

bool bindPeerToNetwork(rist_peer* peer, jlong network)
{
    int socket = -1;
    int socketExtra = -1;
    int result = rist_peer_get_socket(peer, &socket, &socketExtra);
    if (result < 0 || socket < 0) {
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Peer %u has no socket", rist_peer_get_id(peer));
        return false;
    }
    auto handle = static_cast<net_handle_t>(network);
    if (android_setsocknetwork(handle, socket) != 0) {
        __android_log_print(ANDROID_LOG_ERROR,
                            logTag,
                            "Failed to bind peer %u to network %llu: %s",
                            rist_peer_get_id(peer),
                            static_cast<unsigned long long>(handle),
                            strerror(errno));
        return false;
    }
    if (result == 1 && socketExtra >= 0 && android_setsocknetwork(handle, socketExtra) != 0) {
        __android_log_print(ANDROID_LOG_ERROR,
                            logTag,
                            "Failed to bind the RTCP socket of peer %u to network %llu: %s",
                            rist_peer_get_id(peer),
                            static_cast<unsigned long long>(handle),
                            strerror(errno));
        return false;
    }
    __android_log_print(ANDROID_LOG_INFO,
                        logTag,
                        "Bound peer %u to network %llu",
                        rist_peer_get_id(peer),
                        static_cast<unsigned long long>(handle));
    return true;
}

rist_peer_config* parseAddress(JNIEnv* env, jstring url)
{
    if (url == nullptr) {
        return nullptr;
    }
    const char* chars = env->GetStringUTFChars(url, nullptr);
    if (chars == nullptr) {
        clearPendingException(env);
        return nullptr;
    }
    rist_peer_config* config = nullptr;
    int result = rist_parse_address2(chars, &config);
    env->ReleaseStringUTFChars(url, chars);
    if (result != 0) {
        if (config != nullptr) {
            rist_peer_config_free2(&config);
        }
        return nullptr;
    }
    return config;
}

jstring libristVersion(JNIEnv* env, jclass)
{
    const char* version = librist_version();
    return env->NewStringUTF(version != nullptr ? version : "");
}

jlong senderCreate(JNIEnv* env, jclass, jint profile, jobject callbacks)
{
    if (callbacks == nullptr) {
        return 0;
    }
    rist_ctx* ctx = nullptr;
    if (rist_sender_create(&ctx, static_cast<rist_profile>(profile), 0, nullptr) != 0 || ctx == nullptr) {
        return 0;
    }
    auto* sender = new Sender();
    sender->ctx = ctx;
    sender->callbacks = env->NewGlobalRef(callbacks);
    rist_stats_callback_set(ctx, 200, senderStatsCallback, sender);
    rist_connection_status_callback_set(ctx, senderConnectionStatusCallback, sender);
    return toHandle(sender);
}

jint senderStart(JNIEnv*, jclass, jlong handle)
{
    auto* sender = fromHandle<Sender>(handle);
    if (sender == nullptr) {
        return -1;
    }
    return rist_start(sender->ctx);
}

void senderDestroy(JNIEnv* env, jclass, jlong handle)
{
    auto* sender = fromHandle<Sender>(handle);
    if (sender == nullptr) {
        return;
    }
    rist_destroy(sender->ctx);
    env->DeleteGlobalRef(sender->callbacks);
    delete sender;
}

jlong senderAddPeer(JNIEnv* env, jclass, jlong handle, jstring url, jlong network)
{
    auto* sender = fromHandle<Sender>(handle);
    if (sender == nullptr) {
        return 0;
    }
    rist_peer_config* config = parseAddress(env, url);
    if (config == nullptr) {
        return 0;
    }
    rist_peer* peer = nullptr;
    createPeerNetwork = static_cast<net_handle_t>(network);
    int result = rist_peer_create(sender->ctx, &peer, config);
    createPeerNetwork = 0;
    if (peer != nullptr && config->srp_username[0] != 0 && config->srp_password[0] != 0) {
        rist_enable_eap_srp_2(peer, config->srp_username, config->srp_password, nullptr, nullptr);
    }
    rist_peer_config_free2(&config);
    if (result != 0 || peer == nullptr) {
        return 0;
    }
    if (network != 0 && !bindPeerToNetwork(peer, network)) {
        rist_peer_destroy(sender->ctx, peer);
        return 0;
    }
    return toHandle(peer);
}

jint senderRemovePeer(JNIEnv*, jclass, jlong handle, jlong peer)
{
    auto* sender = fromHandle<Sender>(handle);
    if (sender == nullptr || peer == 0) {
        return -1;
    }
    return rist_peer_destroy(sender->ctx, fromHandle<rist_peer>(peer));
}

jint senderSetPeerWeight(JNIEnv*, jclass, jlong handle, jlong peer, jint weight)
{
    auto* sender = fromHandle<Sender>(handle);
    if (sender == nullptr || peer == 0) {
        return -1;
    }
    return rist_peer_weight_set(sender->ctx, fromHandle<rist_peer>(peer), static_cast<uint32_t>(weight));
}

jint senderWrite(JNIEnv* env, jclass, jlong handle, jbyteArray data, jint count)
{
    auto* sender = fromHandle<Sender>(handle);
    if (sender == nullptr || data == nullptr || count <= 0 || count > env->GetArrayLength(data)) {
        return -1;
    }
    NativeBuffer buffer(static_cast<size_t>(count));
    env->GetByteArrayRegion(data, 0, count, reinterpret_cast<jbyte*>(buffer.data()));
    rist_data_block block;
    memset(&block, 0, sizeof(block));
    block.payload = buffer.data();
    block.payload_len = static_cast<size_t>(count);
    return rist_sender_data_write(sender->ctx, &block);
}

jint peerGetId(JNIEnv*, jclass, jlong peer)
{
    return static_cast<jint>(rist_peer_get_id(fromHandle<rist_peer>(peer)));
}

jlong receiverCreate(JNIEnv* env, jclass, jint profile, jstring url, jobject callbacks)
{
    if (callbacks == nullptr) {
        return 0;
    }
    rist_ctx* ctx = nullptr;
    if (rist_receiver_create(&ctx, static_cast<rist_profile>(profile), nullptr) != 0 || ctx == nullptr) {
        return 0;
    }
    rist_peer_config* config = parseAddress(env, url);
    if (config == nullptr) {
        rist_destroy(ctx);
        return 0;
    }
    rist_peer* peer = nullptr;
    int result = rist_peer_create(ctx, &peer, config);
    rist_peer_config_free2(&config);
    if (result != 0 || peer == nullptr) {
        rist_destroy(ctx);
        return 0;
    }
    auto* receiver = new Receiver();
    receiver->ctx = ctx;
    receiver->peer = peer;
    receiver->callbacks = env->NewGlobalRef(callbacks);
    rist_connection_status_callback_set(ctx, receiverConnectionStatusCallback, receiver);
    rist_receiver_data_callback_set2(ctx, receiverDataCallback, receiver);
    return toHandle(receiver);
}

jint receiverStart(JNIEnv*, jclass, jlong handle)
{
    auto* receiver = fromHandle<Receiver>(handle);
    if (receiver == nullptr) {
        return -1;
    }
    return rist_start(receiver->ctx);
}

void receiverDestroy(JNIEnv* env, jclass, jlong handle)
{
    auto* receiver = fromHandle<Receiver>(handle);
    if (receiver == nullptr) {
        return;
    }
    rist_peer_destroy(receiver->ctx, receiver->peer);
    rist_destroy(receiver->ctx);
    env->DeleteGlobalRef(receiver->callbacks);
    delete receiver;
}

jmethodID getMethod(JNIEnv* env, const char* className, const char* name, const char* signature)
{
    jclass callbacksClass = env->FindClass(className);
    if (callbacksClass == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Class %s not found", className);
        return nullptr;
    }
    jmethodID method = env->GetMethodID(callbacksClass, name, signature);
    env->DeleteLocalRef(callbacksClass);
    if (method == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Method %s of %s not found", name, className);
    }
    return method;
}

const JNINativeMethod nativeMethods[] = {
    {"librist_version", "()Ljava/lang/String;", reinterpret_cast<void*>(libristVersion)},
    {"senderCreate", "(ILcom/moblin/android/platform/rist/RistSenderCallbacks;)J", reinterpret_cast<void*>(senderCreate)},
    {"senderStart", "(J)I", reinterpret_cast<void*>(senderStart)},
    {"senderDestroy", "(J)V", reinterpret_cast<void*>(senderDestroy)},
    {"senderAddPeer", "(JLjava/lang/String;J)J", reinterpret_cast<void*>(senderAddPeer)},
    {"senderRemovePeer", "(JJ)I", reinterpret_cast<void*>(senderRemovePeer)},
    {"senderSetPeerWeight", "(JJI)I", reinterpret_cast<void*>(senderSetPeerWeight)},
    {"senderWrite", "(J[BI)I", reinterpret_cast<void*>(senderWrite)},
    {"peerGetId", "(J)I", reinterpret_cast<void*>(peerGetId)},
    {"receiverCreate",
     "(ILjava/lang/String;Lcom/moblin/android/platform/rist/RistReceiverCallbacks;)J",
     reinterpret_cast<void*>(receiverCreate)},
    {"receiverStart", "(J)I", reinterpret_cast<void*>(receiverStart)},
    {"receiverDestroy", "(J)V", reinterpret_cast<void*>(receiverDestroy)},
};

}

extern "C" int __real_setsockopt(int fd, int level, int option, const void* value, socklen_t length);

extern "C" int __wrap_setsockopt(int fd, int level, int option, const void* value, socklen_t length)
{
    if (level == SOL_SOCKET && option == SO_BINDTODEVICE && createPeerNetwork != 0) {
        return android_setsocknetwork(createPeerNetwork, fd);
    }
    return __real_setsockopt(fd, level, option, value, length);
}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void*)
{
    javaVm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    if (pthread_key_create(&detachKey, detachCurrentThread) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Failed to create the thread detach key");
        return JNI_ERR;
    }
    senderOnStatsMethod = getMethod(env, senderCallbacksClassName, "onStats", "(IJJJJJDI)V");
    senderOnConnectionStatusMethod = getMethod(env, senderCallbacksClassName, "onConnectionStatus", "(II)V");
    receiverOnConnectionStatusMethod = getMethod(env, receiverCallbacksClassName, "onConnectionStatus", "(JI)V");
    receiverOnDataMethod = getMethod(env, receiverCallbacksClassName, "onData", "(IJ[B)V");
    if (senderOnStatsMethod == nullptr || senderOnConnectionStatusMethod == nullptr ||
        receiverOnConnectionStatusMethod == nullptr || receiverOnDataMethod == nullptr) {
        return JNI_ERR;
    }
    jclass nativeClass = env->FindClass(ristNativeClassName);
    if (nativeClass == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Class %s not found", ristNativeClassName);
        return JNI_ERR;
    }
    auto count = static_cast<jint>(sizeof(nativeMethods) / sizeof(nativeMethods[0]));
    jint result = env->RegisterNatives(nativeClass, nativeMethods, count);
    env->DeleteLocalRef(nativeClass);
    if (result != JNI_OK) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Failed to register the natives of %s", ristNativeClassName);
        return JNI_ERR;
    }
    __android_log_print(ANDROID_LOG_INFO, logTag, "librist %s", librist_version());
    return JNI_VERSION_1_6;
}
