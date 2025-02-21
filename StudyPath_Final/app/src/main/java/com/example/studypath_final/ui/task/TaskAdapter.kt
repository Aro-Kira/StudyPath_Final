package com.example.studypath_final.ui.task

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studypath_final.EditTask
import com.example.studypath_final.Task
import com.example.studypath_final.Streak
import com.example.studypath_final.databinding.TaskCardItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onTaskClick: (String) -> Unit,
    private val onRemoveTask: (String) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TaskCardItemBinding.inflate(inflater, parent, false)
        return TaskViewHolder(binding, onTaskClick, onRemoveTask)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(
        private val binding: TaskCardItemBinding,
        private val onTaskClick: (String) -> Unit,
        private val onRemoveTask: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(task: Task) {
            binding.taskCardTitle.text = task.title
            binding.taskCardNotes.text = task.notes

            val completedSteps = task.checklist.count { it.isChecked }
            val totalSteps = task.checklist.size
            val progress = if (totalSteps > 0) (completedSteps * 100) / totalSteps else 0

            binding.progressBar.progress = progress
            binding.progressCounter.text = "$completedSteps/$totalSteps"

            // Show "COMPLETE" button only if task is 100% complete
            binding.taskComplete.visibility = if (progress == 100) View.VISIBLE else View.GONE

            if (task.endDate.isNotEmpty()) {
                binding.taskCardDate.text = "Due: ${task.endDate}"
            } else {
                binding.taskCardDate.text = "Due: Not set"
            }

            // Click listener for task selection
            binding.root.setOnClickListener {
                onTaskClick(task.taskId)
            }

            // Click listener for editing task
            binding.editTaskIcon.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, EditTask::class.java)
                intent.putExtra("TASK_ID", task.taskId)
                context.startActivity(intent)
            }

            // Click listener for task completion
            binding.taskComplete.setOnClickListener {
                val context = binding.root.context
                showCompletionDialog(context, task.taskId)
            }
        }

        // Function to format date
        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: "Invalid date"
            } catch (e: Exception) {
                "Invalid date"
            }
        }

        private fun showCompletionDialog(context: Context, taskId: String) {
            AlertDialog.Builder(context)
                .setTitle("🎉 Congratulations!")
                .setMessage("You have successfully completed this task! Would you like to remove it?")
                .setPositiveButton("Yes, Remove") { _, _ ->
                    onRemoveTask(taskId) // Call remove function
                    updateStreak() // Update streak in Firebase
                }
                .setNegativeButton("No, Keep it") { _, _ ->
                    updateStreak() // Update streak even if task is not removed
                }
                .show()
        }

        private fun updateStreak() {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val database = FirebaseDatabase.getInstance().getReference("streaks").child(userId)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            database.get().addOnSuccessListener { snapshot ->
                val streak = snapshot.getValue(Streak::class.java)

                if (streak != null) {
                    if (streak.lastCompleted == today) {
                        // Task already completed today, no need to update streak
                        return@addOnSuccessListener
                    }
                    val yesterday = Calendar.getInstance().apply {
                        add(Calendar.DATE, -1)
                    }.time.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                    }

                    // If lastCompleted was yesterday, increase streak, else reset to 1
                    streak.streakCount = if (streak.lastCompleted == yesterday) streak.streakCount + 1 else 1
                    streak.lastCompleted = today
                } else {
                    // First-time streak entry
                    database.setValue(Streak(userId, 1, today))
                    return@addOnSuccessListener
                }

                database.setValue(streak)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.taskId == newItem.taskId
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }
}
