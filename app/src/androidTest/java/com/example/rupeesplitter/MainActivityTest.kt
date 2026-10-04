package com.example.rupeesplitter

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for the only screen in the app.
 *
 * They run on a device or an emulator:
 *
 *     .\gradlew.bat connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun launchesShowingTheEmptyState() {
        onView(withId(R.id.amountInput)).check(matches(isDisplayed()))
        onView(withText(R.string.empty_state_title)).check(matches(isDisplayed()))
    }

    @Test
    fun typingAnAmountShowsTheBreakdown() {
        onView(withId(R.id.amountInput))
            .perform(replaceText("10000"), closeSoftKeyboard())

        onView(withText(R.string.summary_total_label)).check(matches(isDisplayed()))
        onView(withText(R.string.summary_portions)).check(matches(isDisplayed()))
        onView(withText("Portion 5")).check(matches(isDisplayed()))
    }

    @Test
    fun quickAmountChipFillsTheInput() {
        onView(withText("₹25,000")).perform(click())

        onView(withId(R.id.amountInput)).check(matches(withText("25,000")))
    }

    @Test
    fun resetReturnsToTheEmptyState() {
        onView(withId(R.id.amountInput)).perform(replaceText("10000"), closeSoftKeyboard())
        onView(withId(R.id.resetButton)).perform(click())

        onView(withText(R.string.empty_state_title)).check(matches(isDisplayed()))
    }

    @Test
    fun calculatorReconcilesOnDeviceToo() {
        // The same guarantee the unit tests assert, run in the real device process.
        val result = (SplitCalculator().calculate("10,000.50") as CalculationState.Success).result
        assertEquals(result.originalPaise, result.calculatedTotalPaise)
    }
}
