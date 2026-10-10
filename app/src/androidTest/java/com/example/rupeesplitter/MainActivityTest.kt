package com.example.rupeesplitter

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.IntentCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.R as MaterialR
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for the single screen. Run with `./gradlew connectedDebugAndroidTest`. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule(order = 0)
    val activity = ActivityScenarioRule(MainActivity::class.java)

    // Starts after the activity is up: recording intents during launch stalls ActivityScenario.
    @get:Rule(order = 1)
    val intents = IntentsRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val themePrefs = instrumentation.targetContext
        .getSharedPreferences("rupee_splitter_settings", Context.MODE_PRIVATE)

    @Before
    fun stubExternalApps() {
        intending(not(isInternal())).respondWith(ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun forgetThemeChoice() {
        themePrefs.edit().clear().commit()
        instrumentation.runOnMainSync {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    @Test
    fun launchesShowingTheEmptyState() {
        onView(withId(R.id.amountInput)).check(matches(isDisplayed()))
        onView(withText(R.string.empty_state_title)).check(matches(isDisplayed()))
    }

    @Test
    fun typingAnAmountShowsTheBreakdown() {
        enterAmount("10000")

        onView(withText(R.string.summary_total_label)).check(matches(isDisplayed()))
        onView(withText("Portion 5")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Portion 6")).perform(scrollTo())
        onView(visibleFinalTag()).check(matches(isDisplayed()))
    }

    @Test
    fun exactMultipleHasNoRemainderRow() {
        enterAmount("3998")

        onView(withText("Portion 2")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Portion 3")).check(doesNotExist())
        onView(visibleFinalTag()).check(doesNotExist())
    }

    @Test
    fun largeAmountShowsTheCompactView() {
        enterAmount("10000000")

        onView(withText(containsString("compact view"))).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Portion 1")).check(doesNotExist())
    }

    @Test
    fun largeExactMultipleShowsTheCompactViewWithoutARemainderRow() {
        enterAmount("201899")

        onView(withText(containsString("compact view"))).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Portion 1")).check(doesNotExist())
    }

    @Test
    fun quickAmountChipFillsTheInput() {
        onView(withText("₹25,000")).perform(click())

        onView(withId(R.id.amountInput)).check(matches(withText("25,000")))
    }

    @Test
    fun splitButtonFormatsTheAmount() {
        enterAmount("10000")

        onView(withId(R.id.calculateButton)).perform(click())
        onView(withId(R.id.amountInput)).check(matches(withText("10,000")))

        onView(withId(R.id.calculateButton)).perform(click())
        onView(withId(R.id.amountInput)).check(matches(withText("10,000")))
    }

    @Test
    fun keyboardDoneActionSubmits() {
        onView(withId(R.id.amountInput)).perform(replaceText("25000"), pressImeActionButton())

        onView(withId(R.id.amountInput)).check(matches(withText("25,000")))
    }

    @Test
    fun submittingNothingShowsAnError() {
        onView(withId(R.id.calculateButton)).perform(click())

        onView(withId(MaterialR.id.textinput_error)).check(matches(withText(R.string.error_empty_amount)))
    }

    @Test
    fun submittingAnInvalidAmountShowsAnErrorUntilItIsCorrected() {
        enterAmount("1.234")
        onView(withId(R.id.calculateButton)).perform(click())
        onView(withId(MaterialR.id.textinput_error)).check(matches(withText(R.string.error_invalid_amount)))

        enterAmount("1.23")
        onView(withText(R.string.summary_total_label)).check(matches(isDisplayed()))
    }

    @Test
    fun submittingZeroExplainsThatTheAmountIsZero() {
        enterAmount("0")
        onView(withText(R.string.empty_state_title)).check(matches(isDisplayed()))

        onView(withId(R.id.calculateButton)).perform(click())

        onView(withText(R.string.zero_state_title)).check(matches(isDisplayed()))
    }

    @Test
    fun resetReturnsToTheEmptyState() {
        enterAmount("10000")
        onView(withId(R.id.resetButton)).perform(click())

        onView(withId(R.id.amountInput)).check(matches(withText("")))
        onView(withText(R.string.empty_state_title)).check(matches(isDisplayed()))
    }

    @Test
    fun amountAndResultSurviveRecreation() {
        enterAmount("10000")

        activity.scenario.recreate()

        onView(withId(R.id.amountInput)).check(matches(withText("10000")))
        onView(withText("Portion 5")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun copyPutsTheBreakdownOnTheClipboard() {
        enterAmount("10000")

        onView(withId(R.id.copyButton)).perform(scrollTo(), click())

        var copied = ""
        activity.scenario.onActivity {
            val clipboard = it.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            copied = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        }
        assertTrue(copied, copied.contains("Total Amount: ₹10,000"))
    }

    @Test
    fun shareOffersTheBreakdownAsPlainText() {
        enterAmount("10000")

        onView(withId(R.id.shareButton)).perform(scrollTo(), click())

        val chooser = Intents.getIntents().single { it.action == Intent.ACTION_CHOOSER }
        val shared = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)
        assertEquals(Intent.ACTION_SEND, shared?.action)
        assertEquals("text/plain", shared?.type)
        assertTrue(shared?.getStringExtra(Intent.EXTRA_TEXT).orEmpty().contains("Total Amount: ₹10,000"))
    }

    @Test
    fun themeToggleSwitchesThemeBothWays() {
        // With no saved choice the app starts in the device's own theme.
        themePrefs.edit().clear().commit()
        activity.scenario.recreate()
        var startedDark = false
        activity.scenario.onActivity {
            val nightMode = it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            startedDark = nightMode == Configuration.UI_MODE_NIGHT_YES
        }

        onView(withId(R.id.themeToggle)).check(matches(withContentDescription(switchLabel(toDark = !startedDark))))

        onView(withId(R.id.themeToggle)).perform(click())
        onView(withId(R.id.themeToggle)).check(matches(withContentDescription(switchLabel(toDark = startedDark))))
        assertEquals(!startedDark, themePrefs.getBoolean("theme_is_dark", startedDark))

        onView(withId(R.id.themeToggle)).perform(click())
        onView(withId(R.id.themeToggle)).check(matches(withContentDescription(switchLabel(toDark = !startedDark))))
    }

    private fun enterAmount(amount: String) {
        onView(withId(R.id.amountInput)).perform(scrollTo(), replaceText(amount), closeSoftKeyboard())
    }

    private fun visibleFinalTag() = allOf(withText(R.string.tag_final), isDisplayed())

    private fun switchLabel(toDark: Boolean): Int =
        if (toDark) R.string.a11y_theme_switch_to_dark else R.string.a11y_theme_switch_to_light
}
