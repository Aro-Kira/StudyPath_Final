package com.example.studypath_final.ui.task

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studypath_final.ChecklistItem
import com.example.studypath_final.databinding.DialogChecklistBinding

class ChecklistDialogFragment(
    private val checklist: MutableList<ChecklistItem>,
    private val onChecklistUpdated: (List<ChecklistItem>) -> Unit
) : DialogFragment() {

    private var _binding: DialogChecklistBinding? = null
    private val binding get() = _binding!!
    private lateinit var checklistAdapter: ChecklistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogChecklistBinding.inflate(inflater, container, false)

        checklistAdapter = ChecklistAdapter(checklist)

        binding.recyclerViewChecklist.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = checklistAdapter
        }

        // Save button updates checklist data and sends it back
        binding.saveChecklistItem.setOnClickListener {
            val updatedChecklist = checklistAdapter.getUpdatedChecklist()
            onChecklistUpdated(updatedChecklist)
            dismiss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
