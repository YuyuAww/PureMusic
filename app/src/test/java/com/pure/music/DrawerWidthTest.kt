package com.pure.music

import androidx.compose.ui.unit.dp
import com.pure.music.ui.drawerWidthFor
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawerWidthTest {
    @Test
    fun belowThreeSixtyUsesEightyPercentOfWindowWidth() {
        assertEquals(300.dp * 0.8f, drawerWidthFor(300.dp))
        assertEquals(320.dp * 0.8f, drawerWidthFor(320.dp))
        assertEquals(359.dp * 0.8f, drawerWidthFor(359.dp))
    }

    @Test
    fun betweenThreeSixtyAndSixHundredUsesHalfOfWindowWidth() {
        assertEquals(180.dp, drawerWidthFor(360.dp))
        assertEquals(240.dp, drawerWidthFor(480.dp))
        assertEquals(599.dp * 0.5f, drawerWidthFor(599.dp))
    }

    @Test
    fun betweenSixHundredAndEightFortyIsFixedThreeTwenty() {
        assertEquals(320.dp, drawerWidthFor(600.dp))
        assertEquals(320.dp, drawerWidthFor(700.dp))
        assertEquals(320.dp, drawerWidthFor(839.dp))
    }

    @Test
    fun eightFortyAndWiderIsFixedThreeSixty() {
        assertEquals(360.dp, drawerWidthFor(840.dp))
        assertEquals(360.dp, drawerWidthFor(1200.dp))
    }
}
