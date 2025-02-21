package com.example.studypath_final.data.repository

import com.example.studypath_final.data.model.User
import com.example.studypath_final.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val realtimeDatabase = FirebaseDatabase.getInstance().reference

    fun signupUser(username: String, email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser!!.uid
                    val user = User(username = username, email = email, password = password, id = userId, profileImage = "")

                    // Store user in Firestore (without password)
                    firestore.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            // Store user in Realtime Database as well
                            realtimeDatabase.child("users").child(userId).setValue(user)
                                .addOnSuccessListener {
                                    realtimeDatabase.child("users").child(userId).child("tasks").setValue(null)
                                    onComplete(true, null)
                                }
                                .addOnFailureListener { e -> onComplete(false, e.message) }
                        }
                        .addOnFailureListener { e -> onComplete(false, e.message) }
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }


    // 🔵 LOGIN User and Sync Realtime Database
    fun loginUser(usernameOrEmail: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        // Check if the input is an email
        if (usernameOrEmail.contains("@")) {
            // If it's an email, use FirebaseAuth's signInWithEmailAndPassword
            auth.signInWithEmailAndPassword(usernameOrEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser!!.uid
                        // Fetch data from Realtime Database after login
                        realtimeDatabase.child("users").child(userId).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val user = snapshot.getValue(User::class.java)
                                    onComplete(true, null)
                                } else {
                                    onComplete(false, "User data not found in Realtime Database")
                                }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false, e.message)
                            }
                    } else {
                        onComplete(false, task.exception?.message)
                    }
                }
        } else {
            // If it's not an email, assume it's a username and first find the email for the username
            firestore.collection("users")
                .whereEqualTo("username", usernameOrEmail)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val userDoc = querySnapshot.documents.first()
                        val email = userDoc.getString("email") ?: ""
                        // Attempt to log in with email
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser!!.uid
                                    // Fetch data from Realtime Database after login
                                    realtimeDatabase.child("users").child(userId).get()
                                        .addOnSuccessListener { snapshot ->
                                            if (snapshot.exists()) {
                                                val user = snapshot.getValue(User::class.java)
                                                onComplete(true, null)
                                            } else {
                                                onComplete(false, "User data not found in Realtime Database")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            onComplete(false, e.message)
                                        }
                                } else {
                                    onComplete(false, task.exception?.message)
                                }
                            }
                    } else {
                        onComplete(false, "Username not found")
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
        }
    }


    // 🔵 ADD Task to User's Task List
    fun addTask(userId: String, task: Task, onComplete: (Boolean, String?) -> Unit) {
        val taskId = realtimeDatabase.child("users").child(userId).child("tasks").push().key
        taskId?.let {
            realtimeDatabase.child("users").child(userId).child("tasks").child(it).setValue(task)
                .addOnSuccessListener {
                    onComplete(true, null)
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.message)
                }
        } ?: onComplete(false, "Failed to generate task ID")
    }

    // 🔵 GET Tasks for a User
    fun getUserTasks(userId: String, onComplete: (List<Task>?, String?) -> Unit) {
        realtimeDatabase.child("users").child(userId).child("tasks").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val tasks = snapshot.children.mapNotNull { it.getValue(Task::class.java) }
                    onComplete(tasks, null)
                } else {
                    onComplete(null, "No tasks found")
                }
            }
            .addOnFailureListener { e ->
                onComplete(null, e.message)
            }
    }

    // 🔵 DELETE Task
    fun deleteTask(userId: String, taskId: String, onComplete: (Boolean, String?) -> Unit) {
        realtimeDatabase.child("users").child(userId).child("tasks").child(taskId).removeValue()
            .addOnSuccessListener {
                onComplete(true, null)
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }
}
