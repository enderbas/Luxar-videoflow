package com.example.multiplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import org.hamcrest.CoreMatchers.`is`
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsFourVideoTilesAndInitialFocus() {
        composeRule.onNodeWithText("MultiPlayer TV").assertIsDisplayed()
        onView(withContentDescription("Coins")).check(matches(isDisplayed()))
        onView(withContentDescription("Orta World")).check(matches(isDisplayed()))
        onView(withContentDescription("Purple")).check(matches(isDisplayed()))
        onView(withContentDescription("Red Squares")).check(matches(isDisplayed()))
        onView(withTagValue(`is`("video_slot_coins"))).check(matches(hasFocus()))
    }
}
