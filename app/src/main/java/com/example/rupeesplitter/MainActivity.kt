package com.example.rupeesplitter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.rupeesplitter.databinding.ActivityMainBinding
import com.example.rupeesplitter.databinding.ItemPaymentRowBinding
import com.example.rupeesplitter.databinding.ViewDetailRowBinding
import com.example.rupeesplitter.databinding.ViewPlaceholderBinding
import com.example.rupeesplitter.databinding.ViewResultActionsBinding
import com.example.rupeesplitter.databinding.ViewSummaryBinding
import java.math.BigInteger

/**
 * The offline calculator screen. Parsing and splitting are kept outside the UI.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val calculator = SplitCalculator()

    private var lastRendered: Pair<CalculationState, Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarStyle()
        applyWindowInsets()
        refreshThemeToggle()

        setupQuickAmounts()
        setupInput()
        setupActions()
        setupThemeToggle()

        render(showErrors = false, animate = false)
    }

    /**
     * Keeps the status-bar and navigation-bar icons readable in both themes.
     * Done in code so `minSdk 24` devices never see an API-27 attribute.
     */
    private fun applySystemBarStyle() {
        val isNight = isDarkThemeActive()
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isNight
        controller.isAppearanceLightNavigationBars = !isNight
    }

    /** Targeting API 35+ draws edge-to-edge, so the content must keep clear of bars, cutouts and the keyboard. */
    private fun applyWindowInsets() {
        val content = binding.content
        val bottom = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { scroll, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            // The scroll view stops short of the status bar, so scrolled content never slides under its icons.
            scroll.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = bars.left
                topMargin = bars.top
                rightMargin = bars.right
            }
            content.updatePadding(bottom = bottom + maxOf(bars.bottom, ime.bottom))
            windowInsets
        }
    }

    private fun setupInput() {
        binding.amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                render(showErrors = false, animate = true)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.amountInput.setOnEditorActionListener { _, _, _ ->
            submit()
            true
        }

        binding.amountInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) formatInputIfValid()
        }
    }

    private fun setupQuickAmounts() {
        QUICK_AMOUNTS.forEach { value ->
            binding.quickRow.addView(pill(value), pillParams())
        }
    }

    private fun setupThemeToggle() {
        binding.themeToggle.setOnClickListener {
            val dark = !isDarkThemeActive()
            themePrefs.edit(commit = true) { putBoolean(THEME_IS_DARK_KEY, dark) }
            AppCompatDelegate.setDefaultNightMode(
                if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun applySavedTheme() {
        val dark = if (themePrefs.contains(THEME_IS_DARK_KEY)) {
            themePrefs.getBoolean(THEME_IS_DARK_KEY, false)
        } else {
            isDarkThemeActive()
        }
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun refreshThemeToggle() {
        val dark = isDarkThemeActive()
        binding.themeToggle.setImageResource(
            if (dark) R.drawable.ic_theme_sun else R.drawable.ic_theme_moon
        )
        binding.themeToggle.contentDescription = getString(
            if (dark) R.string.a11y_theme_switch_to_light else R.string.a11y_theme_switch_to_dark
        )
    }

    private fun isDarkThemeActive(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private val themePrefs
        get() = getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)

    private fun setupActions() {
        binding.calculateButton.setOnClickListener { submit() }
        binding.resetButton.setOnClickListener {
            binding.amountInput.setText("")
            binding.amountInput.requestFocus()
        }
    }

    private fun pill(value: String): TextView = TextView(this).apply {
        text = getString(R.string.chip_amount_format, SplitCalculator.RUPEE_SYMBOL, value)
        setTextColor(color(R.color.primary))
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(10), dp(16), dp(10))
        minHeight = dp(48)
        isClickable = true
        isFocusable = true
        background = RippleDrawable(
            ColorStateList.valueOf(color(R.color.ripple)),
            rounded(R.color.chip_bg, 20),
            null
        )
        contentDescription = getString(R.string.a11y_quick_amount_button, value)
        setOnClickListener {
            binding.amountInput.setText(value)
            binding.amountInput.setSelection(value.length)
        }
    }

    private fun pillParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(8) }

    // ----- Result rendering -----

    private fun amountText(): String = binding.amountInput.text.toString()

    private fun submit() {
        hideKeyboard()
        formatInputIfValid()
        render(showErrors = true, animate = true)
    }

    private fun render(showErrors: Boolean, animate: Boolean) {
        val state = calculator.calculate(amountText())
        binding.amountLayout.error = validationMessage(state, showErrors)

        // Rebuild only when what is shown would change, not on every keystroke.
        val showZero = showErrors && state is CalculationState.Zero
        val rendered = state to showZero
        if (rendered == lastRendered) return
        lastRendered = rendered

        val content = when {
            state is CalculationState.Success -> buildResult(state.result)
            showZero -> placeholder(R.string.zero_state_title, R.string.zero_state_subtitle)
            else -> placeholder(R.string.empty_state_title, R.string.empty_state_subtitle)
        }
        binding.resultContainer.removeAllViews()
        binding.resultContainer.addView(content)
        if (animate) animateIn(content)
    }

    private fun validationMessage(state: CalculationState, showErrors: Boolean): String? = when {
        !showErrors -> null
        state is CalculationState.Empty -> getString(R.string.error_empty_amount)
        state is CalculationState.Invalid -> getString(R.string.error_invalid_amount)
        else -> null
    }

    private fun placeholder(titleRes: Int, messageRes: Int): View {
        val view = ViewPlaceholderBinding.inflate(layoutInflater, binding.resultContainer, false)
        view.stateIcon.setImageResource(R.drawable.ic_split)
        view.stateIcon.imageTintList = ColorStateList.valueOf(color(R.color.primary))
        view.stateTitle.setText(titleRes)
        view.stateMessage.setText(messageRes)
        return view.root
    }

    private fun buildResult(result: SplitResult): View {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val summary = ViewSummaryBinding.inflate(layoutInflater, container, false)
        summary.heroAmount.text = RupeeFormatter.money(result.originalPaise)
        summary.heroPortions.text = RupeeFormatter.count(result.fullPortions)
        summary.heroRemainder.text = RupeeFormatter.money(result.remainderPaise)
        summary.detailsContainer.addView(
            detailRow(getString(R.string.summary_total_parts), RupeeFormatter.count(result.totalParts))
        )
        summary.detailsContainer.addView(
            detailRow(getString(R.string.summary_calculated_total), RupeeFormatter.money(result.calculatedTotalPaise)),
            topSpacedParams()
        )
        container.addView(summary.root)

        container.addView(sectionTitle(getString(R.string.label_breakdown)))

        val breakdown = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (result.totalParts <= DETAIL_ROW_LIMIT) {
            addDetailedRows(breakdown, result)
        } else {
            addCompactRows(breakdown, result)
        }
        container.addView(breakdown)

        val actions = ViewResultActionsBinding.inflate(layoutInflater, container, false)
        actions.copyButton.setOnClickListener { copyBreakdown(result) }
        actions.shareButton.setOnClickListener { shareBreakdown(result) }
        container.addView(actions.root)

        return container
    }

    private fun addDetailedRows(target: LinearLayout, result: SplitResult) {
        var number = BigInteger.ONE
        repeat(result.fullPortions.coerceAtMost(DETAIL_ROW_LIMIT).toInt()) {
            target.addView(
                paymentRow(
                    getString(R.string.row_portion_label, RupeeFormatter.count(number)),
                    RupeeFormatter.money(SplitCalculator.CHUNK_PAISE),
                    isRemainder = false
                ),
                rowParams()
            )
            number += BigInteger.ONE
        }
        if (result.remainderPaise != BigInteger.ZERO) {
            target.addView(
                paymentRow(
                    getString(R.string.row_portion_label, RupeeFormatter.count(number)),
                    RupeeFormatter.money(result.remainderPaise),
                    isRemainder = true
                ),
                rowParams()
            )
        }
    }

    private fun addCompactRows(target: LinearLayout, result: SplitResult) {
        target.addView(
            detailRow(getString(R.string.summary_portions), RupeeFormatter.count(result.fullPortions)),
            rowParams()
        )
        if (result.remainderPaise != BigInteger.ZERO) {
            target.addView(
                detailRow(getString(R.string.summary_remainder), RupeeFormatter.money(result.remainderPaise)),
                rowParams()
            )
        }
        target.addView(noteText(getString(R.string.breakdown_compact_note, RupeeFormatter.count(DETAIL_ROW_LIMIT))))
    }

    private fun paymentRow(label: String, amount: String, isRemainder: Boolean): View {
        val view = ItemPaymentRowBinding.inflate(layoutInflater, binding.resultContainer, false)
        view.rowTitle.text = label
        view.rowAmount.text = amount
        view.rowTag.visibility = if (isRemainder) View.VISIBLE else View.GONE
        view.root.contentDescription = buildString {
            append(label)
            append(", ")
            append(amount)
            if (isRemainder) append(", ").append(getString(R.string.a11y_final_remainder))
        }
        return view.root
    }

    private fun detailRow(label: String, value: String): View {
        val view = ViewDetailRowBinding.inflate(layoutInflater, binding.resultContainer, false)
        view.detailLabel.text = label
        view.detailValue.text = value
        return view.root
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(color(R.color.text_secondary))
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(20), 0, dp(10))
    }

    private fun noteText(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(color(R.color.text_tertiary))
        textSize = 12f
        setPadding(dp(4), dp(6), dp(4), dp(4))
    }

    private fun animateIn(view: View) {
        view.alpha = 0f
        view.translationY = dp(14).toFloat()
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ANIM_DURATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun rowParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(7) }

    private fun topSpacedParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) }

    // ----- Clipboard, sharing and helpers -----

    private fun copyBreakdown(result: SplitResult) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(getString(R.string.app_name), sharedText(result))
        )
        Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show()
    }

    private fun shareBreakdown(result: SplitResult) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sharedText(result))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
    }

    private fun sharedText(result: SplitResult): String =
        BreakdownTextFormatter.format(result, COPY_DETAIL_LIMIT)

    private fun formatInputIfValid() {
        val success = calculator.calculate(amountText()) as? CalculationState.Success ?: return
        val formatted = RupeeFormatter.money(success.result.originalPaise)
            .removePrefix(SplitCalculator.RUPEE_SYMBOL)
        if (amountText() != formatted) {
            binding.amountInput.setText(formatted)
            binding.amountInput.setSelection(formatted.length)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.amountInput.windowToken, 0)
        binding.amountInput.clearFocus()
    }

    private fun rounded(colorRes: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color(colorRes))
        cornerRadius = dp(radius).toFloat()
    }

    private fun color(res: Int): Int = ContextCompat.getColor(this, res)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
        const val ANIM_DURATION_MS = 250L
        const val THEME_PREFS_NAME = "rupee_splitter_settings"
        const val THEME_IS_DARK_KEY = "theme_is_dark"
        val DETAIL_ROW_LIMIT: BigInteger = BigInteger("100")
        val COPY_DETAIL_LIMIT: BigInteger = BigInteger("200")
        val QUICK_AMOUNTS = listOf("1,999", "5,000", "10,000", "25,000", "50,000", "1,00,000")
    }
}
