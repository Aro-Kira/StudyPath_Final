package com.example.studypath_final

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedTaskViewModel : ViewModel() {
    private val _tasks = MutableLiveData<List<Task_Data>>()
    val tasks: LiveData<List<Task_Data>> = _tasks

    fun addTask(task: Task_Data) {
        val updatedTasks = _tasks.value.orEmpty().toMutableList()
        updatedTasks.add(task)
        _tasks.value = updatedTasks  // This triggers the LiveData update
    }



}


