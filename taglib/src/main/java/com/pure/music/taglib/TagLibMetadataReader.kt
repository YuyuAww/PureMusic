package com.pure.music.taglib

/** Metadata decoded by TagLib. Values are nullable because tags are optional. */
data class AudioMetadata(
    val title: String?, val artist: String?, val album: String?, val trackNumber: Int?,
    val durationMs: Long?, val bitrateKbps: Int?, val sampleRateHz: Int?, val channels: Int?,
    val lyrics: String? = null, val composer: String? = null, val genre: String? = null
)

object TagLibMetadataReader {
    private var loaded = false

    init { loaded = runCatching { System.loadLibrary("puremusic_taglib"); true }.getOrDefault(false) }

    fun read(path: String): AudioMetadata? {
        if (!loaded || path.isBlank()) return null
        return runCatching { readNative(path)?.split('\n')?.takeIf { it.size >= 8 }?.let { p ->
            AudioMetadata(p[0].ifBlank { null }, p[1].ifBlank { null }, p[2].ifBlank { null },
                p[3].toIntOrNull()?.takeIf { it > 0 }, p[4].toLongOrNull()?.takeIf { it > 0 },
                p[5].toIntOrNull()?.takeIf { it > 0 }, p[6].toIntOrNull()?.takeIf { it > 0 },
                p[7].toIntOrNull()?.takeIf { it > 0 }, p.getOrNull(8), p.getOrNull(9), p.getOrNull(10))
        } }.getOrNull()
    }

    @JvmStatic
    private external fun readNative(path: String): String?
}
