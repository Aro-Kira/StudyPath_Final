package com.example.studypath_final.ui.Study

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studypath_final.R
import com.example.studypath_final.databinding.ActivityStudybuddyChatBinding
import com.example.studypath_final.databinding.ItemMessageReceivedBinding
import com.example.studypath_final.databinding.ItemMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class ActivityStudyBuddyChat : AppCompatActivity() {
    private lateinit var binding: ActivityStudybuddyChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var chatId: String
    private lateinit var buddyId: String
    private lateinit var buddyName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudybuddyChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        chatId = intent.getStringExtra("CHAT_ID") ?: return
        buddyId = intent.getStringExtra("BUDDY_ID") ?: return
        buddyName = intent.getStringExtra("BUDDY_NAME") ?: "Buddy"

        setupRecyclerView()
        setupClickListeners()
        setupUI()
        loadMessages()
    }

    private fun setupUI() {
        binding.tvName.text = buddyName

        // Fetch buddy's profile image from Firebase Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("users/$buddyId/profileImage")
        databaseRef.get().addOnSuccessListener { dataSnapshot ->
            val profileImageUrl = dataSnapshot.value as? String

            // Load profile image using Glide
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.default_avatar) // Placeholder image if no profile pic
                .error(R.drawable.default_avatar) // Fallback in case of error
                .into(binding.ivProfile2) // ivProfile2 is the ImageView ID
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load profile image", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupRecyclerView() {
        adapter = MessageAdapter(auth.currentUser?.uid ?: "")

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ActivityStudyBuddyChat).apply {
                stackFromEnd = true
            }
            adapter = this@ActivityStudyBuddyChat.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSend.setOnClickListener {
            val messageContent = binding.etMessage.text.toString().trim()
            if (messageContent.isNotEmpty()) {
                sendMessage(messageContent)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun loadMessages() {
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading messages: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val message = doc.toObject(Message::class.java) ?: return@mapNotNull null

                        // Fetch sender's profile image from Firebase Realtime Database
                        val databaseRef = FirebaseDatabase.getInstance().getReference("users/${message.senderId}/profileImage")
                        databaseRef.get().addOnSuccessListener { dataSnapshot ->
                            val profileImageUrl = dataSnapshot.value as? String ?: ""
                            message.profileImageUrl = profileImageUrl
                            adapter.updateMessage(message) // Update adapter dynamically
                        }

                        message
                    }
                    adapter.submitList(messages)
                    binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
    }


    private fun sendMessage(content: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val message = Message(content, currentUserId, Date())

        firestore.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener { documentReference ->
                // Update last message in chat document
                firestore.collection("chats").document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to content,
                            "lastMessageTimestamp" to message.timestamp
                        )
                    )
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating chat: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                // Add notification for the other user
                firestore.collection("users").document(buddyId)
                    .collection("notifications")
                    .add(
                        mapOf(
                            "type" to "new_message",
                            "chatId" to chatId,
                            "senderId" to currentUserId,
                            "messageId" to documentReference.id,
                            "content" to content,
                            "timestamp" to message.timestamp
                        )
                    )
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error sending notification: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error sending message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

data class Message(
    val content: String = "",
    val senderId: String = "",
    val timestamp: Date = Date(),
    var profileImageUrl: String = "" // New field for profile image
)


class MessageAdapter(private val currentUserId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var messages = mutableListOf<Message>()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun submitList(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun updateMessage(updatedMessage: Message) {
        val index = messages.indexOfFirst { it.timestamp == updatedMessage.timestamp }
        if (index != -1) {
            messages[index] = updatedMessage
            notifyItemChanged(index)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> SentMessageViewHolder(
                ItemMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> ReceivedMessageViewHolder(
                ItemMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    class SentMessageViewHolder(
        private val binding: ItemMessageSentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
        }
    }

    class ReceivedMessageViewHolder(
        private val binding: ItemMessageReceivedBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.content
        }
    }
}
