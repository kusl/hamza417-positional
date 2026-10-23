package app.simple.positional.ui.compose

import app.simple.positional.constants.LauncherBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtsCatalogTest {
    @Test
    fun everyArtHasColors() {
        val backgrounds = LauncherBackground.vectorBackground.size + LauncherBackground.vectorBackgroundNight.size
        val colors = LauncherBackground.vectorColors.size + LauncherBackground.vectorNightColors.size
        assertEquals(backgrounds, colors)
        assertEquals(backgrounds, ArtsCatalog.arts.size)
    }

    @Test
    fun colorPairsAreComplete() {
        (LauncherBackground.vectorColors + LauncherBackground.vectorNightColors).forEach { assertTrue(it.size >= 2) }
    }

    @Test
    fun nightArtsFollowDayArts() {
        val day = LauncherBackground.vectorBackground.size
        assertEquals(LauncherBackground.vectorBackgroundNight[0], ArtsCatalog.arts[day].background)
        assertEquals(LauncherBackground.vectorNightColors[0][0], ArtsCatalog.arts[day].startColor)
    }
}
