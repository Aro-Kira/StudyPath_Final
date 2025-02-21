package com.example.studypath_final

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotifActivity : AppCompatActivity() {

    private lateinit var notificationContainer: LinearLayout
    private lateinit var notificationCount: TextView
    private lateinit var dismissAll: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notif)

        notificationContainer = findViewById(R.id.notificationContainer)
        notificationCount = findViewById(R.id.notificationCount)
        dismissAll = findViewById(R.id.dismissAll)
        val btnBack: ImageButton = findViewById(R.id.btnBack)

        updateNotificationCount()

        btnBack.setOnClickListener {
            finish()
        }

        dismissAll.setOnClickListener {
            notificationContainer.removeAllViews()
            updateNotificationCount()
        }
    }

    private fun updateNotificationCount() {
        val count = notificationContainer.childCount
        notificationCount.text = "Notification ($count)"
        dismissAll.visibility = if (count > 0) View.VISIBLE else View.GONE
    }
}
