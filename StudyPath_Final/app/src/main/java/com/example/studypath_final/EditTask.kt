package com.example.studypath_final

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import java.text.SimpleDateFormat
import java.util.*
import com.example.studypath_final.databinding.ActivityEditTaskBinding

class EditTask : AppCompatActivity() {

    private lateinit var binding: ActivityEditTaskBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val taskId = intent.getStringExtra("TASK_ID") ?: ""
        if (taskId.isNotEmpty()) fetchTaskData(taskId) else showSnackbar("Task ID not found")

        binding.editStartDateText.setOnClickListener { showDatePicker(binding.editStartDateText) }
        binding.editEndDateText.setOnClickListener { showDatePicker(binding.editEndDateText) }

        binding.editSaveTaskButton.setOnClickListener { confirmSaveTask(taskId) }
        binding.editAddChecklistItem.setOnClickListener { addChecklistItem() }
        binding.editDeleteTaskButton.setOnClickListener { confirmDeleteTask(taskId) }
    }

    private fun fetchTaskData(taskId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val taskRef = FirebaseDatabase.getInstance().reference.child("users").child(userId).child("tasks").child(taskId)

        taskRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(Task::class.java)?.let { populateTaskData(it) } ?: showSnackbar("Task not found")
            }
            override fun onCancelled(error: DatabaseError) {
                showSnackbar("Failed to fetch data: ${error.message}")
            }
        })
    }

    private fun populateTaskData(task: Task) {
        binding.apply {
            editTaskTitle.setText(task.title)
            editTaskNotes.setText(task.notes)
            editStartDateText.text = task.startDate
            editEndDateText.text = task.endDate
            editChecklistContainer.removeAllViews()
            task.checklist.forEach { addChecklistItem(it.text, it.isChecked) }
        }
    }


    private fun addChecklistItem(text: String = "", isChecked: Boolean = false) {
        val itemView = layoutInflater.inflate(R.layout.checklist_item, binding.editChecklistContainer, false)
        itemView.findViewById<EditText>(R.id.checklist_edit_text).setText(text)
        itemView.findViewById<CheckBox>(R.id.checklist_checkbox).isChecked = isChecked
        itemView.findViewById<ImageButton>(R.id.delete_checklist_item).setOnClickListener {
            binding.editChecklistContainer.removeView(itemView)
        }
        binding.editChecklistContainer.addView(itemView)
    }

    private fun showDatePicker(targetView: TextView) {
        val constraints = CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build()
        MaterialDatePicker.Builder.datePicker().setTitleText("Select a Date").setCalendarConstraints(constraints).build().apply {
            show(supportFragmentManager, "DATE_PICKER")
            addOnPositiveButtonClickListener { targetView.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it)) }
        }
    }

    private fun confirmSaveTask(taskId: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Save")
            .setMessage("Are you sure you want to save the changes?")
            .setPositiveButton("Save") { _, _ -> updateTask(taskId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTask(taskId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return showSnackbar("User not logged in!")
        if (taskId.isEmpty()) return showSnackbar("Task ID is missing!")

        val checklistItems = (0 until binding.editChecklistContainer.childCount).mapNotNull {
            binding.editChecklistContainer.getChildAt(it).let { view ->
                val text = view.findViewById<EditText>(R.id.checklist_edit_text).text.toString().trim()
                if (text.isNotEmpty()) ChecklistItem(text, view.findViewById<CheckBox>(R.id.checklist_checkbox).isChecked) else null
            }
        }
        if (checklistItems.isEmpty()) return showSnackbar("Please add at least one checklist item!")


        val updatedTask = Task(taskId, binding.editTaskTitle.text.toString().trim(), binding.editTaskNotes.text.toString().trim(),
            binding.editStartDateText.text.toString().trim(), binding.editEndDateText.text.toString().trim(),
            checklistItems)

        FirebaseDatabase.getInstance().reference.child("users").child(userId).child("tasks").child(taskId).setValue(updatedTask)
            .addOnSuccessListener { showSnackbar("Task updated successfully!") }
            .addOnFailureListener { showSnackbar("Failed to update task: ${it.message}") }
    }

    private fun confirmDeleteTask(taskId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ -> deleteTask(taskId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(taskId: String) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            FirebaseDatabase.getInstance().reference.child("users").child(userId).child("tasks").child(taskId).removeValue()
                .addOnSuccessListener {
                    showSnackbar("Task deleted successfully!")
                    finish()
                }
                .addOnFailureListener { showSnackbar("Failed to delete task: ${it.message}") }
        } ?: showSnackbar("User not logged in!")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
