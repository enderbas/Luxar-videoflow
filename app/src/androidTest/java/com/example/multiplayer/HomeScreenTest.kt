package com.example.multiplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsFourVideoTilesAndInitialFocus() {
        composeRule.onNodeWithText("MultiPlayer TV").assertIsDisplayed()
        composeRule.onNodeWithText("Coins").assertIsDisplayed()
        composeRule.onNodeWithText("Orta World").assertIsDisplayed()
        composeRule.onNodeWithText("Purple").assertIsDisplayed()
        composeRule.onNodeWithText("Red Squares").assertIsDisplayed()
        composeRule.onNodeWithTag("video_diagnostics_coins").assertIsDisplayed()
        composeRule.onNodeWithTag("video_tile_coins").assertIsFocused()
    }
}
