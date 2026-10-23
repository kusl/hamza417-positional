package app.simple.positional.ui.compose

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ComposeDrawablesTest {
    private val sources = listOf("src/main/java/app/simple/positional/ui/compose", "src/main/java/app/simple/positional/constants", "src/main/java/app/simple/positional/util/MoonAngle.kt")
    private val reference = Regex("""R\.drawable\.(\w+)""")
    private val root = Regex("""<([a-zA-Z-]+)[\s>]""")

    @Test
    fun composeDrawablesAreVectorsOrBitmaps() {
        val names = sources.flatMap { path -> File(path).walk().filter { it.extension == "kt" }.toList() }
            .flatMap { file -> reference.findAll(file.readText()).map { it.groupValues[1] }.toList() }
            .toSet()
        assertTrue(names.isNotEmpty())
        val drawables = File("src/main/res").listFiles { file -> file.name.startsWith("drawable") }.orEmpty().flatMap { it.listFiles().orEmpty().toList() }
        names.forEach { name ->
            val files = drawables.filter { it.nameWithoutExtension == name }
            assertTrue(name, files.isNotEmpty())
            files.filter { it.extension == "xml" }.forEach { file ->
                val text = file.readText().replace(Regex("""<\?xml[^>]*>"""), "")
                assertTrue("$name is ${root.find(text)?.groupValues?.get(1)}", root.find(text)?.groupValues?.get(1) == "vector")
            }
        }
    }
}
