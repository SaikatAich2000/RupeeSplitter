package com.example.rupeesplitter

import java.math.BigInteger

/** Creates the local clipboard/share representation without depending on Android UI classes. */
object BreakdownTextFormatter {
    fun format(result: SplitResult, detailedRowLimit: BigInteger): String {
        require(detailedRowLimit > BigInteger.ZERO) { "Detailed row limit must be positive." }

        return buildString {
            appendLine("Rupee Splitter")
            appendLine()
            appendLine("Total Amount: ${RupeeFormatter.money(result.originalPaise)}")
            appendLine()

            if (result.totalParts <= detailedRowLimit) {
                appendDetailedPayments(result)
            } else {
                appendLine("₹1,999 Portions: ${RupeeFormatter.count(result.fullPortions)}")
                appendLine("Remaining: ${RupeeFormatter.money(result.remainderPaise)}")
                appendLine("Detailed rows are compacted for large results.")
            }

            appendLine()
            appendLine("Total Parts: ${RupeeFormatter.count(result.totalParts)}")
            append("Calculated Total: ${RupeeFormatter.money(result.calculatedTotalPaise)}")
        }
    }

    private fun StringBuilder.appendDetailedPayments(result: SplitResult) {
        var paymentNumber = BigInteger.ONE
        repeat(result.fullPortions.toInt()) {
            appendLine("Payment ${RupeeFormatter.count(paymentNumber)}: ₹1,999")
            paymentNumber += BigInteger.ONE
        }
        if (result.remainderPaise != BigInteger.ZERO) {
            appendLine("Payment ${RupeeFormatter.count(paymentNumber)}: ${RupeeFormatter.money(result.remainderPaise)}")
        }
    }
}
