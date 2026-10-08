package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.DayCloseRecordEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.LoanEntity
import com.example.data.model.LoanTransactionEntity
import com.example.data.model.OrderEntity
import com.example.data.model.OrderItemEntity
import com.example.data.model.PaymentEntity
import com.example.data.model.ProductEntity
import com.example.data.model.StockMovementEntity
import com.example.data.model.WasteLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // --- Products ---
    @Query("SELECT * FROM products ORDER BY id ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getProductByName(name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("UPDATE products SET currentStock = currentStock + :amount WHERE id = :productId")
    suspend fun adjustProductStock(productId: Long, amount: Double)

    @Query("UPDATE products SET currentStock = :newStock WHERE id = :productId")
    suspend fun updateProductStock(productId: Long, newStock: Double)

    @Query("UPDATE products SET price = :newPrice WHERE id = :productId")
    suspend fun updateProductPrice(productId: Long, newPrice: Int)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM orders")
    suspend fun clearOrders()

    @Query("DELETE FROM order_items")
    suspend fun clearOrderItems()

    @Query("DELETE FROM payments")
    suspend fun clearPayments()

    @Query("DELETE FROM stock_movements")
    suspend fun clearStockMovements()

    @Query("DELETE FROM expenses")
    suspend fun clearExpenses()

    @Query("DELETE FROM waste_logs")
    suspend fun clearWasteLogs()

    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Query("DELETE FROM loan_transactions")
    suspend fun clearLoanTransactions()

    @Query("DELETE FROM day_close_records")
    suspend fun clearDayCloseRecords()

    // --- Orders & Items ---
    @Query("SELECT * FROM orders ORDER BY dateMillis DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE businessDate = :date ORDER BY dateMillis DESC")
    fun getOrdersByDate(date: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE dateMillis >= :startMillis AND dateMillis < :endMillis ORDER BY dateMillis DESC")
    fun getOrdersBetween(startMillis: Long, endMillis: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE orderNumber = :orderNumber LIMIT 1")
    suspend fun getOrderByNumber(orderNumber: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsForOrder(orderId: Long): List<OrderItemEntity>

    @Query("SELECT * FROM order_items ORDER BY id DESC")
    fun getAllOrderItems(): Flow<List<OrderItemEntity>>

    // --- Payments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT * FROM payments WHERE businessDate = :date")
    fun getPaymentsByDate(date: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE dateMillis >= :startMillis AND dateMillis < :endMillis")
    fun getPaymentsBetween(startMillis: Long, endMillis: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    // --- Stock Movements ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements WHERE businessDate = :date ORDER BY dateMillis DESC")
    fun getStockMovementsByDate(date: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY dateMillis DESC")
    fun getAllStockMovements(): Flow<List<StockMovementEntity>>

    // --- Expenses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT * FROM expenses WHERE businessDate = :date ORDER BY dateMillis DESC")
    fun getExpensesByDate(date: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    // --- Waste & Personal Food Logs ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWasteLog(wasteLog: WasteLogEntity): Long

    @Update
    suspend fun updateWasteLog(wasteLog: WasteLogEntity)

    @Query("DELETE FROM waste_logs WHERE id = :id")
    suspend fun deleteWasteLogById(id: Long)

    @Query("SELECT * FROM waste_logs WHERE businessDate = :date ORDER BY dateMillis DESC")
    fun getWasteLogsByDate(date: String): Flow<List<WasteLogEntity>>

    @Query("SELECT * FROM waste_logs ORDER BY dateMillis DESC")
    fun getAllWasteLogs(): Flow<List<WasteLogEntity>>

    // --- Loans & Transactions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Update
    suspend fun updateLoan(loan: LoanEntity)

    @Query("SELECT * FROM loans ORDER BY dateMillis DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans WHERE id = :loanId")
    suspend fun getLoanById(loanId: Long): LoanEntity?

    @Query("SELECT * FROM loans WHERE LOWER(personName) = LOWER(:personName) LIMIT 1")
    suspend fun getLoanByName(personName: String): LoanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanTransaction(tx: LoanTransactionEntity): Long

    @Query("SELECT * FROM loan_transactions WHERE businessDate = :date")
    fun getLoanTransactionsByDate(date: String): Flow<List<LoanTransactionEntity>>

    @Query("SELECT * FROM loan_transactions WHERE loanId = :loanId ORDER BY dateMillis DESC")
    fun getLoanTransactionsForLoan(loanId: Long): Flow<List<LoanTransactionEntity>>

    @Query("SELECT * FROM loan_transactions ORDER BY dateMillis DESC")
    fun getAllLoanTransactions(): Flow<List<LoanTransactionEntity>>

    // --- Day Close Records ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayCloseRecord(record: DayCloseRecordEntity): Long

    @Query("SELECT * FROM day_close_records WHERE businessDate = :date LIMIT 1")
    suspend fun getDayCloseRecord(date: String): DayCloseRecordEntity?

    @Query("SELECT * FROM day_close_records ORDER BY closedAtMillis DESC")
    fun getAllDayCloseRecords(): Flow<List<DayCloseRecordEntity>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getProductCount(): Int
}
