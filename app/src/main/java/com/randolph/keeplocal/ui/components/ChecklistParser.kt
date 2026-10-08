package com.randolph.keeplocal.ui.components

data class ChecklistItem(
    val isChecked: Boolean = false,
    val text: String = ""
)

object ChecklistParser {
    fun parseContentToChecklist(content: String): List<ChecklistItem> {
        if (content.isBlank()) return emptyList()
        val lines = content.lines()
        val items = mutableListOf<ChecklistItem>()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[x]") -> items.add(ChecklistItem(isChecked = true, text = trimmed.removePrefix("[x]").trim()))
                trimmed.startsWith("[X]") -> items.add(ChecklistItem(isChecked = true, text = trimmed.removePrefix("[X]").trim()))
                trimmed.startsWith("[ ]") -> items.add(ChecklistItem(isChecked = false, text = trimmed.removePrefix("[ ]").trim()))
                trimmed.isNotEmpty() -> items.add(ChecklistItem(isChecked = false, text = trimmed))
            }
        }
        return items
    }

    fun formatChecklistToContent(items: List<ChecklistItem>): String {
        return items.filter { it.text.isNotBlank() }
            .joinToString("\n") { item ->
                if (item.isChecked) "[x] ${item.text}" else "[ ] ${item.text}"
            }
    }
}
