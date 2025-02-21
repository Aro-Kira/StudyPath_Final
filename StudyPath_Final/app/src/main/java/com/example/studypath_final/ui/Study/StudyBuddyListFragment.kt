package com.example.studypath_final.ui.Study

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.studypath_final.R
import com.example.studypath_final.databinding.FragmentStudyBuddyListBinding
import com.example.studypath_final.databinding.ItemChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

class StudyBuddyListFragment : Fragment() {
    private var _binding: FragmentStudyBuddyListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ChatAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var notificationListener: ListenerRegistration? = null
    private var chatsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyBuddyListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        setupRecyclerView()
        setupClickListeners()
        loadChats()
        setupNotificationListener()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter { chatItem ->
            val intent = Intent(requireContext(), ActivityStudyBuddyChat::class.java).apply {
                putExtra("CHAT_ID", chatItem.chatId)
                putExtra("BUDDY_ID", chatItem.buddyId)
                putExtra("BUDDY_NAME", chatItem.name)
            }
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StudyBuddyListFragment.adapter
        }
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        chatsListener = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading chats: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chatItems = mutableListOf<ChatItem>()

                    for (doc in snapshot.documents) {
                        val chatId = doc.id
                        val participants = doc.get("participants") as? List<String> ?: continue
                        val buddyId = participants.find { it != currentUserId } ?: continue
                        val lastMessage = doc.getString("lastMessage") ?: ""

                        // Fetch buddy name from Firestore
                        firestore.collection("users").document(buddyId).get()
                            .addOnSuccessListener { userDoc ->
                                val buddyName = userDoc.getString("username") ?: "Unknown"

                                // Fetch profile image from Realtime Database
                                val databaseRef = FirebaseDatabase.getInstance().getReference("users/$buddyId/profileImage")
                                databaseRef.get().addOnSuccessListener { dataSnapshot ->
                                    val profileImageUrl = dataSnapshot.value as? String ?: ""

                                    val chatItem = ChatItem(chatId, buddyId, buddyName, lastMessage, profileImageUrl)
                                    adapter.updateChatItem(chatItem)
                                }.addOnFailureListener {
                                    val chatItem = ChatItem(chatId, buddyId, buddyName, lastMessage, "")
                                    adapter.updateChatItem(chatItem)
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error fetching user data", Toast.LENGTH_SHORT).show()
                            }

                        chatItems.add(ChatItem(chatId, buddyId, "Loading...", lastMessage))
                    }
                    adapter.submitList(chatItems)
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        notificationListener?.remove()
        chatsListener?.remove()
        _binding = null
    }

    private fun setupClickListeners() {
        binding.btnAddStudyBuddy.setOnClickListener {
            val intent = Intent(requireContext(), ActivityStudyBuddySearch::class.java)
            startActivity(intent)
        }
    }

    private fun setupNotificationListener() {
        val currentUserId = auth.currentUser?.uid ?: return

        notificationListener = firestore.collection("users").document(currentUserId)
            .collection("notifications")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error listening for notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val chatId = doc.getString("chatId") ?: continue
                        val senderId = doc.getString("senderId") ?: continue
                        val content = doc.getString("content") ?: continue

                        // Update the chat item with the new message
                        adapter.updateChatItem(ChatItem(chatId, senderId, "", content))

                        // Remove the notification after processing
                        doc.reference.delete()
                    }
                }
            }
    }
}

data class ChatItem(
    val chatId: String,
    val buddyId: String,
    val name: String,
    val lastMessage: String,
    var profileImageUrl: String = "" // New field for the profile image
)

class ChatAdapter(
    private val onItemClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var items = mutableListOf<ChatItem>()

    fun submitList(newItems: List<ChatItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateChatItem(updatedItem: ChatItem) {
        val index = items.indexOfFirst { it.chatId == updatedItem.chatId }
        if (index != -1) {
            items[index] = updatedItem
            notifyItemChanged(index)
        } else {
            items.add(0, updatedItem)
            notifyItemInserted(0)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        return ChatViewHolder(
            ItemChatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ChatViewHolder(
        private val binding: ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                onItemClick(items[adapterPosition])
            }
        }

        fun bind(item: ChatItem) {
            binding.tvName.text = item.name
            binding.tvLastMessage.text = item.lastMessage

            // Load profile image with Glide
            Glide.with(binding.root.context)
                .load(item.profileImageUrl)
                .placeholder(R.drawable.default_avatar) // Replace with a default image
                .error(R.drawable.default_avatar) // Replace with a default image
                .into(binding.ivProfile) // Assuming your ImageView ID is ivProfile
        }
    }
}
