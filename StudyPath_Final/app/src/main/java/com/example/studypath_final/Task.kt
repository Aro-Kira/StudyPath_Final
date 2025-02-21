package com.example.studypath_final

data class ChecklistItem(
    var text: String = "",
    var isChecked: Boolean = false
)

data class Task(
    var taskId: String = "",
    var title: String = "",
    var notes: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var checklist: List<ChecklistItem> = mutableListOf(), // Changed to MutableList for easy updates
    var progress: Int = 0
)
data class Streak(
    val userId: String = "",
    var streakCount: Int = 0,
    var lastCompleted: String = "" // Store date as String (e.g., "2025-02-11")
)
