package com.example.studypath_final

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.slider.Slider

class PomodoroActivity : AppCompatActivity() {

    private lateinit var timerText: TextView
    private lateinit var timerSlider: Slider
    private lateinit var btnStartStop: Button
    private lateinit var toggleMusic: ToggleButton
    private lateinit var toggleChime: ToggleButton
    private lateinit var toggleVibration: ToggleButton
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var timeInSeconds = 1500 // 25 minutes
    private var timerRunning = false
    private var countDownTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var notificationManager: NotificationManager? = null
    private var originalDndMode: Int = NotificationManager.INTERRUPTION_FILTER_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pomodoro)

        toolbar = findViewById(R.id.toolbar)
        val btnBack: ImageButton = findViewById(R.id.btnBack)

        timerText = findViewById(R.id.timerText)
        timerSlider = findViewById(R.id.timerSlider)
        btnStartStop = findViewById(R.id.btnStartStop)
        toggleMusic = findViewById(R.id.toggleMusic)
        toggleChime = findViewById(R.id.toggleChime)
        toggleVibration = findViewById(R.id.toggleVibration)


        Glide.with(this)
            .load(R.drawable.anime_night_sky_illustration)
            .override(500, 500) // Resize
            .centerCrop() // Crop
            .into(findViewById(R.id.img_src))


        // Initialize Slider
        timerSlider.valueFrom = 0f
        timerSlider.valueTo = 1500f // 25 minutes max
        timerSlider.value = timeInSeconds.toFloat()
        timerSlider.setLabelFormatter { value ->
            val minutes = (value / 60).toInt()
            "$minutes min"
        }
        updateTimerText(timeInSeconds)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        originalDndMode = notificationManager?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL



        btnStartStop.setOnClickListener {
            if (timerRunning) {
                stopTimer()
            } else {
                startTimer()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        timerSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                timeInSeconds = value.toInt()
                updateTimerText(timeInSeconds)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer() {
        enterFocusMode()
        countDownTimer = object : CountDownTimer((timeInSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeInSeconds = (millisUntilFinished / 1000).toInt()
                timerSlider.value = timeInSeconds.toFloat()
                updateTimerText(timeInSeconds)
                if (toggleVibration.isChecked) {
                    timerText.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }

            }



            override fun onFinish() {
                stopTimer()
                playChime()
                vibrate()
            }
        }.start()

        timerRunning = true
        btnStartStop.text = "Exit Focus"
    }

    @SuppressLint("SetTextI18n")
    private fun stopTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        btnStartStop.text = "Enter Focus"
        exitFocusMode()
    }

    @SuppressLint("DefaultLocale")
    private fun updateTimerText(seconds: Int) {
        val minutes = seconds / 60
        val secs = seconds % 60
        timerText.text = String.format("%02d:%02d", minutes, secs)
    }

    private fun playChime() {
        if (toggleChime.isChecked) {
            mediaPlayer = MediaPlayer.create(this, R.raw.chime)
            mediaPlayer?.start()
        }
    }

    private fun vibrate() {
        if (toggleVibration.isChecked) {
            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator?.vibrate(500)
        }
    }

    private fun enterFocusMode() {
        val imgSrc: ImageView = findViewById(R.id.img_src)
        val timerText: TextView = findViewById(R.id.timerText)

        toolbar.visibility = View.GONE
        timerSlider.visibility = View.GONE
        imgSrc.visibility = View.GONE

        // Make the timer text larger
        timerText.textSize = 64f

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Load background using Glide
        val bgImageView: ImageView = findViewById(R.id.bgImage)
        bgImageView.visibility = View.VISIBLE
        Glide.with(this)
            .load(R.drawable.anime_night_sky_illustration)
            .into(bgImageView)

        if (notificationManager != null && notificationManager!!.isNotificationPolicyAccessGranted) {
            originalDndMode = notificationManager!!.currentInterruptionFilter
            notificationManager!!.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    private fun exitFocusMode() {
        val imgSrc: ImageView = findViewById(R.id.img_src)
        val timerText: TextView = findViewById(R.id.timerText)

        toolbar.visibility = View.VISIBLE
        timerSlider.visibility = View.VISIBLE
        imgSrc.visibility = View.VISIBLE

        // Restore original timer text size
        timerText.textSize = 48f

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Restore original background
        val bgImageView: ImageView = findViewById(R.id.bgImage)
        bgImageView.visibility = View.GONE

        notificationManager?.setInterruptionFilter(originalDndMode)
    }




    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }



}