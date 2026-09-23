#include <android/log.h>
#include <jni.h>
#include <pthread.h>
#include <sys/prctl.h>

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <memory>
#include <mutex>
#include <unordered_map>

#include "srt.h"

static_assert(SRT_INVALID_SOCK == -1, "SRT_INVALID_SOCK");
static_assert(SRT_ERROR == -1, "SRT_ERROR");
static_assert(SRT_LIVE_DEF_PLSIZE == 1316, "SRT_LIVE_DEF_PLSIZE");
static_assert(SRT_LIVE_MAX_PLSIZE == 1456, "SRT_LIVE_MAX_PLSIZE");
static_assert(SRT_LIVE_DEF_LATENCY_MS == 120, "SRT_LIVE_DEF_LATENCY_MS");
static_assert(AF_INET == 2, "AF_INET");
static_assert(sizeof(sockaddr_in) == 16, "sockaddr_in");
static_assert(SRTS_INIT == 1, "SRTS_INIT");
static_assert(SRTS_OPENED == 2, "SRTS_OPENED");
static_assert(SRTS_LISTENING == 3, "SRTS_LISTENING");
static_assert(SRTS_CONNECTING == 4, "SRTS_CONNECTING");
static_assert(SRTS_CONNECTED == 5, "SRTS_CONNECTED");
static_assert(SRTS_BROKEN == 6, "SRTS_BROKEN");
static_assert(SRTS_CLOSING == 7, "SRTS_CLOSING");
static_assert(SRTS_CLOSED == 8, "SRTS_CLOSED");
static_assert(SRTS_NONEXIST == 9, "SRTS_NONEXIST");
static_assert(SRTO_MSS == 0, "SRTO_MSS");
static_assert(SRTO_SNDSYN == 1, "SRTO_SNDSYN");
static_assert(SRTO_RCVSYN == 2, "SRTO_RCVSYN");
static_assert(SRTO_ISN == 3, "SRTO_ISN");
static_assert(SRTO_FC == 4, "SRTO_FC");
static_assert(SRTO_SNDBUF == 5, "SRTO_SNDBUF");
static_assert(SRTO_RCVBUF == 6, "SRTO_RCVBUF");
static_assert(SRTO_LINGER == 7, "SRTO_LINGER");
static_assert(SRTO_UDP_SNDBUF == 8, "SRTO_UDP_SNDBUF");
static_assert(SRTO_UDP_RCVBUF == 9, "SRTO_UDP_RCVBUF");
static_assert(SRTO_RENDEZVOUS == 12, "SRTO_RENDEZVOUS");
static_assert(SRTO_SNDTIMEO == 13, "SRTO_SNDTIMEO");
static_assert(SRTO_RCVTIMEO == 14, "SRTO_RCVTIMEO");
static_assert(SRTO_REUSEADDR == 15, "SRTO_REUSEADDR");
static_assert(SRTO_MAXBW == 16, "SRTO_MAXBW");
static_assert(SRTO_STATE == 17, "SRTO_STATE");
static_assert(SRTO_EVENT == 18, "SRTO_EVENT");
static_assert(SRTO_SNDDATA == 19, "SRTO_SNDDATA");
static_assert(SRTO_RCVDATA == 20, "SRTO_RCVDATA");
static_assert(SRTO_SENDER == 21, "SRTO_SENDER");
static_assert(SRTO_TSBPDMODE == 22, "SRTO_TSBPDMODE");
static_assert(SRTO_LATENCY == 23, "SRTO_LATENCY");
static_assert(SRTO_INPUTBW == 24, "SRTO_INPUTBW");
static_assert(SRTO_OHEADBW == 25, "SRTO_OHEADBW");
static_assert(SRTO_PASSPHRASE == 26, "SRTO_PASSPHRASE");
static_assert(SRTO_PBKEYLEN == 27, "SRTO_PBKEYLEN");
static_assert(SRTO_KMSTATE == 28, "SRTO_KMSTATE");
static_assert(SRTO_IPTTL == 29, "SRTO_IPTTL");
static_assert(SRTO_IPTOS == 30, "SRTO_IPTOS");
static_assert(SRTO_TLPKTDROP == 31, "SRTO_TLPKTDROP");
static_assert(SRTO_SNDDROPDELAY == 32, "SRTO_SNDDROPDELAY");
static_assert(SRTO_NAKREPORT == 33, "SRTO_NAKREPORT");
static_assert(SRTO_VERSION == 34, "SRTO_VERSION");
static_assert(SRTO_PEERVERSION == 35, "SRTO_PEERVERSION");
static_assert(SRTO_CONNTIMEO == 36, "SRTO_CONNTIMEO");
static_assert(SRTO_DRIFTTRACER == 37, "SRTO_DRIFTTRACER");
static_assert(SRTO_MININPUTBW == 38, "SRTO_MININPUTBW");
static_assert(SRTO_SNDKMSTATE == 40, "SRTO_SNDKMSTATE");
static_assert(SRTO_RCVKMSTATE == 41, "SRTO_RCVKMSTATE");
static_assert(SRTO_LOSSMAXTTL == 42, "SRTO_LOSSMAXTTL");
static_assert(SRTO_RCVLATENCY == 43, "SRTO_RCVLATENCY");
static_assert(SRTO_PEERLATENCY == 44, "SRTO_PEERLATENCY");
static_assert(SRTO_MINVERSION == 45, "SRTO_MINVERSION");
static_assert(SRTO_STREAMID == 46, "SRTO_STREAMID");
static_assert(SRTO_CONGESTION == 47, "SRTO_CONGESTION");
static_assert(SRTO_MESSAGEAPI == 48, "SRTO_MESSAGEAPI");
static_assert(SRTO_PAYLOADSIZE == 49, "SRTO_PAYLOADSIZE");
static_assert(SRTO_TRANSTYPE == 50, "SRTO_TRANSTYPE");
static_assert(SRTO_KMREFRESHRATE == 51, "SRTO_KMREFRESHRATE");
static_assert(SRTO_KMPREANNOUNCE == 52, "SRTO_KMPREANNOUNCE");
static_assert(SRTO_ENFORCEDENCRYPTION == 53, "SRTO_ENFORCEDENCRYPTION");
static_assert(SRTO_IPV6ONLY == 54, "SRTO_IPV6ONLY");
static_assert(SRTO_PEERIDLETIMEO == 55, "SRTO_PEERIDLETIMEO");
static_assert(SRTO_BINDTODEVICE == 56, "SRTO_BINDTODEVICE");
static_assert(SRTO_GROUPCONNECT == 57, "SRTO_GROUPCONNECT");
static_assert(SRTO_GROUPMINSTABLETIMEO == 58, "SRTO_GROUPMINSTABLETIMEO");
static_assert(SRTO_GROUPTYPE == 59, "SRTO_GROUPTYPE");
static_assert(SRTO_PACKETFILTER == 60, "SRTO_PACKETFILTER");
static_assert(SRTO_RETRANSMITALGO == 61, "SRTO_RETRANSMITALGO");
static_assert(SRTO_MAXREXMITBW == 63, "SRTO_MAXREXMITBW");
static_assert(SRTO_SRTLAPATCHES == 120, "SRTO_SRTLAPATCHES");
static_assert(SRTT_LIVE == 0, "SRTT_LIVE");
static_assert(SRTT_FILE == 1, "SRTT_FILE");
static_assert(SRTT_INVALID == 2, "SRTT_INVALID");
static_assert(SRT_EUNKNOWN == -1, "SRT_EUNKNOWN");
static_assert(SRT_SUCCESS == 0, "SRT_SUCCESS");
static_assert(SRT_ECONNSETUP == 1000, "SRT_ECONNSETUP");
static_assert(SRT_ENOSERVER == 1001, "SRT_ENOSERVER");
static_assert(SRT_ECONNREJ == 1002, "SRT_ECONNREJ");
static_assert(SRT_ESOCKFAIL == 1003, "SRT_ESOCKFAIL");
static_assert(SRT_ESECFAIL == 1004, "SRT_ESECFAIL");
static_assert(SRT_ESCLOSED == 1005, "SRT_ESCLOSED");
static_assert(SRT_ECONNFAIL == 2000, "SRT_ECONNFAIL");
static_assert(SRT_ECONNLOST == 2001, "SRT_ECONNLOST");
static_assert(SRT_ENOCONN == 2002, "SRT_ENOCONN");
static_assert(SRT_ERESOURCE == 3000, "SRT_ERESOURCE");
static_assert(SRT_ETHREAD == 3001, "SRT_ETHREAD");
static_assert(SRT_ENOBUF == 3002, "SRT_ENOBUF");
static_assert(SRT_ESYSOBJ == 3003, "SRT_ESYSOBJ");
static_assert(SRT_EFILE == 4000, "SRT_EFILE");
static_assert(SRT_EINVRDOFF == 4001, "SRT_EINVRDOFF");
static_assert(SRT_ERDPERM == 4002, "SRT_ERDPERM");
static_assert(SRT_EINVWROFF == 4003, "SRT_EINVWROFF");
static_assert(SRT_EWRPERM == 4004, "SRT_EWRPERM");
static_assert(SRT_EINVOP == 5000, "SRT_EINVOP");
static_assert(SRT_EBOUNDSOCK == 5001, "SRT_EBOUNDSOCK");
static_assert(SRT_ECONNSOCK == 5002, "SRT_ECONNSOCK");
static_assert(SRT_EINVPARAM == 5003, "SRT_EINVPARAM");
static_assert(SRT_EINVSOCK == 5004, "SRT_EINVSOCK");
static_assert(SRT_EUNBOUNDSOCK == 5005, "SRT_EUNBOUNDSOCK");
static_assert(SRT_ENOLISTEN == 5006, "SRT_ENOLISTEN");
static_assert(SRT_ERDVNOSERV == 5007, "SRT_ERDVNOSERV");
static_assert(SRT_ERDVUNBOUND == 5008, "SRT_ERDVUNBOUND");
static_assert(SRT_EINVALMSGAPI == 5009, "SRT_EINVALMSGAPI");
static_assert(SRT_EINVALBUFFERAPI == 5010, "SRT_EINVALBUFFERAPI");
static_assert(SRT_EDUPLISTEN == 5011, "SRT_EDUPLISTEN");
static_assert(SRT_ELARGEMSG == 5012, "SRT_ELARGEMSG");
static_assert(SRT_EINVPOLLID == 5013, "SRT_EINVPOLLID");
static_assert(SRT_EPOLLEMPTY == 5014, "SRT_EPOLLEMPTY");
static_assert(SRT_EBINDCONFLICT == 5015, "SRT_EBINDCONFLICT");
static_assert(SRT_EASYNCFAIL == 6000, "SRT_EASYNCFAIL");
static_assert(SRT_EASYNCSND == 6001, "SRT_EASYNCSND");
static_assert(SRT_EASYNCRCV == 6002, "SRT_EASYNCRCV");
static_assert(SRT_ETIMEOUT == 6003, "SRT_ETIMEOUT");
static_assert(SRT_ECONGEST == 6004, "SRT_ECONGEST");
static_assert(SRT_EPEERERR == 7000, "SRT_EPEERERR");

