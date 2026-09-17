package com.pure.music.taglib

/**
 * 完整音频元数据，由 TagLib 读取。
 * 基本字段、扩展标签、技术参数和内嵌封面均有值时为 null 表示源文件缺失。
 */
data class AudioMetadata(
    val title: String?,
    val artist: String?,
    val album: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val date: String?,
    val subtitle: String?,
    val composer: String?,
    val lyricist: String?,
    val conductor: String?,
    val remixer: String?,
    val albumArtist: String?,
    val genre: String?,
    val mood: String?,
    val comment: String?,
    val lyrics: String?,
    val bpm: String?,
    val isrc: String?,
    val copyright: String?,
    val label: String?,
    val musicBrainzTrackId: String?,
    val musicBrainzAlbumId: String?,
    val musicBrainzArtistId: String?,
    // 技术参数
    val durationMs: Long?,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val channels: Int?,
    // 内嵌封面
    val coverMimeType: String?,
    val coverDescription: String?,
    val coverData: ByteArray?
) {
    val hasEmbeddedCover: Boolean get() = coverData != null && coverData.isNotEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioMetadata) return false
        return title == other.title &&
                artist == other.artist &&
                album == other.album &&
                trackNumber == other.trackNumber &&
                discNumber == other.discNumber &&
                year == other.year &&
                date == other.date &&
                subtitle == other.subtitle &&
                composer == other.composer &&
                lyricist == other.lyricist &&
                conductor == other.conductor &&
                remixer == other.remixer &&
                albumArtist == other.albumArtist &&
                genre == other.genre &&
                mood == other.mood &&
                comment == other.comment &&
                lyrics == other.lyrics &&
                bpm == other.bpm &&
                isrc == other.isrc &&
                copyright == other.copyright &&
                label == other.label &&
                musicBrainzTrackId == other.musicBrainzTrackId &&
                musicBrainzAlbumId == other.musicBrainzAlbumId &&
                musicBrainzArtistId == other.musicBrainzArtistId &&
                durationMs == other.durationMs &&
                bitrateKbps == other.bitrateKbps &&
                sampleRateHz == other.sampleRateHz &&
                channels == other.channels &&
                coverMimeType == other.coverMimeType &&
                coverDescription == other.coverDescription &&
                coverData?.contentEquals(other.coverData ?: ByteArray(0)) == true
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (trackNumber ?: 0)
        result = 31 * result + (discNumber ?: 0)
        result = 31 * result + (year ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        result = 31 * result + (composer?.hashCode() ?: 0)
        result = 31 * result + (lyricist?.hashCode() ?: 0)
        result = 31 * result + (conductor?.hashCode() ?: 0)
        result = 31 * result + (remixer?.hashCode() ?: 0)
        result = 31 * result + (albumArtist?.hashCode() ?: 0)
        result = 31 * result + (genre?.hashCode() ?: 0)
        result = 31 * result + (mood?.hashCode() ?: 0)
        result = 31 * result + (comment?.hashCode() ?: 0)
        result = 31 * result + (lyrics?.hashCode() ?: 0)
        result = 31 * result + (bpm?.hashCode() ?: 0)
        result = 31 * result + (isrc?.hashCode() ?: 0)
        result = 31 * result + (copyright?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (musicBrainzTrackId?.hashCode() ?: 0)
        result = 31 * result + (musicBrainzAlbumId?.hashCode() ?: 0)
        result = 31 * result + (musicBrainzArtistId?.hashCode() ?: 0)
        result = 31 * result + (durationMs?.hashCode() ?: 0)
        result = 31 * result + (bitrateKbps?.hashCode() ?: 0)
        result = 31 * result + (sampleRateHz?.hashCode() ?: 0)
        result = 31 * result + (channels?.hashCode() ?: 0)
        result = 31 * result + (coverMimeType?.hashCode() ?: 0)
        result = 31 * result + (coverDescription?.hashCode() ?: 0)
        result = 31 * result + (coverData?.contentHashCode() ?: 0)
        return result
    }
}

object TagLibMetadataReader {
    private var loaded = false

    init {
        loaded = runCatching {
            System.loadLibrary("puremusic_taglib")
            true
        }.getOrDefault(false)
    }

    fun read(path: String): AudioMetadata? {
        if (!loaded || path.isBlank()) return null
        return runCatching {
            val raw = readNative(path) ?: return null
            // Fields: 0..29 (text fields only, picData is read separately)
            // 0:title 1:artist 2:album 3:track 4:durationMs 5:bitrateKbps 6:sampleRateHz 7:channels
            // 8:lyrics 9:composer 10:genre 11:comment 12:year 13:albumArtist 14:subtitle 15:date
            // 16:discNumber 17:lyricist 18:conductor 19:remixer 20:isrc 21:bpm 22:copyright
            // 23:mood 24:label 25:mbTrackId 26:mbAlbumId 27:mbArtistId
            // 28:picMime 29:picDesc
            val fields = raw.split('\u001f')
            if (fields.size < 30) return null
            fun s(i: Int) = fields.getOrNull(i)?.takeIf { it.isNotEmpty() }
            fun int(i: Int) = s(i)?.toIntOrNull()?.takeIf { it > 0 }
            fun intOrZero(i: Int) = s(i)?.toIntOrNull()?.takeIf { it >= 0 }

            val picMime = s(28)
            val picDesc = s(29)
            val picData = readCoverNative(path)

            AudioMetadata(
                title = s(0),
                artist = s(1),
                album = s(2),
                trackNumber = int(3),
                discNumber = int(16),
                year = intOrZero(12),
                date = s(15),
                subtitle = s(14),
                composer = s(9),
                lyricist = s(17),
                conductor = s(18),
                remixer = s(19),
                albumArtist = s(13),
                genre = s(10),
                mood = s(23),
                comment = s(11),
                lyrics = s(8),
                bpm = s(21),
                isrc = s(20),
                copyright = s(22),
                label = s(24),
                musicBrainzTrackId = s(25),
                musicBrainzAlbumId = s(26),
                musicBrainzArtistId = s(27),
                durationMs = fields.getOrNull(4)?.toLongOrNull()?.takeIf { it > 0 },
                bitrateKbps = int(5),
                sampleRateHz = int(6),
                channels = int(7),
                coverMimeType = picMime,
                coverDescription = picDesc,
                coverData = picData
            )
        }.getOrNull()
    }

    @JvmStatic
    private external fun readNative(path: String): String?

    @JvmStatic
    private external fun readCoverNative(path: String): ByteArray?
}
