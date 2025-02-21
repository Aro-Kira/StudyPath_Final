package com.example.studypath_final.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.studypath_final.PomodoroActivity
import com.example.studypath_final.Task
import com.example.studypath_final.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var tasksRef: DatabaseReference
    private lateinit var streakRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var streakListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        val userId = auth.currentUser?.uid
        if (userId != null) {
            tasksRef = database.getReference("tasks")
            streakRef = database.getReference("streaks").child(userId)

            // Fetch and display streak
            fetchStreak()
        }

        val calendarView = binding.calendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = GregorianCalendar(year, month, dayOfMonth).timeInMillis
            getTasksForDate(selectedDate) { tasks ->
                markEventsOnCalendar(tasks)
            }
        }

        val pomodoroButton = binding.pomodoroButton
        pomodoroButton.setOnClickListener {
            val intent = Intent(requireContext(), PomodoroActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        streakListener?.let { streakRef.removeEventListener(it) }
        _binding = null
    }

    // Function to fetch and update the streak label
    private fun fetchStreak() {
        streakListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (_binding != null) { // Ensure binding is still valid
                    val streak = snapshot.child("streakCount").getValue(Int::class.java) ?: 0
                    _binding?.streakLabel?.text = "Streak: $streak Days"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Failed to fetch streak", Toast.LENGTH_SHORT).show()
                }
            }
        }
        streakRef.addValueEventListener(streakListener!!)
    }

    // Function to fetch tasks from Firebase
    private fun getTasksForDate(dateMillis: Long, callback: (List<Task>) -> Unit) {
        tasksRef.orderByChild("startDate").equalTo(dateMillis.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tasks = mutableListOf<Task>()
                    for (taskSnapshot in snapshot.children) {
                        val task = taskSnapshot.getValue(Task::class.java)
                        task?.let {
                            tasks.add(it)
                        }
                    }
                    callback(tasks)
                }

                override fun onCancelled(error: DatabaseError) {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    // Function to mark events on the calendar
    private fun markEventsOnCalendar(tasks: List<Task>) {
        val calendarView = binding.calendarView
        val eventDates = mutableSetOf<Long>()

        tasks.forEach { task ->
            val startDate = getDateFromString(task.startDate)
            if (startDate != null) {
                val normalizedStartDate = getNormalizedDate(startDate)
                eventDates.add(normalizedStartDate)
            }
        }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = GregorianCalendar(year, month, dayOfMonth).timeInMillis
            val normalizedSelectedDate = getNormalizedDate(Date(selectedDate))

            if (eventDates.contains(normalizedSelectedDate)) {
                Toast.makeText(requireContext(), "Event on this day", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to normalize a Date object (strip time part)
    private fun getNormalizedDate(date: Date): Long {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // Helper function to convert date string to Date object
    private fun getDateFromString(dateString: String): Date? {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return try {
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
