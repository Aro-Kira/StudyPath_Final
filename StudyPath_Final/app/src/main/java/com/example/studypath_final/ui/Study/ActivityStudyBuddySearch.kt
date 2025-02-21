package com.example.studypath_final.ui.Study

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studypath_final.databinding.ActivityStudybuddySearchBinding
import com.example.studypath_final.databinding.ItemUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ActivityStudyBuddySearch : AppCompatActivity() {
    private lateinit var binding: ActivityStudybuddySearchBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudybuddySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Add this check
        if (auth.currentUser == null) {
            Toast.makeText(this, "You must be logged in to use this feature", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerView()
        setupClickListeners()
        setupSearchListener()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter { user ->
            addStudyBuddy(user)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ActivityStudyBuddySearch)
            adapter = this@ActivityStudyBuddySearch.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    if (it.length >= 3) {
                        searchUsers(it.toString())
                    } else {
                        adapter.submitList(emptyList())
                    }
                }
            }
        })
    }

    private fun searchUsers(query: String) {
        firestore.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + '\uf8ff')
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.apply { id = doc.id }
                }
                adapter.submitList(users)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error searching users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addStudyBuddy(user: User) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.e("AddStudyBuddy", "Current user is null")
            Toast.makeText(this, "You must be logged in to add a study buddy", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == user.id) {
            Toast.makeText(this, "You can't add yourself as a study buddy", Toast.LENGTH_SHORT).show()
            return
        }

        val chatId = getChatId(currentUserId, user.id)

        val chatData = hashMapOf(
            "participants" to listOf(currentUserId, user.id),
            "lastMessage" to "",
            "lastMessageTimestamp" to null
        )

        firestore.collection("chats").document(chatId)
            .set(chatData)
            .addOnSuccessListener {
                Log.d("AddStudyBuddy", "Study buddy added successfully")
                Toast.makeText(this, "Study Buddy added!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AddStudyBuddy", "Error adding study buddy", e)
                Toast.makeText(this, "Failed to add Study Buddy: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }
}

data class User(
    var id: String = "",
    val username: String = "",
    val email: String = ""
)

class UserAdapter(private val onItemClick: (User) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    private var users = listOf<User>()

    fun submitList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder(
            ItemUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClick(users[adapterPosition])
            }
        }

        fun bind(user: User) {
            binding.tvUsername.text = user.username
        }
    }
}