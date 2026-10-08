package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val price: Int, // Whole rupees
    val costPrice: Int,
    val sku: String,
    val unit: String = "unit",
    val currentStock: Double, // Decimal for fractional items like chicken (kg)
    val lowStockThreshold: Double = 5.0,
    val imageUrl: String = "",
    val code: String = "#01",
    val isAvailable: Boolean = true
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String, // e.g. #047
    val dateMillis: Long,
    val businessDate: String, // e.g. 2026-10-02
    val subtotal: Int,
    val discountAmount: Int,
    val discountPercentage: Int = 0,
    val total: Int,
    val orderType: String = "Walk-in Counter",
    val status: String = "PAID", // PAID, PARTIAL, UNPAID, VOIDED
    val kotNumber: Int = 12
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String, // Saved at time of sale so menu changes don't alter history
    val unitPrice: Int, // Saved at time of sale
    val quantity: Int,
    val lineTotal: Int,
    val notes: String = ""
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val method: String, // CASH, JAZZCASH, SPLIT
    val amount: Int, // Total amount allocated to order
    val cashReceived: Int = 0, // Customer tendered cash
    val changeReturned: Int = 0,
    val dateMillis: Long,
    val businessDate: String
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: String, // OPENING, PURCHASE, WASTE, PERSONAL, SALE, ADJUSTMENT
    val quantity: Double, // Positive for increase (+10), negative for deduction (-0.5)
    val unitCost: Int = 0,
    val reason: String = "",
    val dateMillis: Long,
    val businessDate: String
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // Gas/Fuel, Raw Chicken, Bakery, Packaging, Rent, Utilities, Other
    val amount: Int, // Money in whole rupees
    val paymentMethod: String, // CASH, JAZZCASH
    val description: String = "",
    val dateMillis: Long,
    val businessDate: String
)

@Entity(tableName = "waste_logs")
data class WasteLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val reasonCategory: String, // BURNT, SPOILED, STAFF MEAL, COURTESY
    val locationOrNotes: String = "",
    val estimatedCost: Int, // In whole rupees
    val dateMillis: Long,
    val businessDate: String
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val direction: String, // GIVEN (shop gave loan to person), RECEIVED (shop borrowed from person)
    val originalAmount: Int,
    val remainingBalance: Int,
    val dueDate: String = "",
    val notes: String = "",
    val status: String = "OPEN", // OPEN, SETTLED, OVERDUE
    val dateMillis: Long
) {
    val khataCode: String
        get() = "KH-${100 + id}"
}

@Entity(tableName = "loan_transactions")
data class LoanTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val type: String, // BORROW, LEND, REPAYMENT
    val amount: Int,
    val paymentMethod: String = "CASH", // CASH, JAZZCASH
    val dateMillis: Long,
    val businessDate: String
)

@Entity(tableName = "day_close_records")
data class DayCloseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessDate: String,
    val openingCash: Int,
    val cashSales: Int,
    val jazzCashSales: Int,
    val totalOrders: Int,
    val totalGrossSales: Int,
    val purchasesCash: Int,
    val otherExpensesCash: Int,
    val wasteCost: Int,
    val personalCost: Int,
    val loansReceivedCash: Int,
    val loansGivenCash: Int,
    val borrowingsRepaidCash: Int = 0,
    val loanRepaymentsReceivedCash: Int = 0,
    val systemExpectedCash: Int,
    val actualCountedCash: Int,
    val cashVariance: Int, // actual - expected
    val expectedJazzCash: Int,
    val actualJazzCash: Int = 0,
    val estimatedNetProfit: Int,
    val cashierName: String = "Tariq Mehmood",
    val shiftDetails: String = "11:30 AM – 11:45 PM • Till #01",
    val closedAtMillis: Long,
    val isAudited: Boolean = true
)