namespace {

const char* const logTag = "MoblinSrt";
const char* const srtNativeClassName = "com/moblin/android/platform/srt/SrtNative";
const char* const sendHookClassName = "com/moblin/android/platform/srt/SrtSendHook";
const char* const perfMonClassName = "com/moblin/android/media/haishinkit/srt/CBytePerfMon";

JavaVM* javaVm = nullptr;
pthread_key_t detachKey;
jclass sendHookClass = nullptr;
jmethodID onSendMethod = nullptr;

struct PerfMonFields {
    jclass perfMonClass = nullptr;
    jfieldID pktRetransTotal = nullptr;
    jfieldID pktRecvNAKTotal = nullptr;
    jfieldID pktSndDropTotal = nullptr;
    jfieldID pktFlightSize = nullptr;
    jfieldID msRTT = nullptr;
    jfieldID pktSndBuf = nullptr;
    jfieldID mbpsSendRate = nullptr;
    bool valid = false;
};

PerfMonFields perfMonFields;
std::mutex sendHooksMutex;
std::unordered_map<SRTSOCKET, jobject> sendHooks;

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

void clearPendingException(JNIEnv* env)
{
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
}

jstring newStringAscii(JNIEnv* env, const char* value)
{
    if (value == nullptr) {
        return env->NewStringUTF("");
    }
    size_t length = strlen(value);
    NativeBuffer buffer(length + 1);
    char* ascii = buffer.data();
    for (size_t i = 0; i < length; i++) {
        auto character = static_cast<unsigned char>(value[i]);
        ascii[i] = character < 0x80 ? static_cast<char>(character) : '?';
    }
    ascii[length] = 0;
    return env->NewStringUTF(ascii);
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

int sendTrampoline(void* opaque, int, void* buf1, int size1, void* buf2, int size2)
{
    if (buf1 == nullptr || buf2 == nullptr || size1 < 0 || size2 < 0 || onSendMethod == nullptr) {
        return -1;
    }
    JNIEnv* env = getAttachedEnv();
    if (env == nullptr) {
        return -1;
    }
    auto socket = static_cast<SRTSOCKET>(reinterpret_cast<intptr_t>(opaque));
    jobject hook = nullptr;
    {
        std::lock_guard<std::mutex> lock(sendHooksMutex);
        auto entry = sendHooks.find(socket);
        if (entry != sendHooks.end()) {
            hook = env->NewLocalRef(entry->second);
        }
    }
    if (hook == nullptr) {
        clearPendingException(env);
        return -1;
    }
    int result = -1;
    jbyteArray packet = env->NewByteArray(size1 + size2);
    if (packet != nullptr) {
        env->SetByteArrayRegion(packet, 0, size1, static_cast<const jbyte*>(buf1));
        env->SetByteArrayRegion(packet, size1, size2, static_cast<const jbyte*>(buf2));
        jboolean sent = env->CallBooleanMethod(hook, onSendMethod, packet);
        if (!env->ExceptionCheck() && sent == JNI_TRUE) {
            result = size1 + size2;
        }
        env->DeleteLocalRef(packet);
    }
    clearPendingException(env);
    env->DeleteLocalRef(hook);
    return result;
}

void removeSendHook(JNIEnv* env, SRTSOCKET socket)
{
    jobject hook = nullptr;
    {
        std::lock_guard<std::mutex> lock(sendHooksMutex);
        auto entry = sendHooks.find(socket);
        if (entry != sendHooks.end()) {
            hook = entry->second;
            sendHooks.erase(entry);
        }
    }
    if (hook != nullptr) {
        env->DeleteGlobalRef(hook);
    }
}

bool copySockaddr(JNIEnv* env, jbyteArray name, jint namelen, sockaddr_storage* address)
{
    if (name == nullptr || namelen < 0 || static_cast<size_t>(namelen) > sizeof(sockaddr_storage)) {
        return false;
    }
    if (namelen > env->GetArrayLength(name)) {
        return false;
    }
    memset(address, 0, sizeof(sockaddr_storage));
    env->GetByteArrayRegion(name, 0, namelen, reinterpret_cast<jbyte*>(address));
    return true;
}

jint srtGetversion(JNIEnv*, jclass)
{
    return static_cast<jint>(srt_getversion());
}

jint srtStartup(JNIEnv*, jclass)
{
    return srt_startup();
}

jint srtCleanup(JNIEnv*, jclass)
{
    return srt_cleanup();
}

jint srtCreateSocket(JNIEnv*, jclass)
{
    return srt_create_socket();
}

jint srtClose(JNIEnv* env, jclass, jint u)
{
    int result = srt_close(u);
    removeSendHook(env, u);
    return result;
}

jint srtSetsockopt(JNIEnv* env, jclass, jint u, jint level, jint optname, jbyteArray optval, jint optlen)
{
    if (optval == nullptr || optlen < 0 || optlen > env->GetArrayLength(optval)) {
        return SRT_ERROR;
    }
    NativeBuffer buffer(static_cast<size_t>(optlen));
    env->GetByteArrayRegion(optval, 0, optlen, reinterpret_cast<jbyte*>(buffer.data()));
    return srt_setsockopt(u, level, static_cast<SRT_SOCKOPT>(optname), buffer.data(), optlen);
}

jint srtGetsockflagBytes(JNIEnv* env, jclass, jint u, jint opt, jbyteArray optval, jintArray optlen)
{
    if (optval == nullptr || optlen == nullptr || env->GetArrayLength(optlen) < 1) {
        return SRT_ERROR;
    }
    jsize capacity = env->GetArrayLength(optval);
    jint length = 0;
    env->GetIntArrayRegion(optlen, 0, 1, &length);
    if (length < 0) {
        return SRT_ERROR;
    }
    NativeBuffer buffer(static_cast<size_t>(capacity));
    env->GetByteArrayRegion(optval, 0, capacity, reinterpret_cast<jbyte*>(buffer.data()));
    int size = std::min<int>(length, capacity);
    int result = srt_getsockflag(u, static_cast<SRT_SOCKOPT>(opt), buffer.data(), &size);
    if (result != SRT_ERROR) {
        env->SetByteArrayRegion(optval, 0, capacity, reinterpret_cast<const jbyte*>(buffer.data()));
        jint newLength = size;
        env->SetIntArrayRegion(optlen, 0, 1, &newLength);
    }
    return result;
}

jint srtGetsockflagInts(JNIEnv* env, jclass, jint u, jint opt, jintArray optval, jintArray optlen)
{
    if (optval == nullptr || optlen == nullptr || env->GetArrayLength(optlen) < 1) {
        return SRT_ERROR;
    }
    jsize count = env->GetArrayLength(optval);
    jint length = 0;
    env->GetIntArrayRegion(optlen, 0, 1, &length);
    if (length < 0) {
        return SRT_ERROR;
    }
    size_t slots = std::max<size_t>(static_cast<size_t>(count), 16);
    std::unique_ptr<jint[]> values(new jint[slots]());
    env->GetIntArrayRegion(optval, 0, count, values.get());
    int size = std::min<int>(length, count * static_cast<int>(sizeof(jint)));
    int result = srt_getsockflag(u, static_cast<SRT_SOCKOPT>(opt), values.get(), &size);
    if (result != SRT_ERROR) {
        env->SetIntArrayRegion(optval, 0, count, values.get());
        jint newLength = size;
        env->SetIntArrayRegion(optlen, 0, 1, &newLength);
    }
    return result;
}

jint srtBind(JNIEnv* env, jclass, jint u, jbyteArray name, jint namelen)
{
    sockaddr_storage address;
    if (!copySockaddr(env, name, namelen, &address)) {
        return SRT_ERROR;
    }
    return srt_bind(u, reinterpret_cast<const sockaddr*>(&address), namelen);
}

jint srtListen(JNIEnv*, jclass, jint u, jint backlog)
{
    return srt_listen(u, backlog);
}

jint srtAccept(JNIEnv*, jclass, jint u)
{
    return srt_accept(u, nullptr, nullptr);
}

jint srtConnect(JNIEnv* env, jclass, jint u, jbyteArray name, jint namelen)
{
    sockaddr_storage address;
    if (!copySockaddr(env, name, namelen, &address)) {
        return SRT_ERROR;
    }
    return srt_connect(u, reinterpret_cast<const sockaddr*>(&address), namelen);
}

jint srtSendmsg2(JNIEnv* env, jclass, jint u, jbyteArray buf, jint len)
{
    if (buf == nullptr || len < 0 || len > env->GetArrayLength(buf)) {
        return SRT_ERROR;
    }
    NativeBuffer buffer(static_cast<size_t>(len));
    env->GetByteArrayRegion(buf, 0, len, reinterpret_cast<jbyte*>(buffer.data()));
    return srt_sendmsg2(u, buffer.data(), len, nullptr);
}

jint srtRecvmsg(JNIEnv* env, jclass, jint u, jbyteArray buf, jint len)
{
    if (buf == nullptr || len < 0 || len > env->GetArrayLength(buf)) {
        return SRT_ERROR;
    }
    NativeBuffer buffer(static_cast<size_t>(len));
    int result = srt_recvmsg(u, buffer.data(), len);
    if (result > 0) {
        env->SetByteArrayRegion(buf, 0, result, reinterpret_cast<const jbyte*>(buffer.data()));
    }
    return result;
}

jint srtBstats(JNIEnv* env, jclass, jint u, jobject perf, jint clear)
{
    SRT_TRACEBSTATS stats;
    memset(&stats, 0, sizeof(stats));
    int result = srt_bstats(u, &stats, clear);
    if (result == SRT_ERROR || perf == nullptr || !perfMonFields.valid) {
        return result;
    }
    env->SetIntField(perf, perfMonFields.pktRetransTotal, stats.pktRetransTotal);
    env->SetIntField(perf, perfMonFields.pktRecvNAKTotal, stats.pktRecvNAKTotal);
    env->SetIntField(perf, perfMonFields.pktSndDropTotal, stats.pktSndDropTotal);
    env->SetIntField(perf, perfMonFields.pktFlightSize, stats.pktFlightSize);
    env->SetDoubleField(perf, perfMonFields.msRTT, stats.msRTT);
    env->SetIntField(perf, perfMonFields.pktSndBuf, stats.pktSndBuf);
    env->SetDoubleField(perf, perfMonFields.mbpsSendRate, stats.mbpsSendRate);
    return result;
}

jint srtSendCallback(JNIEnv* env, jclass, jint u, jobject hook)
{
    if (hook == nullptr) {
        return SRT_ERROR;
    }
    jobject globalHook = env->NewGlobalRef(hook);
    if (globalHook == nullptr) {
        return SRT_ERROR;
    }
    jobject previousHook = nullptr;
    {
        std::lock_guard<std::mutex> lock(sendHooksMutex);
        auto entry = sendHooks.find(u);
        if (entry != sendHooks.end()) {
            previousHook = entry->second;
            entry->second = globalHook;
        } else {
            sendHooks.emplace(u, globalHook);
        }
    }
    if (previousHook != nullptr) {
        env->DeleteGlobalRef(previousHook);
    }
    int result = srt_send_callback(u, sendTrampoline, reinterpret_cast<void*>(static_cast<intptr_t>(u)));
    if (result == SRT_ERROR) {
        removeSendHook(env, u);
    }
    return result;
}

jstring srtGetlasterrorStr(JNIEnv* env, jclass)
{
    return newStringAscii(env, srt_getlasterror_str());
}

jint srtGetlasterror(JNIEnv* env, jclass, jintArray errnoLoc)
{
    int systemErrno = 0;
    int result = srt_getlasterror(&systemErrno);
    if (errnoLoc != nullptr && env->GetArrayLength(errnoLoc) > 0) {
        jint value = systemErrno;
        env->SetIntArrayRegion(errnoLoc, 0, 1, &value);
    }
    return result;
}

jint srtGetrejectreason(JNIEnv*, jclass, jint u)
{
    return srt_getrejectreason(u);
}

jstring srtRejectreasonStr(JNIEnv* env, jclass, jint reason)
{
    return newStringAscii(env, srt_rejectreason_str(reason));
}

void cachePerfMonFields(JNIEnv* env)
{
    jclass perfMonClass = env->FindClass(perfMonClassName);
    if (perfMonClass == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Class %s not found", perfMonClassName);
        return;
    }
    perfMonFields.perfMonClass = static_cast<jclass>(env->NewGlobalRef(perfMonClass));
    env->DeleteLocalRef(perfMonClass);
    const char* const intFields[] = {"pktRetransTotal", "pktRecvNAKTotal", "pktSndDropTotal", "pktFlightSize", "pktSndBuf"};
    jfieldID* const intFieldIds[] = {
        &perfMonFields.pktRetransTotal,
        &perfMonFields.pktRecvNAKTotal,
        &perfMonFields.pktSndDropTotal,
        &perfMonFields.pktFlightSize,
        &perfMonFields.pktSndBuf,
    };
    const char* const doubleFields[] = {"msRTT", "mbpsSendRate"};
    jfieldID* const doubleFieldIds[] = {&perfMonFields.msRTT, &perfMonFields.mbpsSendRate};
    bool valid = true;
    for (size_t i = 0; i < sizeof(intFields) / sizeof(intFields[0]); i++) {
        *intFieldIds[i] = env->GetFieldID(perfMonFields.perfMonClass, intFields[i], "I");
        if (*intFieldIds[i] == nullptr) {
            clearPendingException(env);
            __android_log_print(ANDROID_LOG_ERROR, logTag, "Field %s of %s not found", intFields[i], perfMonClassName);
            valid = false;
        }
    }
    for (size_t i = 0; i < sizeof(doubleFields) / sizeof(doubleFields[0]); i++) {
        *doubleFieldIds[i] = env->GetFieldID(perfMonFields.perfMonClass, doubleFields[i], "D");
        if (*doubleFieldIds[i] == nullptr) {
            clearPendingException(env);
            __android_log_print(ANDROID_LOG_ERROR, logTag, "Field %s of %s not found", doubleFields[i], perfMonClassName);
            valid = false;
        }
    }
    perfMonFields.valid = valid;
}

bool cacheSendHook(JNIEnv* env)
{
    jclass hookClass = env->FindClass(sendHookClassName);
    if (hookClass == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Class %s not found", sendHookClassName);
        return false;
    }
    sendHookClass = static_cast<jclass>(env->NewGlobalRef(hookClass));
    env->DeleteLocalRef(hookClass);
    onSendMethod = env->GetMethodID(sendHookClass, "onSend", "([B)Z");
    if (onSendMethod == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Method onSend of %s not found", sendHookClassName);
        return false;
    }
    return true;
}

const JNINativeMethod nativeMethods[] = {
    {"srt_getversion", "()I", reinterpret_cast<void*>(srtGetversion)},
    {"srt_startup", "()I", reinterpret_cast<void*>(srtStartup)},
    {"srt_cleanup", "()I", reinterpret_cast<void*>(srtCleanup)},
    {"srt_create_socket", "()I", reinterpret_cast<void*>(srtCreateSocket)},
    {"srt_close", "(I)I", reinterpret_cast<void*>(srtClose)},
    {"srt_setsockopt", "(III[BI)I", reinterpret_cast<void*>(srtSetsockopt)},
    {"srt_getsockflag", "(II[B[I)I", reinterpret_cast<void*>(srtGetsockflagBytes)},
    {"srt_getsockflag", "(II[I[I)I", reinterpret_cast<void*>(srtGetsockflagInts)},
    {"srt_bind", "(I[BI)I", reinterpret_cast<void*>(srtBind)},
    {"srt_listen", "(II)I", reinterpret_cast<void*>(srtListen)},
    {"srt_accept", "(I)I", reinterpret_cast<void*>(srtAccept)},
    {"srt_connect", "(I[BI)I", reinterpret_cast<void*>(srtConnect)},
    {"srt_sendmsg2", "(I[BI)I", reinterpret_cast<void*>(srtSendmsg2)},
    {"srt_recvmsg", "(I[BI)I", reinterpret_cast<void*>(srtRecvmsg)},
    {"srt_bstats", "(ILcom/moblin/android/media/haishinkit/srt/CBytePerfMon;I)I", reinterpret_cast<void*>(srtBstats)},
    {"srt_send_callback", "(ILcom/moblin/android/platform/srt/SrtSendHook;)I", reinterpret_cast<void*>(srtSendCallback)},
    {"srt_getlasterror_str", "()Ljava/lang/String;", reinterpret_cast<void*>(srtGetlasterrorStr)},
    {"srt_getlasterror", "([I)I", reinterpret_cast<void*>(srtGetlasterror)},
    {"srt_getrejectreason", "(I)I", reinterpret_cast<void*>(srtGetrejectreason)},
    {"srt_rejectreason_str", "(I)Ljava/lang/String;", reinterpret_cast<void*>(srtRejectreasonStr)},
};

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
    if (!cacheSendHook(env)) {
        return JNI_ERR;
    }
    cachePerfMonFields(env);
    jclass nativeClass = env->FindClass(srtNativeClassName);
    if (nativeClass == nullptr) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Class %s not found", srtNativeClassName);
        return JNI_ERR;
    }
    auto count = static_cast<jint>(sizeof(nativeMethods) / sizeof(nativeMethods[0]));
    jint result = env->RegisterNatives(nativeClass, nativeMethods, count);
    env->DeleteLocalRef(nativeClass);
    if (result != JNI_OK) {
        clearPendingException(env);
        __android_log_print(ANDROID_LOG_ERROR, logTag, "Failed to register the natives of %s", srtNativeClassName);
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}
