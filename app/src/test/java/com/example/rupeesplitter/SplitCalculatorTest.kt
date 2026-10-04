package com.example.rupeesplitter

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitCalculatorTest {
    private val calculator = SplitCalculator()

    @Test
    fun `1999 is one exact portion`() = assertSplit("1999", "1", "0", "1")

    @Test
    fun `2000 has one rupee remainder`() = assertSplit("2000", "1", "100", "2")

    @Test
    fun `3998 is two exact portions`() = assertSplit("3998", "2", "0", "2")

    @Test
    fun `3999 has two portions and one rupee remainder`() = assertSplit("3999", "2", "100", "3")

    @Test
    fun `10000 has five portions and five rupee remainder`() = assertSplit("10000", "5", "500", "6")

    @Test
    fun `decimal amount retains exact paise`() = assertSplit("10,000.50", "5", "550", "6")

    @Test
    fun `sub rupee amount is kept as exact paise`() = assertSplit("0.01", "0", "1", "1")

    @Test
    fun `valid required Indian formats are accepted`() {
        listOf("1", "5", "100", "1998", "1,00,000", "10,00,000", "1,00,00,000").forEach { input ->
            assertTrue("$input should calculate", calculator.calculate(input) is CalculationState.Success)
        }
    }

    @Test
    fun `optional symbol and incidental spaces are accepted`() {
        assertSplit(" ₹ 10,000.50 ", "5", "550", "6")
    }

    @Test
    fun `crore scale value reconciles exactly`() {
        val result = success("1,00,00,000")
        assertEquals(BigInteger("1000000000"), result.originalPaise)
        assertEquals(BigInteger("5002"), result.fullPortions)
        assertEquals(BigInteger("100200"), result.remainderPaise)
        assertReconciles(result)
    }

    @Test
    fun `very large valid value remains precise and compactable`() {
        val result = success("9999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")
        assertTrue(result.totalParts > BigInteger("100"))
        assertReconciles(result)
    }

    @Test
    fun `zero has deliberate state`() {
        assertTrue(calculator.calculate("0") is CalculationState.Zero)
        assertTrue(calculator.calculate("0.00") is CalculationState.Zero)
    }

    @Test
    fun `shared breakdown contains a clear detailed representation`() {
        val text = BreakdownTextFormatter.format(success("10000"), BigInteger("200"))
        assertTrue(text.contains("Total Amount: ₹10,000"))
        assertTrue(text.contains("Payment 1: ₹1,999"))
        assertTrue(text.contains("Payment 6: ₹5"))
        assertTrue(text.contains("Total Parts: 6"))
        assertTrue(text.contains("Calculated Total: ₹10,000"))
    }

    @Test
    fun `large shared breakdown is compact`() {
        val text = BreakdownTextFormatter.format(success("1,00,00,000"), BigInteger("200"))
        assertTrue(text.contains("₹1,999 Portions: 5,002"))
        assertTrue(text.contains("Detailed rows are compacted for large results."))
        assertFalse(text.contains("Payment 1:"))
    }

    @Test
    fun `malformed and negative inputs never produce a split`() {
        listOf(
            "",
            "   ",
            "abc",
            "-",
            "--",
            "1..2",
            "₹₹₹",
            "-5",
            "1.234",
            "1,,000",
            "1,000,000",
            ",100",
            "100,"
        ).forEach { input ->
            assertFalse("$input should not calculate", calculator.calculate(input) is CalculationState.Success)
        }
    }

    private fun assertSplit(input: String, portions: String, remainderPaise: String, parts: String) {
        val result = success(input)
        assertEquals(BigInteger(portions), result.fullPortions)
        assertEquals(BigInteger(remainderPaise), result.remainderPaise)
        assertEquals(BigInteger(parts), result.totalParts)
        assertReconciles(result)
    }

    private fun success(input: String): SplitResult =
        (calculator.calculate(input) as? CalculationState.Success)?.result
            ?: error("Expected a successful calculation for $input")

    private fun assertReconciles(result: SplitResult) {
        assertEquals(result.originalPaise, result.calculatedTotalPaise)
    }
}
