package com.example.rupeesplitter

import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

/** Pure, offline parsing and splitting logic for the ₹1,999 split. */
class SplitCalculator {
    fun calculate(rawInput: String): CalculationState {
        val cleaned = rawInput.replace(WHITESPACE, "").removePrefix(RUPEE_SYMBOL)

        if (cleaned.isEmpty()) return CalculationState.Empty
        if (!isWellFormedAmount(cleaned)) return CalculationState.Invalid

        val paise = BigDecimal(normalizeAmount(cleaned)).movePointRight(2).toBigIntegerExact()
        if (paise.signum() == 0) return CalculationState.Zero

        val (fullPortions, remainder) = paise.divideAndRemainder(CHUNK_PAISE)
        val extraPart = if (remainder.signum() == 0) BigInteger.ZERO else BigInteger.ONE
        return CalculationState.Success(
            SplitResult(
                originalPaise = paise,
                fullPortions = fullPortions,
                remainderPaise = remainder,
                totalParts = fullPortions + extraPart
            )
        )
    }

    companion object {
        const val RUPEE_SYMBOL = "₹"
        val CHUNK_PAISE: BigInteger = BigInteger("199900")

        private const val MAX_INPUT_LENGTH = 104
        private val WHITESPACE = Regex("""\s+""")
        private val PLAIN_AMOUNT_PATTERN = Regex("""^(\d+(?:\.\d{1,2})?|\.\d{1,2})$""")
        // Indian grouping: 10,000; 1,00,000; 1,00,00,000.
        private val INDIAN_GROUPED_AMOUNT_PATTERN = Regex("""^\d{1,3}(?:,\d{2})*,\d{3}(?:\.\d{1,2})?$""")

        private fun normalizeAmount(value: String): String {
            val digits = value.replace(",", "")
            return if (digits.startsWith(".")) "0$digits" else digits
        }

        private fun isWellFormedAmount(value: String): Boolean =
            value.length <= MAX_INPUT_LENGTH &&
                (PLAIN_AMOUNT_PATTERN.matches(value) || INDIAN_GROUPED_AMOUNT_PATTERN.matches(value))
    }
}

sealed interface CalculationState {
    data object Empty : CalculationState
    data object Zero : CalculationState
    data object Invalid : CalculationState
    data class Success(val result: SplitResult) : CalculationState
}

data class SplitResult(
    val originalPaise: BigInteger,
    val fullPortions: BigInteger,
    val remainderPaise: BigInteger,
    val totalParts: BigInteger
) {
    init {
        require(originalPaise.signum() >= 0) { "Original paise cannot be negative." }
        require(fullPortions.signum() >= 0) { "Full portions cannot be negative." }
        require(remainderPaise.signum() >= 0) { "Remainder paise cannot be negative." }
        require(totalParts > BigInteger.ZERO) { "Total parts must be positive." }
    }

    val calculatedTotalPaise: BigInteger
        get() = (fullPortions * SplitCalculator.CHUNK_PAISE) + remainderPaise
}

object RupeeFormatter {
    private val indianLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()

    fun money(paise: BigInteger): String {
        val format = NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        return SplitCalculator.RUPEE_SYMBOL + format.format(BigDecimal(paise, 2))
    }

    fun count(value: BigInteger): String = NumberFormat.getIntegerInstance(indianLocale).format(value)
}
