package com.example.studypath_final.ui.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.example.studypath_final.ChecklistItem
import com.example.studypath_final.R

class ChecklistAdapter(
    private val checklist: MutableList<ChecklistItem>,
    private var isEditable: Boolean = false // Control if the items are in editable mode
) : RecyclerView.Adapter<ChecklistAdapter.ChecklistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChecklistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.checklist_item, parent, false)
        return ChecklistViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChecklistViewHolder, position: Int) {
        val item = checklist[position]
        holder.checkBox.isChecked = item.isChecked
        holder.editText.setText(item.text)

        // Set checkbox clickable depending on the mode
        holder.checkBox.isClickable = true

        // Make the EditText unfocusable when not in editable mode
        holder.editText.isFocusableInTouchMode = isEditable
        holder.editText.isFocusable = isEditable
        holder.editText.isClickable = false

        // Show or hide the delete button based on isEditable flag
        holder.deleteButton.visibility = if (isEditable) View.VISIBLE else View.GONE

        // Update checklist data when checkbox or text is changed
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            checklist[position] = checklist[position].copy(isChecked = isChecked)
        }

        holder.editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                checklist[position] = checklist[position].copy(text = holder.editText.text.toString())
            }
        }
    }

    override fun getItemCount() = checklist.size

    inner class ChecklistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checklist_checkbox)
        val editText: EditText = view.findViewById(R.id.checklist_edit_text)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_checklist_item)
    }

    // Return the modified checklist
    fun getUpdatedChecklist(): List<ChecklistItem> {
        return checklist.toList()
    }

    // Method to set the editable mode
    fun setEditableMode(editable: Boolean) {
        isEditable = editable
        notifyDataSetChanged() // Notify the adapter to update the UI based on the new mode
    }
}

