package app.simple.positional.constants

import app.simple.positional.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HtmlPageTest {
    @Test
    fun everyPageHasAnAsset() {
        HtmlPage.entries.forEach { assertTrue(it.asset, File("src/main/assets/html/${it.asset}.html").isFile) }
    }

    @Test
    fun everyAssetHasAPage() {
        val assets = File("src/main/assets/html").listFiles().orEmpty().map { it.nameWithoutExtension }.toSet()
        assertEquals(HtmlPage.entries.map { it.asset }.toSet(), assets)
    }

    @Test
    fun legalTitlesResolve() {
        assertEquals(HtmlPage.PRIVACY_POLICY, HtmlPage.forTitle(R.string.privacy_policy))
        assertEquals(HtmlPage.TERMS, HtmlPage.forTitle(R.string.terms_of_use))
        assertEquals(HtmlPage.CREDITS, HtmlPage.forTitle(R.string.credits))
        assertNull(HtmlPage.forTitle(R.string.app_name))
    }

    @Test
    fun namesRoundTrip() {
        HtmlPage.entries.forEach { assertEquals(it, HtmlPage.forName(it.name)) }
        assertNull(HtmlPage.forName(null))
        assertNull(HtmlPage.forName("unknown"))
    }

    @Test
    fun urlPointsToAndroidAssets() {
        assertEquals("file:///android_asset/html/credits.html", HtmlPage.CREDITS.url)
    }
}
