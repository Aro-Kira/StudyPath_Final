package com.example.studypath_final

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.UUID

data class Task_Data(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String,
    val checklist: List<String>,
    val progress: Int = 0
)


