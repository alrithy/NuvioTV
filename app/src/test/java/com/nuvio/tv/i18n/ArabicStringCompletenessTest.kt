package com.nuvio.tv.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * The Arabic build must never fall back to English: every translatable string, plural and array in
 * values/ needs an entry in values-ar/, with the same format placeholders.
 */
class ArabicStringCompletenessTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }

    private data class Entry(val file: String, val placeholders: Set<String>)

    private fun entries(dir: String): Map<String, Entry> {
        val factory = DocumentBuilderFactory.newInstance()
        return File(res, dir).listFiles { file -> file.extension == "xml" }.orEmpty().flatMap { file ->
            val root = factory.newDocumentBuilder().parse(file).documentElement
            (0 until root.childNodes.length).mapNotNull { root.childNodes.item(it) as? Element }
                .filter { it.tagName in setOf("string", "plurals", "string-array") }
                .filter { it.getAttribute("translatable") != "false" }
                .map { it.getAttribute("name") to Entry(file.name, placeholders(it)) }
        }.toMap()
    }

    private fun placeholders(element: Element): Set<String> =
        if (element.tagName == "string") PLACEHOLDER.findAll(element.textContent).map { it.value }.toSet() else emptySet()

    @Test
    fun everyTranslatableStringHasArabic() {
        val english = entries("values")
        val arabic = entries("values-ar")
        val missing = english.filterKeys { it !in arabic }.map { (name, entry) -> "${entry.file}: $name" }.sorted()
        assertTrue("Strings without Arabic (${missing.size}):\n" + missing.joinToString("\n"), missing.isEmpty())
    }

    @Test
    fun arabicKeepsTheEnglishPlaceholders() {
        val english = entries("values")
        val mismatched = entries("values-ar").mapNotNull { (name, entry) ->
            val source = english[name] ?: return@mapNotNull null
            if (source.placeholders == entry.placeholders) null else "$name: ${source.placeholders} vs ${entry.placeholders}"
        }.sorted()
        assertEquals("", mismatched.joinToString("\n"))
    }

    private companion object {
        val PLACEHOLDER = Regex("%(\\d+\\$)?[sd]")
    }
}
