package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.focus.FocusLeaderboardRepository
import com.example.data.focus.WeekKeyUtils
import com.example.data.focus.WeeklyFocusRanking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusLeaderboardRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: FocusLeaderboardRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("studyos_leaderboard_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = FocusLeaderboardRepository(context)
    }

    @Test
    fun testWeekKeyFormat() {
        val key = WeekKeyUtils.getCurrentWeekKey()
        assertTrue(key.matches(Regex("\\d{4}-W\\d{2}")))
        val displayTitle = WeekKeyUtils.getWeekDisplayTitle()
        assertTrue(displayTitle.startsWith("Week "))
    }

    @Test
    fun testWeeklyFocusRankingFormatting() {
        val entry1 = WeeklyFocusRanking(totalFocusMinutes = 780)
        assertEquals("13h 0m", entry1.formattedTime)

        val entry2 = WeeklyFocusRanking(totalFocusMinutes = 45)
        assertEquals("45m", entry2.formattedTime)

        val entry3 = WeeklyFocusRanking(totalFocusMinutes = 125)
        assertEquals("2h 5m", entry3.formattedTime)
    }

    @Test
    fun testLeaderboardRankingComputation() = runBlocking {
        // User with 900 minutes should be #1 (above Maya's 780)
        val rankingsTop = repository.observeWeeklyLeaderboard(
            currentUserId = "test_user_01",
            currentUserName = "Top Student",
            currentWeeklyMinutes = 900,
            currentStreak = 10,
            currentSessions = 20
        ).first()

        assertNotNull(rankingsTop)
        assertTrue(rankingsTop.isNotEmpty())
        val userItem = rankingsTop.first { it.isCurrentUser }
        assertEquals(1, userItem.rank)
        assertEquals("Top Student", userItem.displayName)

        // User with 500 minutes should be ranked between Sofia (540 min, rank 3) and David (450 min, rank 4) -> rank 4
        val rankingsMid = repository.observeWeeklyLeaderboard(
            currentUserId = "test_user_02",
            currentUserName = "Mid Student",
            currentWeeklyMinutes = 500,
            currentStreak = 5,
            currentSessions = 8
        ).first()

        val userMidItem = rankingsMid.first { it.isCurrentUser }
        assertEquals(4, userMidItem.rank)
    }
}
