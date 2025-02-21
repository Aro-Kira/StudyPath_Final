package com.example.studypath_final.ui.Achievement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.studypath_final.R
import com.example.studypath_final.databinding.FragmentAchievementBinding
import com.google.android.material.tabs.TabLayout

class AchievementFragment : Fragment() {

    private var _binding: FragmentAchievementBinding? = null
    private val binding get() = _binding!!

    private lateinit var badgesContainer: LinearLayout
    private lateinit var quotesContainer: LinearLayout
    private lateinit var badgesCounter: TextView
    private lateinit var quotesCounter: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupTabLayout()
        loadInitialContent()
    }

    private fun setupViews() {
        badgesContainer = (binding.viewSwitcher.getChildAt(0) as ViewGroup).findViewById(R.id.itemsContainer)
        quotesContainer = (binding.viewSwitcher.getChildAt(1) as ViewGroup).findViewById(R.id.itemsContainer)
        badgesCounter = (binding.viewSwitcher.getChildAt(0) as ViewGroup).findViewById(R.id.tvCounter)
        quotesCounter = (binding.viewSwitcher.getChildAt(1) as ViewGroup).findViewById(R.id.tvCounter)
    }
    // Setting up the Badges and Quotes Tabs
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.viewSwitcher.displayedChild = 0 // Show Badges
                        if (badgesContainer.childCount == 0) {
                            populateBadges()
                        }
                    }
                    1 -> {
                        binding.viewSwitcher.displayedChild = 1 // Show Quotes
                        if (quotesContainer.childCount == 0) {
                            populateQuotes()
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadInitialContent() {
        // Load badges by default
        populateBadges()
    }

    // ‼️‼️‼️📛 Content of the Badges Tab 📛‼️‼️‼️
    private fun populateBadges() {
        badgesCounter.text = "2/10 Badges Unlocked"
        val badges = listOf(
            Badge("100-hour Club", "Accumulate 100 hours of total study time.", "Earned 1/1/25", R.drawable.taskmaster2),
            Badge("Streak Master", "Hit a 30-day streak without skipping a day.", "Earned 1/2/25", R.drawable.taskmaster),
            Badge("Pomodoro Pro", "Complete 50 pomodoro task cycles.", "Not earned yet", R.drawable.query),
            Badge("StudyBuddy Champ", "Maintain a StudyBuddy streak for 10 consecutive days.", "Not earned yet", R.drawable.query),
            Badge("Goal Getter", "Achieve a daily goal 7 days in a row.", "Not earned yet", R.drawable.query),
            Badge("Task Slayer", "Complete 50 tasks in your checklist.", "Not earned yet", R.drawable.query),
            Badge("Deadline Dodger", "Change a task's deadline 3 times.", "Not earned yet", R.drawable.query),
            Badge("Checklist Conqueror", "Mark 10 checklists as fully completed.", "Not earned yet", R.drawable.query),
            Badge("Reminder King/Queen", "Receive 30 notifications for study reminders.", "Not earned yet", R.drawable.query),
            Badge("Social Scholar", "Add 10 friends on StudyPaths.", "Not earned yet", R.drawable.query)
        )

        for (badge in badges) {
            val badgeView = layoutInflater.inflate(R.layout.item_badge, badgesContainer, false)
            badgeView.findViewById<TextView>(R.id.tvBadgeTitle).text = badge.title
            badgeView.findViewById<TextView>(R.id.tvBadgeDescription).text = badge.description
            badgeView.findViewById<TextView>(R.id.tvBadgeEarned).text = "${badge.earnedDate}"
            badgeView.findViewById<ImageView>(R.id.ivBadge).setImageResource(badge.iconResId)
            badgesContainer.addView(badgeView)
        }
    }

    //‼️‼️‼️💬 Content of the Quotes Tab 💬‼️‼️‼️
    private fun populateQuotes() {
        quotesCounter.text = "3/10 Quotes Unlocked"
        val quotes = listOf(
            Quote("'Fly high butterfly'", "Miss your Study Buddy Pair Streak.", "Earned 1/4/25"),
            Quote("'Stairs to heaven'", "Change a task's deadline to a year later.", "Earned 1/7/25"),
            Quote("'Every step counts'", "Complete your first task.", "Earned 1/7/25"),
            Quote("'One day at a time'", "Keep a 3-day streak.", "Not earned yet"),
            Quote("'Great things take time'", "Achieve a 10-day streak.", "Not earned yet"),
            Quote("'Lists are life'", "Complete your first checklist.", "Not earned yet"),
            Quote("'Better late than never'", "Finish a task after the deadline.", "Not earned yet"),
            Quote("'Friends make studying fun'", "Add your first friend.", "Not earned yet"),
            Quote("'Time is gold'", "Use the Pomodoro timer 20 times.", "Not earned yet"),
            Quote("'Hard work pays off'", "Earn 5 badges.", "Not earned yet")
        )

        for (quote in quotes) {
            val quoteView = layoutInflater.inflate(R.layout.item_quote, quotesContainer, false)
            quoteView.findViewById<TextView>(R.id.tvQuoteText).text = quote.text
            quoteView.findViewById<TextView>(R.id.tvQuoteDescription).text = quote.description
            quoteView.findViewById<TextView>(R.id.tvQuoteEarned).text = "${quote.earnedDate}"
            quotesContainer.addView(quoteView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Badge(val title: String, val description: String, val earnedDate: String, val iconResId: Int)
    data class Quote(val text: String, val description: String, val earnedDate: String)
}