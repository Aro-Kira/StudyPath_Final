package com.example.studypath_final.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studypath_final.databinding.FragmentTaskBinding
import com.example.studypath_final.ui.home.HomeFragment

class TaskFragment : Fragment() {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root



    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)

        taskAdapter = TaskAdapter(
            onTaskClick = { taskId -> openChecklistDialog(taskId) },
            onRemoveTask = { taskId -> taskViewModel.removeTask(taskId) } // Handles deletion
        )

        binding.taskRecyclerView.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        taskViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            taskAdapter.submitList(tasks)
        }
    }

    private fun openChecklistDialog(taskId: String) {
        val selectedTask = taskViewModel.tasks.value?.find { it.taskId == taskId }
        selectedTask?.let { task ->
            val dialog = ChecklistDialogFragment(task.checklist.toMutableList()) { updatedChecklist ->
                taskViewModel.updateChecklist(taskId, updatedChecklist)
            }
            dialog.show(childFragmentManager, "ChecklistDialog")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
