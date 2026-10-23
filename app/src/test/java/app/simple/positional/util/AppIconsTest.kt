package app.simple.positional.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppIconsTest {
    @Test
    fun aliasesAndIconsAreUnique() {
        assertEquals(12, AppIcons.icons.size)
        assertEquals(AppIcons.icons.size, AppIcons.icons.map { it.first }.toSet().size)
        assertEquals(AppIcons.icons.size, AppIcons.icons.map { it.second }.toSet().size)
    }

    @Test
    fun componentNamesPointToAliasPackage() {
        AppIcons.icons.forEach { (alias, _) ->
            assertTrue(AppIcons.componentName(alias).startsWith("app.simple.positional.activities.alias."))
            assertTrue(AppIcons.componentName(alias).endsWith(alias))
        }
    }

    @Test
    fun manifestDeclaresEveryAlias() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        AppIcons.icons.forEach { (alias, _) -> assertTrue(alias, manifest.contains(".activities.alias.$alias\"")) }
    }
}
