package com.example.studypath_final

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase



class Create_TaskActivity : AppCompatActivity() {


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)


        // Initialize Firebase Database
        FirebaseApp.initializeApp(this) // Manually initialize Firebase

        val database = FirebaseDatabase.getInstance() // Now, it should not crash

        // Now get Firebase database reference



        // Initialize other views
        val rootView = findViewById<View>(R.id.root_layout)

        val addChecklistButton = findViewById<Button>(R.id.add_checklist_item)
        val checklistContainer = findViewById<LinearLayout>(R.id.checklist_container)
        val startDateText = findViewById<TextView>(R.id.startDateText)
        val endDateText = findViewById<TextView>(R.id.endDateText)

        startDateText.setOnClickListener { showDatePicker(startDateText) }
        endDateText.setOnClickListener { showDatePicker(endDateText) }

        val saveTaskButton = findViewById<Button>(R.id.save_task_button)
        saveTaskButton.setOnClickListener {
            saveTask()
        }

        // Hide keyboard when touching outside EditText
        rootView.setOnTouchListener { v, event ->
            val focusedView = currentFocus
            if (focusedView is EditText) {
                focusedView.clearFocus()
                val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(focusedView.windowToken, 0)
            }
            false
        }

        // Add checklist item functionality
        addChecklistButton.setOnClickListener {
            val newChecklistItem = LayoutInflater.from(this).inflate(R.layout.checklist_item, checklistContainer, false)
            checklistContainer.addView(newChecklistItem)

            newChecklistItem.findViewById<ImageButton>(R.id.delete_checklist_item).setOnClickListener {
                checklistContainer.removeView(newChecklistItem)
            }
        }
    }

    private fun showDatePicker(textView: TextView) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val monthName = getMonthName(month) // No +1 needed
                val selectedDate = "$monthName $dayOfMonth, $year"
                textView.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun getMonthName(month: Int): String {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return months[month] // Directly fetch the correct month name
    }


    private fun saveTask() {
        val taskTitle = findViewById<EditText>(R.id.task_title).text.toString().trim()
        val taskNotes = findViewById<EditText>(R.id.task_notes).text.toString().trim()
        val startDate = findViewById<TextView>(R.id.startDateText).text.toString().trim()
        val endDate = findViewById<TextView>(R.id.endDateText).text.toString().trim()

        // Validate required fields
        if (taskTitle.isEmpty()) {
            showDetailsDialog("Task title cannot be empty!", isSuccess = false)
            return
        }
        if (startDate.isEmpty()) {
            showDetailsDialog("Start date cannot be empty!" , isSuccess = false)
            return
        }
        if (endDate.isEmpty()) {
            showDetailsDialog("End date cannot be empty!" , isSuccess = false)
            return
        }


        val checklistContainer = findViewById<LinearLayout>(R.id.checklist_container)
        val checklistItems = mutableListOf<ChecklistItem>()

        for (i in 0 until checklistContainer.childCount) {
            val checklistItemView = checklistContainer.getChildAt(i)
            val checklistEditText = checklistItemView.findViewById<EditText>(R.id.checklist_edit_text)
            val itemText = checklistEditText.text.toString().trim()
            val isChecked = checklistItemView.findViewById<CheckBox>(R.id.checklist_checkbox).isChecked
            if (itemText.isNotEmpty()) {
                checklistItems.add(ChecklistItem(itemText, isChecked))
            }
        }

        // Ensure at least one checklist item is present
        if (checklistItems.isEmpty()) {
            showDetailsDialog("Please add at least one checklist item!", isSuccess = false)
            return
        }


        // Create a unique ID for the task
        val taskId = UUID.randomUUID().toString()

        // Create a Task object
        val task = Task(
            taskId = taskId,
            title = taskTitle,
            notes = taskNotes,
            startDate = startDate,
            endDate = endDate,
            checklist = checklistItems

        )

        // Get the current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val database = FirebaseDatabase.getInstance()
            val tasksRef = database.reference.child("users").child(userId).child("tasks")
            tasksRef.child(taskId).setValue(task)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showDetailsDialog("Task saved successfully!", isSuccess = true)
                    } else {
                        showDetailsDialog("Failed to save task: ${task.exception?.message}",isSuccess = false)
                    }
                }
        } else {
            showDetailsDialog("User is not logged in!" , isSuccess = false)
        }
    }





    // Function to show details in a dialog
    private fun showDetailsDialog(details: String, isSuccess: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Saved Task Details")
            .setMessage(details)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // Finish the current activity only if the task was saved successfully
                if (isSuccess) {
                    finish()
                }
            }
            .show()
    }



}
