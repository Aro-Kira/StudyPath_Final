package com.example.studypath_final

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.studypath_final.databinding.ActivityLoginBinding
import com.example.studypath_final.databinding.BottomSheetSignupBinding
import com.example.studypath_final.databinding.BottomSheetLoginBinding
import com.example.studypath_final.ui.viewmodel.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

class ActivityLogin : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var signupSheetBinding: BottomSheetSignupBinding
    private lateinit var loginSheetBinding: BottomSheetLoginBinding
    private lateinit var signupSheetBehavior: BottomSheetBehavior<View>
    private lateinit var loginSheetBehavior: BottomSheetBehavior<View>

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        signupSheetBinding = BottomSheetSignupBinding.bind(binding.bottomSheetSignup.root)
        loginSheetBinding = BottomSheetLoginBinding.bind(binding.bottomSheetLogin.root)

        signupSheetBehavior = BottomSheetBehavior.from(signupSheetBinding.root)
        loginSheetBehavior = BottomSheetBehavior.from(loginSheetBinding.root)

        signupSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        loginSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        // 🔵 Handle SignUp Button Click
        binding.btnSignUp.setOnClickListener {
            signupSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.btnLogin.isEnabled = false // Disable login button
        }

        // 🔵 Handle Login Button Click
        binding.btnLogin.setOnClickListener {
            loginSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.btnSignUp.isEnabled = false // Disable sign-up button
        }

        // 🔵 Handle Back Button on SignUp Sheet
        signupSheetBinding.btnBackSignup.setOnClickListener {
            signupSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            binding.btnLogin.isEnabled = true // Enable login button when signup sheet is hidden
        }

        // 🔵 Handle Back Button on Login Sheet
        loginSheetBinding.btnBackLogin.setOnClickListener {
            loginSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            binding.btnSignUp.isEnabled = true // Enable sign-up button when login sheet is hidden
        }

        // 🔵 Handle Signup Click
        signupSheetBinding.btnCreateAccount.setOnClickListener {
            val username = signupSheetBinding.etSignupUsername.text.toString().trim()
            val email = signupSheetBinding.etSignupEmail.text.toString().trim()
            val password = signupSheetBinding.etSignupPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.signupUser(username, email, password) { success, message ->
                if (success) {
                    Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                    signupSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    binding.btnSignUp.isEnabled = true // Re-enable sign-up button
                    startActivity(Intent(this, ActivityLogin::class.java)) // Restart activity to return to start
                    finish() // Optionally finish the current activity
                } else {
                    Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 🔵 Handle Login Click
        loginSheetBinding.btnLogin.setOnClickListener {
            val username = loginSheetBinding.etLoginUsername.text.toString().trim()
            val password = loginSheetBinding.etLoginPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.loginUser(username, password) { success, message ->
                if (success) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
