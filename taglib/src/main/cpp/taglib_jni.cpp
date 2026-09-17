#include <jni.h>
#include <taglib/fileref.h>
#include <taglib/tag.h>
#include <taglib/audioproperties.h>
#include <taglib/toolkit/tpropertymap.h>
#include <algorithm>
#include <string>
#include <initializer_list>

static const char SEP = '\x1f';

static std::string clean(const TagLib::String& s) {
    std::string out = s.to8Bit(true);
    out.erase(std::remove(out.begin(), out.end(), '\x1f'), out.end());
    out.erase(std::remove(out.begin(), out.end(), '\x1e'), out.end());
    out.erase(std::remove(out.begin(), out.end(), '\x1d'), out.end());
    return out;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_pure_music_taglib_TagLibMetadataReader_readNative(JNIEnv* env, jclass, jstring path) {
    if (!path) return nullptr;
    const char* utfPath = env->GetStringUTFChars(path, nullptr);
    TagLib::FileRef file(utfPath);
    env->ReleaseStringUTFChars(path, utfPath);
    if (file.isNull()) return nullptr;

    auto* tag = file.tag();
    auto* props = file.audioProperties();

    const int durationMs = props ? props->lengthInMilliseconds() : 0;
    const int bitrateKbps = props ? props->bitrate() : 0;
    const int sampleRate = props ? props->sampleRate() : 0;
    const int channels = props ? props->channels() : 0;

    const auto title = tag ? clean(tag->title()) : std::string();
    const auto artist = tag ? clean(tag->artist()) : std::string();
    const auto album = tag ? clean(tag->album()) : std::string();
    const auto comment = tag ? clean(tag->comment()) : std::string();
    const auto genre = tag ? clean(tag->genre()) : std::string();
    const unsigned int year = tag ? tag->year() : 0u;
    const unsigned int track = tag ? tag->track() : 0u;

    TagLib::PropertyMap tagProps = tag ? tag->properties() : TagLib::PropertyMap();
    auto firstVal = [&](std::initializer_list<const char*> keys) -> std::string {
        for (auto key : keys) {
            auto it = tagProps.find(key);
            if (it != tagProps.end() && !it->second.isEmpty())
                return clean(it->second.front());
        }
        return std::string();
    };

    std::string lyrics = firstVal({"LYRICS", "UNSYNCEDLYRICS", "USLT"});
    std::string composer = firstVal({"COMPOSER", "COMPOSERS"});
    std::string albumArtist = firstVal({"ALBUMARTIST", "ALBUM ARTIST", "TPE2"});
    std::string subtitle = firstVal({"SUBTITLE"});
    std::string dateStr = firstVal({"DATE", "YEAR", "TDAT"});
    std::string discNumberStr = firstVal({"DISCNUMBER"});
    std::string lyricist = firstVal({"LYRICIST"});
    std::string conductor = firstVal({"CONDUCTOR"});
    std::string remixer = firstVal({"REMIXER"});
    std::string isrc = firstVal({"ISRC"});
    std::string bpm = firstVal({"BPM"});
    std::string copyright = firstVal({"COPYRIGHT"});
    std::string mood = firstVal({"MOOD"});
    std::string label = firstVal({"LABEL"});
    std::string mbTrackId = firstVal({"MUSICBRAINZ_TRACKID", "MUSICBRAINZTRACKID"});
    std::string mbAlbumId = firstVal({"MUSICBRAINZ_ALBUMID", "MUSICBRAINZALBUMID"});
    std::string mbArtistId = firstVal({"MUSICBRAINZ_ARTISTID", "MUSICBRAINZARTISTID"});

    // Embedded cover: data + mimeType + description
    std::string picData;
    std::string picMime;
    std::string picDesc;
    if (tag) {
        TagLib::StringList picKeys = tag->complexPropertyKeys();
        if (std::find(picKeys.begin(), picKeys.end(), TagLib::String("PICTURE")) != picKeys.end()) {
            auto pics = tag->complexProperties(TagLib::String("PICTURE"));
            if (!pics.isEmpty()) {
                auto& firstPic = pics.front();
                auto dataIt = firstPic.find(TagLib::String("data"));
                if (dataIt != firstPic.end()) {
                    bool ok = false;
                    auto bv = dataIt->second.toByteVector(&ok);
                    if (ok) {
                        const unsigned char* raw = bv.data();
                        int len = static_cast<int>(bv.size());
                        if (len > 256 * 1024) len = 256 * 1024;
                        picData.assign(reinterpret_cast<const char*>(raw), len);
                    }
                }
                auto mimeIt = firstPic.find(TagLib::String("mimeType"));
                if (mimeIt != firstPic.end()) picMime = clean(mimeIt->second.toString());
                auto descIt = firstPic.find(TagLib::String("description"));
                if (descIt != firstPic.end()) picDesc = clean(descIt->second.toString());
            }
        }
    }

    // Fields: 0..29 (text fields only, picData is separate)
    //  0:title 1:artist 2:album 3:track 4:durationMs 5:bitrateKbps 6:sampleRateHz 7:channels
    //  8:lyrics 9:composer 10:genre 11:comment 12:year 13:albumArtist 14:subtitle 15:date
    //  16:discNumber 17:lyricist 18:conductor 19:remixer 20:isrc 21:bpm 22:copyright
    //  23:mood 24:label 25:mbTrackId 26:mbAlbumId 27:mbArtistId
    //  28:picMime 29:picDesc
    std::string result;
    result += title + SEP + artist + SEP + album + SEP;
    result += std::to_string(track) + SEP;
    result += std::to_string(durationMs) + SEP;
    result += std::to_string(bitrateKbps) + SEP;
    result += std::to_string(sampleRate) + SEP;
    result += std::to_string(channels) + SEP;
    result += lyrics + SEP + composer + SEP + genre + SEP + comment + SEP;
    result += std::to_string(year) + SEP;
    result += albumArtist + SEP + subtitle + SEP + dateStr + SEP;
    result += discNumberStr + SEP + lyricist + SEP + conductor + SEP + remixer + SEP;
    result += isrc + SEP + bpm + SEP + copyright + SEP + mood + SEP + label + SEP;
    result += mbTrackId + SEP + mbAlbumId + SEP + mbArtistId + SEP;
    result += picMime + SEP + picDesc;

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_pure_music_taglib_TagLibMetadataReader_readCoverNative(JNIEnv* env, jclass, jstring path) {
    if (!path) return nullptr;
    const char* utfPath = env->GetStringUTFChars(path, nullptr);
    TagLib::FileRef file(utfPath, false);
    env->ReleaseStringUTFChars(path, utfPath);
    if (file.isNull() || !file.tag()) return nullptr;

    auto* tag = file.tag();
    TagLib::StringList picKeys = tag->complexPropertyKeys();
    if (std::find(picKeys.begin(), picKeys.end(), TagLib::String("PICTURE")) == picKeys.end())
        return nullptr;

    auto pics = tag->complexProperties(TagLib::String("PICTURE"));
    if (pics.isEmpty()) return nullptr;

    auto& firstPic = pics.front();
    auto dataIt = firstPic.find(TagLib::String("data"));
    if (dataIt == firstPic.end()) return nullptr;

    bool ok = false;
    auto bv = dataIt->second.toByteVector(&ok);
    if (!ok || bv.isEmpty()) return nullptr;

    int len = static_cast<int>(bv.size());
    if (len > 256 * 1024) len = 256 * 1024;

    jbyteArray arr = env->NewByteArray(len);
    env->SetByteArrayRegion(arr, 0, len, reinterpret_cast<const jbyte*>(bv.data()));
    return arr;
}
