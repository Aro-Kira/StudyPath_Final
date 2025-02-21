package com.example.studypath_final

import android.content.Intent

import android.os.Bundle

import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.studypath_final.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        val bottomAppBar = findViewById<com.google.android.material.bottomappbar.BottomAppBar>(R.id.bottomAppBar)

        bottomNav.background = null

        // Access header views
        val headerView = navView.getHeaderView(0)
        val notifButton: ImageButton = headerView.findViewById(R.id.notif)
        val settingsButton: ImageButton = headerView.findViewById(R.id.settings)
        val profileButton: ImageView = headerView.findViewById(R.id.profileIcon)
        val usernameTextView: TextView = headerView.findViewById(R.id.username)

        // Fetch user data (username & profile picture)
        fetchUserData(usernameTextView)

        // Setup Navigation
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_task, R.id.nav_achievement),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        bottomNav.setupWithNavController(navController)

        // Bottom Navigation Animation Handling
        bottomNav.setOnItemSelectedListener { item ->
            val currentDestinationId = navController.currentDestination?.id
            val options = when (item.itemId) {
                R.id.nav_home -> NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_left)
                    .setExitAnim(R.anim.slide_out_right)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build()
                R.id.nav_task -> NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_right)
                    .setPopExitAnim(R.anim.slide_out_left)
                    .build()
                else -> null
            }

            options?.let {
                when (item.itemId) {
                    R.id.nav_home -> {
                        if (currentDestinationId != R.id.nav_home) {
                            navController.navigate(R.id.nav_home, null, it)
                        }
                        true
                    }
                    R.id.nav_task -> {
                        if (currentDestinationId != R.id.nav_task) {
                            navController.navigate(R.id.nav_task, null, it)
                        }
                        true
                    }
                    else -> false
                }
            } ?: false
        }

        fab.setOnClickListener {
            val intent = Intent(this, Create_TaskActivity::class.java)
            startActivity(intent)
        }

        // Handle FAB and Bottom Navigation Visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val shouldShow = destination.id == R.id.nav_home || destination.id == R.id.nav_task
            fab.visibility = if (shouldShow) View.VISIBLE else View.GONE
            bottomAppBar.visibility = if (shouldShow) View.VISIBLE else View.GONE
            bottomNav.visibility = if (shouldShow) View.VISIBLE else View.GONE
        }

        // Click Listeners
        notifButton.setOnClickListener {
            val intent = Intent(this, NotifActivity::class.java)
            startActivity(intent)
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun fetchUserData(usernameTextView: TextView) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isDestroyed && !isFinishing) { // ✅ Check if Activity is still alive
                    if (snapshot.exists()) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: "Default Username"
                        val profileImageUrl = snapshot.child("profileImage").getValue(String::class.java)

                        // Update username
                        usernameTextView.text = username

                        // Get NavigationView header
                        val headerView = binding.navView.getHeaderView(0)
                        val profileImageView: ImageView? = headerView.findViewById(R.id.profileIcon)

                        if (profileImageView != null && !profileImageUrl.isNullOrEmpty()) {
                            Glide.with(this@MainActivity) // ✅ Now only runs if activity is alive
                                .load(profileImageUrl)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(profileImageView)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeDB", "Error fetching user data: ${error.message}")
            }
        })
    }




}
