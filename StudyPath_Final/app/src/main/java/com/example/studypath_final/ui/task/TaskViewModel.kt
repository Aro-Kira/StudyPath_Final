package com.example.studypath_final.ui.task

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.studypath_final.Task
import com.example.studypath_final.ChecklistItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TaskViewModel : ViewModel() {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    val tasks: LiveData<List<Task>> get() = _tasks

    init {
        fetchTasksFromFirebase()
    }

    private fun fetchTasksFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val tasksRef = database.child(userId).child("tasks")
        tasksRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val taskList = snapshot.children.mapNotNull { it.getValue(Task::class.java) }
                _tasks.value = taskList
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskViewModel", "Error fetching tasks: ${error.message}")
            }
        })
    }

    fun addTask(task: Task) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val taskId = database.child(userId).child("tasks").push().key ?: return
        val newTask = task.copy(taskId = taskId) // ✅ Ensure taskId is set properly
        database.child(userId).child("tasks").child(taskId).setValue(newTask)
            .addOnSuccessListener {
                fetchTasksFromFirebase() // Refresh tasks after adding
            }
            .addOnFailureListener { e ->
                Log.e("TaskViewModel", "Error adding task: ${e.message}")
            }
    }

    fun updateChecklist(taskId: String, updatedChecklist: List<ChecklistItem>) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val taskRef = database.child(userId).child("tasks").child(taskId)

        // Update checklist in Firebase
        taskRef.child("checklist").setValue(updatedChecklist)
            .addOnSuccessListener {
                Log.d("TaskViewModel", "Checklist updated in Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("TaskViewModel", "Error updating checklist in Firebase: ${e.message}")
            }
    }

    fun removeTask(taskId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        database.child(userId).child("tasks").child(taskId).removeValue()
            .addOnSuccessListener {
                Log.d("TaskViewModel", "Task removed from Firebase")
            }
            .addOnFailureListener { e ->
                Log.e("TaskViewModel", "Error removing task: ${e.message}")
            }
    }
}
