package com.example.rupeesplitter

import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

/** Pure, offline calculation and formatting logic for the ₹1,999 split. */
class SplitCalculator {
    fun calculate(rawInput: String): CalculationState {
        val cleaned = rawInput.trim().replace(Regex("\\s+"), "")
        val withoutSymbol = if (cleaned.startsWith(RUPEE_SYMBOL)) cleaned.drop(1) else cleaned

        if (withoutSymbol.isEmpty()) return CalculationState.Empty
        if (!isWellFormedAmount(withoutSymbol)) return CalculationState.Invalid

        return try {
            val paise = BigDecimal(withoutSymbol.replace(",", ""))
                .movePointRight(2)
                .toBigIntegerExact()
            when {
                paise.signum() < 0 -> CalculationState.Invalid
                paise == BigInteger.ZERO -> CalculationState.Zero
                else -> {
                    val quotientAndRemainder = paise.divideAndRemainder(CHUNK_PAISE)
                    val fullPortions = quotientAndRemainder[0]
                    val remainder = quotientAndRemainder[1]
                    CalculationState.Success(
                        SplitResult(
                            originalPaise = paise,
                            fullPortions = fullPortions,
                            remainderPaise = remainder,
                            totalParts = fullPortions + if (remainder == BigInteger.ZERO) BigInteger.ZERO else BigInteger.ONE
                        )
                    )
                }
            }
        } catch (_: NumberFormatException) {
            CalculationState.Invalid
        } catch (_: ArithmeticException) {
            CalculationState.Invalid
        }
    }

    companion object {
        const val RUPEE_SYMBOL = "₹"
        val CHUNK_PAISE: BigInteger = BigInteger("199900")
        private val PLAIN_AMOUNT_PATTERN = Regex("^\\d+(?:\\.\\d{1,2})?$")
        // Indian grouping: 10,000; 1,00,000; 1,00,00,000.
        private val INDIAN_GROUPED_AMOUNT_PATTERN = Regex("^\\d{1,3}(?:,\\d{2})*,\\d{3}(?:\\.\\d{1,2})?$")

        private fun isWellFormedAmount(value: String): Boolean {
            if (value.length > 104) return false // prevents pathological pasted input while allowing enormous valid totals
            return PLAIN_AMOUNT_PATTERN.matches(value) || INDIAN_GROUPED_AMOUNT_PATTERN.matches(value)
        }
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
    val calculatedTotalPaise: BigInteger
        get() = (fullPortions * SplitCalculator.CHUNK_PAISE) + remainderPaise
}

object RupeeFormatter {
    private val indianLocale = Locale.Builder().setLanguage("en").setRegion("IN").build()

    private fun formatter(): NumberFormat = NumberFormat.getNumberInstance(indianLocale).apply {
        isGroupingUsed = true
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    fun money(paise: BigInteger): String = SplitCalculator.RUPEE_SYMBOL + formatter().format(BigDecimal(paise, 2))

    fun count(value: BigInteger): String = NumberFormat.getIntegerInstance(indianLocale).format(value)
}
