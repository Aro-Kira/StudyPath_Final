package com.example.studypath_final.data.model

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = "", // Avoid storing plain-text passwords (use Firebase Authentication instead)
    val profileImage: String = "", // Add profileImage field
    val id: String = ""
)
