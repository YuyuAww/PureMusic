package com.ella.music.data.lastfm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LastFmModelsTest {

    @Test
    fun historySourceFlagsKeepLocalAndRemoteModesDistinct() {
        assertTrue(ListeningHistorySource.Local.usesLocal)
        assertTrue(!ListeningHistorySource.Local.usesLastFm)
        assertTrue(!ListeningHistorySource.LastFm.usesLocal)
        assertTrue(ListeningHistorySource.LastFm.usesLastFm)
        assertTrue(ListeningHistorySource.Combined.usesLocal)
        assertTrue(ListeningHistorySource.Combined.usesLastFm)
    }

    @Test
    fun remoteSyntheticIdsAreStableAndNeverCollideWithMediaStoreIds() {
        val first = stableLastFmSongId("Winter Bells", "Mai Kuraki", "Winter Bells")
        val second = stableLastFmSongId(" Winter  Bells ", "mai kuraki", "Winter Bells")

        assertEquals(first, second)
        assertTrue(first < 0L)
    }

    @Test
    fun remoteHistoryKeepsDurationWhenTheSongIsNotInTheLocalLibrary() {
        val entry = LastFmTrack(
            title = "Winter Bells",
            artist = "Mai Kuraki",
            album = "Winter Bells",
            playedAt = 1_000_000L,
            durationMs = 211_000L
        ).toPlaybackHistoryEntry()

        assertEquals(211_000L, entry.durationMs)
    }
}
