#include <jni.h>
#include <taglib/fileref.h>
#include <taglib/tag.h>
#include <taglib/audioproperties.h>
#include <taglib/toolkit/tpropertymap.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_pure_music_taglib_TagLibMetadataReader_readNative(JNIEnv* env, jclass, jstring path) {
    if (!path) return nullptr;
    const char* utfPath = env->GetStringUTFChars(path, nullptr);
    TagLib::FileRef file(utfPath);
    env->ReleaseStringUTFChars(path, utfPath);
    if (file.isNull() || !file.tag()) return nullptr;
    auto* tag = file.tag();
    auto* props = file.audioProperties();
    auto tagProps = tag->properties();
    auto first = [&](const char* key) -> std::string {
        auto it = tagProps.find(key);
        return it == tagProps.end() || it->second.isEmpty() ? "" : it->second.front().to8Bit(true);
    };
    auto clean = [](const TagLib::String& value) { return value.to8Bit(true); };
    const int duration = props ? props->lengthInMilliseconds() : 0;
    const int bitrate = props ? props->bitrate() : 0;
    const int sampleRate = props ? props->sampleRate() : 0;
    const int channels = props ? props->channels() : 0;
    std::string result = clean(tag->title()) + "\n" + clean(tag->artist()) + "\n" +
        clean(tag->album()) + "\n" + std::to_string(tag->track()) + "\n" +
        std::to_string(duration) + "\n" + std::to_string(bitrate) + "\n" +
        std::to_string(sampleRate) + "\n" + std::to_string(channels) + "\n" +
        first("LYRICS") + "\n" + first("COMPOSER") + "\n" + first("GENRE");
    return env->NewStringUTF(result.c_str());
}
