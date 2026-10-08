package com.example.domain

import kotlin.math.max
import kotlin.math.roundToInt

object SaleCalculator {

    /** Line Total = Quantity * Unit Sale Price */
    fun calculateLineTotal(quantity: Int, unitPrice: Int): Int {
        require(quantity >= 0) { "Quantity cannot be negative" }
        require(unitPrice >= 0) { "Unit price cannot be negative" }
        return quantity * unitPrice
    }

    /** Subtotal = Sum of Line Totals */
    fun calculateSubtotal(lineTotals: List<Int>): Int {
        return lineTotals.sum()
    }

    /**
     * Discount calculation:
     * Supports fixed-rupee discount or percentage discount (including decimal percentages like 2%, 7%, 3.5%).
     * Enforces discount cannot exceed subtotal.
     */
    fun calculateDiscount(subtotal: Int, discountPercentage: Double = 0.0, fixedDiscountRupees: Int = 0): Int {
        val calculated = if (discountPercentage > 0.0) {
            ((subtotal * discountPercentage) / 100.0).roundToInt()
        } else {
            fixedDiscountRupees
        }
        return calculated.coerceIn(0, subtotal)
    }

    fun calculateDiscount(subtotal: Int, discountPercentage: Int, fixedDiscountRupees: Int): Int {
        return calculateDiscount(subtotal, discountPercentage.toDouble(), fixedDiscountRupees)
    }

    /** Final Order Total = Subtotal - Discount */
    fun calculateTotal(subtotal: Int, discount: Int): Int {
        return max(0, subtotal - discount)
    }

    /**
     * Cash Payment Change:
     * Change = max(0, Cash Received - Amount Due)
     */
    fun calculateChange(amountDue: Int, cashReceived: Int): Int {
        return max(0, cashReceived - amountDue)
    }

    /** Remaining Due = max(0, Order Total - Total Paid) */
    fun calculateRemainingDue(orderTotal: Int, totalPaid: Int): Int {
        return max(0, orderTotal - totalPaid)
    }
}

object StockCalculator {

    /** Current Stock = Opening Quantity + Sum of Stock Movement Quantities */
    fun calculateCurrentStock(openingQuantity: Double, movementQuantities: List<Double>): Double {
        return openingQuantity + movementQuantities.sum()
    }

    /** Low stock check: Current Stock <= Low Stock Limit */
    fun isLowStock(currentStock: Double, threshold: Double): Boolean {
        return currentStock <= threshold
    }

    /**
     * Weighted Average Cost:
     * (Old Qty * Old Avg Cost + Received Qty * Purchase Cost) / (Old Qty + Received Qty)
     */
    fun calculateWeightedAverageCost(
        oldQty: Double,
        oldAvgCost: Double,
        receivedQty: Double,
        purchaseUnitCost: Double
    ): Double {
        val totalQty = oldQty + receivedQty
        if (totalQty <= 0.0) return 0.0
        return ((oldQty * oldAvgCost) + (receivedQty * purchaseUnitCost)) / totalQty
    }

    /** Estimated Stock Value = Current Quantity * Cost per Unit */
    fun calculateEstimatedStockValue(currentQuantity: Double, costPerUnit: Double): Double {
        return max(0.0, currentQuantity * costPerUnit)
    }
}

object DayCloseCalculator {

    data class CashReconciliation(
        val openingCash: Int,
        val cashSales: Int,
        val loanRepaymentsReceived: Int,
        val borrowingsReceived: Int,
        val otherCashIn: Int,
        val cashExpenses: Int,
        val cashPurchases: Int,
        val loansGiven: Int,
        val borrowingsRepaid: Int,
        val otherCashOut: Int,
        val systemExpectedCash: Int,
        val actualCountedCash: Int,
        val variance: Int, // actual - expected
        val isBalanced: Boolean,
        val varianceLabel: String
    )

