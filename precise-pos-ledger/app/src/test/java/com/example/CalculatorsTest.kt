package com.example

import com.example.domain.DayCloseCalculator
import com.example.domain.SaleCalculator
import com.example.domain.StockCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsTest {

    @Test
    fun testSaleCalculations() {
        // Example from prompt:
        // Shawarma: 2 * 160 = 320
        // Dew Glass: 1 * 60 = 60
        // Fries: 1 * 100 = 100
        // Subtotal = 480
        val line1 = SaleCalculator.calculateLineTotal(2, 160)
        val line2 = SaleCalculator.calculateLineTotal(1, 60)
        val line3 = SaleCalculator.calculateLineTotal(1, 100)
        assertEquals(320, line1)
        assertEquals(60, line2)
        assertEquals(100, line3)

        val subtotal = SaleCalculator.calculateSubtotal(listOf(line1, line2, line3))
        assertEquals(480, subtotal)

        // 10% discount: 480 * 0.10 = 48
        val discount = SaleCalculator.calculateDiscount(subtotal, discountPercentage = 10)
        assertEquals(48, discount)

        val total = SaleCalculator.calculateTotal(subtotal, discount)
        assertEquals(432, total)

        // Change calculation: Order total 460, customer gives 500 -> change 40
        val change = SaleCalculator.calculateChange(460, 500)
        assertEquals(40, change)
    }

    @Test
    fun testDayCloseCalculations() {
        // Expected Cash formula from prompt:
        // Opening Cash: 2,000
        // + Cash Sales: 11,800
        // + Loans Received: 200
        // - Purchases / Expenses: 1,420
        // - Loans Given: 150
        // System Expected Cash: 2000 + 11800 + 200 - 1420 - 150 = 12,430
        val recon = DayCloseCalculator.calculateCashReconciliation(
            openingCash = 2000,
            cashSales = 11800,
            borrowingsReceived = 200,
            cashExpenses = 1420,
            loansGiven = 150,
            actualCountedCash = 12430
        )
        assertEquals(12430, recon.systemExpectedCash)
        assertEquals(12430, recon.actualCountedCash)
        assertEquals(0, recon.variance)
        assertTrue(recon.isBalanced)
        assertEquals("BALANCED", recon.varianceLabel)

        // Test short cash difference
        // Expected 12,750, Actual 12,700 -> Short by Rs 50
        val shortRecon = DayCloseCalculator.calculateCashReconciliation(
            openingCash = 2000,
            cashSales = 10750,
            actualCountedCash = 12700
        )
        assertEquals(12750, shortRecon.systemExpectedCash)
        assertEquals(-50, shortRecon.variance)
        assertEquals("SHORT by Rs. 50", shortRecon.varianceLabel)
    }

    @Test
    fun testStockWeightedAverageCost() {
        // Existing: 5 kg at Rs 700/kg = 3,500
        // Purchase: 10 kg at Rs 720/kg = 7,200
        // New avg cost = (3500 + 7200) / 15 = 713.333
        val avgCost = StockCalculator.calculateWeightedAverageCost(
            oldQty = 5.0,
            oldAvgCost = 700.0,
            receivedQty = 10.0,
            purchaseUnitCost = 720.0
        )
        assertEquals(713.333, avgCost, 0.001)

        // Stock calculation
        val currentStock = StockCalculator.calculateCurrentStock(
            openingQuantity = 5.0,
            movementQuantities = listOf(10.0, -0.5, -0.25, 0.75)
        )
        assertEquals(15.0, currentStock, 0.001)
    }
}
