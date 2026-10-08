package com.randolph.keeplocal.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistParserTest {

    @Test
    fun testParseContentToChecklist() {
        val rawContent = """
            [ ] Buy almond milk
            [x] Check organic apples
            [ ] Prepare project roadmap
        """.trimIndent()

        val items = ChecklistParser.parseContentToChecklist(rawContent)
        assertEquals(3, items.size)
        assertFalse(items[0].isChecked)
        assertEquals("Buy almond milk", items[0].text)

        assertTrue(items[1].isChecked)
        assertEquals("Check organic apples", items[1].text)

        assertFalse(items[2].isChecked)
        assertEquals("Prepare project roadmap", items[2].text)
    }

    @Test
    fun testFormatChecklistToContent() {
        val items = listOf(
            ChecklistItem(isChecked = false, text = "Task 1"),
            ChecklistItem(isChecked = true, text = "Task 2")
        )

        val formatted = ChecklistParser.formatChecklistToContent(items)
        val expected = "[ ] Task 1\n[x] Task 2"
        assertEquals(expected, formatted)
    }
}
