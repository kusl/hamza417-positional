package app.simple.positional.constants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LauncherArtTest {
    private val art = File("src/main/res/drawable").listFiles { file -> file.name.startsWith("launcher_") }.orEmpty().toList()

    @Test
    fun everyArtHasColors() {
        assertEquals(LauncherBackground.vectorBackground.size, LauncherBackground.vectorColors.size)
        assertEquals(LauncherBackground.vectorBackgroundNight.size, LauncherBackground.vectorNightColors.size)
        assertEquals(art.size, LauncherBackground.vectorBackground.size + LauncherBackground.vectorBackgroundNight.size)
    }

    @Test
    fun pathDataIsWellFormedAndCompact() {
        assertTrue(art.isNotEmpty())
        art.forEach { file ->
            PATH.findAll(file.readText()).forEach { path ->
                val data = path.groupValues[1]
                val segments = SEGMENT.findAll(data).toList()
                assertEquals(file.name, data, segments.joinToString("") { it.value })
                assertTrue(file.name, data.first() in "Mm")
                segments.forEach { segment ->
                    val command = segment.groupValues[1].lowercaseChar()
                    val rest = segment.groupValues[2]
                    val numbers = NUMBER.findAll(rest).map { it.value }.toList()
                    assertTrue(file.name, rest.replace(NUMBER, "").all { it == ' ' || it == ',' })
                    val arity = ARITY.getValue(command)
                    assertTrue("${file.name} $command", if (arity == 0) numbers.isEmpty() else numbers.isNotEmpty() && numbers.size % arity == 0)
                    if (command != 'a') numbers.forEach { assertTrue("${file.name} $it", COMPACT.matches(it)) }
                }
            }
        }
    }

    private companion object {
        val PATH = Regex("pathData=\"([^\"]*)\"")
        val SEGMENT = Regex("([MmLlHhVvCcSsQqTtAaZz])([^MmLlHhVvCcSsQqTtAaZz]*)")
        val NUMBER = Regex("-?(?:\\d+\\.?\\d*|\\.\\d+)")
        val COMPACT = Regex("-?(?:\\d+|\\d*\\.\\d)")
        val ARITY = mapOf('m' to 2, 'l' to 2, 'h' to 1, 'v' to 1, 'c' to 6, 's' to 4, 'q' to 4, 't' to 2, 'a' to 7, 'z' to 0)
    }
}
