package com.example.studypath_final.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.studypath_final.data.repository.FirestoreRepository
import com.example.studypath_final.Task

class AuthViewModel : ViewModel() {

    private val firestoreRepository = FirestoreRepository()

    // 🔵 SIGN UP User
    fun signupUser(username: String, email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        firestoreRepository.signupUser(username, email, password) { success, message ->
            onComplete(success, message)
        }
    }

    // 🔵 LOGIN User
    fun loginUser(username: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        firestoreRepository.loginUser(username, password) { success, message ->
            onComplete(success, message)
        }
    }

    // 🔵 ADD Task
    fun addTask(userId: String, task: Task, onComplete: (Boolean, String?) -> Unit) {
        firestoreRepository.addTask(userId, task) { success, message ->
            onComplete(success, message)
        }
    }

    // 🔵 GET Tasks
    fun getUserTasks(userId: String, onComplete: (List<Task>?, String?) -> Unit) {
        firestoreRepository.getUserTasks(userId) { tasks, message ->
            onComplete(tasks, message)
        }
    }

    // 🔵 DELETE Task
    fun deleteTask(userId: String, taskId: String, onComplete: (Boolean, String?) -> Unit) {
        firestoreRepository.deleteTask(userId, taskId) { success, message ->
            onComplete(success, message)
        }
    }
}