    data class JazzCashReconciliation(
        val openingJazzCash: Int,
        val jazzCashSales: Int,
        val otherIn: Int,
        val jazzCashExpenses: Int,
        val jazzCashPurchases: Int,
        val expectedJazzCash: Int,
        val actualJazzCash: Int,
        val variance: Int
    )

    data class ProfitSummary(
        val grossSales: Int,
        val directExpenses: Int,
        val wasteCost: Int,
        val personalCost: Int,
        val estimatedDailyNetProfit: Int,
        val marginPercentage: Double
    )

    /**
     * Formula:
     * Expected Cash = Opening Cash
     *               + Cash Sales Collected
     *               + Loan Repayments Received
     *               + Borrowings Received
     *               + Other Cash In
     *               - Cash Expenses
     *               - Cash Purchases Paid
     *               - Loans Given
     *               - Borrowings Repaid
     *               - Other Cash Out
     */
    fun calculateCashReconciliation(
        openingCash: Int,
        cashSales: Int,
        loanRepaymentsReceived: Int = 0,
        borrowingsReceived: Int = 0,
        otherCashIn: Int = 0,
        cashExpenses: Int = 0,
        cashPurchases: Int = 0,
        loansGiven: Int = 0,
        borrowingsRepaid: Int = 0,
        otherCashOut: Int = 0,
        actualCountedCash: Int
    ): CashReconciliation {
        val totalInflow = cashSales + loanRepaymentsReceived + borrowingsReceived + otherCashIn
        val totalOutflow = cashExpenses + cashPurchases + loansGiven + borrowingsRepaid + otherCashOut
        val expected = openingCash + totalInflow - totalOutflow
        val variance = actualCountedCash - expected

        val varianceLabel = when {
            variance == 0 -> "BALANCED"
            variance > 0 -> "OVER by Rs. $variance"
            else -> "SHORT by Rs. ${-variance}"
        }

        return CashReconciliation(
            openingCash = openingCash,
            cashSales = cashSales,
            loanRepaymentsReceived = loanRepaymentsReceived,
            borrowingsReceived = borrowingsReceived,
            otherCashIn = otherCashIn,
            cashExpenses = cashExpenses,
            cashPurchases = cashPurchases,
            loansGiven = loansGiven,
            borrowingsRepaid = borrowingsRepaid,
            otherCashOut = otherCashOut,
            systemExpectedCash = expected,
            actualCountedCash = actualCountedCash,
            variance = variance,
            isBalanced = variance == 0,
            varianceLabel = varianceLabel
        )
    }

    fun calculateJazzCashReconciliation(
        openingJazzCash: Int = 0,
        jazzCashSales: Int,
        otherIn: Int = 0,
        jazzCashExpenses: Int = 0,
        jazzCashPurchases: Int = 0,
        actualJazzCash: Int = 0
    ): JazzCashReconciliation {
        val expected = openingJazzCash + jazzCashSales + otherIn - jazzCashExpenses - jazzCashPurchases
        val variance = actualJazzCash - expected
        return JazzCashReconciliation(
            openingJazzCash = openingJazzCash,
            jazzCashSales = jazzCashSales,
            otherIn = otherIn,
            jazzCashExpenses = jazzCashExpenses,
            jazzCashPurchases = jazzCashPurchases,
            expectedJazzCash = expected,
            actualJazzCash = actualJazzCash,
            variance = variance
        )
    }

    fun calculateProfit(
        grossSales: Int,
        directExpenses: Int,
        wasteCost: Int,
        personalCost: Int
    ): ProfitSummary {
        val totalDeductions = directExpenses + wasteCost + personalCost
        val netProfit = grossSales - totalDeductions
        val margin = if (grossSales > 0) (netProfit.toDouble() / grossSales) * 100.0 else 0.0
        return ProfitSummary(
            grossSales = grossSales,
            directExpenses = directExpenses,
            wasteCost = wasteCost,
            personalCost = personalCost,
            estimatedDailyNetProfit = netProfit,
            marginPercentage = margin
        )
    }
}
