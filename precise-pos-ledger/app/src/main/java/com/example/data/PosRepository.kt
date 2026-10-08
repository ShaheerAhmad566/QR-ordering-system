package com.example.data

import com.example.data.dao.PosDao
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val posDao: PosDao) {

    val allProducts: Flow<List<ProductEntity>> = posDao.getAllProducts()
    val allOrders: Flow<List<OrderEntity>> = posDao.getAllOrders()
    val allPayments: Flow<List<PaymentEntity>> = posDao.getAllPayments()
    val allExpenses: Flow<List<ExpenseEntity>> = posDao.getAllExpenses()
    val allWasteLogs: Flow<List<WasteLogEntity>> = posDao.getAllWasteLogs()
    val allStockMovements: Flow<List<StockMovementEntity>> = posDao.getAllStockMovements()
    val allLoans: Flow<List<LoanEntity>> = posDao.getAllLoans()
    val allLoanTransactions: Flow<List<LoanTransactionEntity>> = posDao.getAllLoanTransactions()
    val allDayCloseRecords: Flow<List<DayCloseRecordEntity>> = posDao.getAllDayCloseRecords()
    val allOrderItems: Flow<List<OrderItemEntity>> = posDao.getAllOrderItems()

    suspend fun completeSale(
        orderNumber: String,
        businessDate: String,
        subtotal: Int,
        discountAmount: Int,
        discountPercentage: Int,
        total: Int,
        items: List<OrderItemEntity>,
        paymentMethod: String, // CASH, JAZZCASH
        cashReceived: Int,
        changeReturned: Int
    ): Long {
        val now = System.currentTimeMillis()
        val order = OrderEntity(
            orderNumber = orderNumber,
            dateMillis = now,
            businessDate = businessDate,
            subtotal = subtotal,
            discountAmount = discountAmount,
            discountPercentage = discountPercentage,
            total = total,
            status = "PAID"
        )
        val orderId = posDao.insertOrder(order)
        val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
        posDao.insertOrderItems(itemsWithOrderId)

        val payment = PaymentEntity(
            orderId = orderId,
            method = paymentMethod,
            amount = total,
            cashReceived = if (paymentMethod == "CASH") cashReceived else total,
            changeReturned = if (paymentMethod == "CASH") changeReturned else 0,
            dateMillis = now,
            businessDate = businessDate
        )
        posDao.insertPayment(payment)

        // Deduct inventory stock if products match
        items.forEach { item ->
            posDao.adjustProductStock(item.productId, -item.quantity.toDouble())
            posDao.insertStockMovement(
                StockMovementEntity(
                    productId = item.productId,
                    productName = item.productName,
                    movementType = "SALE",
                    quantity = -item.quantity.toDouble(),
                    unitCost = item.unitPrice,
                    reason = "Order $orderNumber sale",
                    dateMillis = now,
                    businessDate = businessDate
                )
            )
        }

        return orderId
    }

    suspend fun addStock(
        productId: Long,
        productName: String,
        quantity: Double,
        unitCost: Int,
        businessDate: String,
        movementType: String = "PURCHASE"
    ) {
        val now = System.currentTimeMillis()
        if (productId > 0) {
            posDao.adjustProductStock(productId, quantity)
        }
        posDao.insertStockMovement(
            StockMovementEntity(
                productId = productId,
                productName = productName,
                movementType = movementType,
                quantity = quantity,
                unitCost = unitCost,
                reason = "Replenished inventory",
                dateMillis = now,
                businessDate = businessDate
            )
        )
    }

    suspend fun addExpense(
        title: String,
        category: String,
        amount: Int,
        paymentMethod: String,
        description: String,
        businessDate: String
    ) {
        posDao.insertExpense(
            ExpenseEntity(
                title = title,
                category = category,
                amount = amount,
                paymentMethod = paymentMethod,
                description = description,
                dateMillis = System.currentTimeMillis(),
                businessDate = businessDate
            )
        )
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        posDao.updateExpense(expense)
    }

    suspend fun deleteExpense(id: Long) {
        posDao.deleteExpenseById(id)
    }

    suspend fun addWasteLog(
        itemName: String,
        reasonCategory: String,
        locationOrNotes: String,
        estimatedCost: Int,
        businessDate: String
    ) {
        posDao.insertWasteLog(
            WasteLogEntity(
                itemName = itemName,
                reasonCategory = reasonCategory,
                locationOrNotes = locationOrNotes,
                estimatedCost = estimatedCost,
                dateMillis = System.currentTimeMillis(),
                businessDate = businessDate
            )
        )
    }

    suspend fun updateWasteLog(wasteLog: WasteLogEntity) {
        posDao.updateWasteLog(wasteLog)
    }

    suspend fun deleteWasteLog(id: Long) {
        posDao.deleteWasteLogById(id)
    }

    suspend fun addLoan(
        personName: String,
        direction: String, // GIVEN, RECEIVED
        amount: Int,
        dueDate: String,
        notes: String,
        businessDate: String
    ): Long {
        val now = System.currentTimeMillis()
        val trimmed = personName.trim()

        // Search for existing ledger by name or by ID (e.g., "KH-105" or "5")
        val existingByName = posDao.getLoanByName(trimmed)
        val existingById = if (trimmed.startsWith("KH-", ignoreCase = true)) {
            val idNum = trimmed.substring(3).toLongOrNull()?.let { it - 100 }
            if (idNum != null && idNum > 0) posDao.getLoanById(idNum) else null
        } else trimmed.toLongOrNull()?.let { posDao.getLoanById(it) }

        val existing = existingByName ?: existingById

        if (existing != null && existing.direction == direction) {
            val newBalance = existing.remainingBalance + amount
            val updated = existing.copy(
                remainingBalance = newBalance,
                status = "OPEN",
                dueDate = if (dueDate.isNotBlank()) dueDate else existing.dueDate,
                notes = if (notes.isNotBlank()) "${existing.notes} | $notes".trimStart('|', ' ') else existing.notes
            )
            posDao.updateLoan(updated)
            posDao.insertLoanTransaction(
                LoanTransactionEntity(
                    loanId = existing.id,
                    type = if (direction == "GIVEN") "LEND" else "BORROW",
                    amount = amount,
                    paymentMethod = "CASH",
                    dateMillis = now,
                    businessDate = businessDate
                )
            )
            return existing.id
        }

        val loanId = posDao.insertLoan(
            LoanEntity(
                personName = trimmed,
                direction = direction,
                originalAmount = amount,
                remainingBalance = amount,
                dueDate = dueDate,
                notes = notes,
                status = "OPEN",
                dateMillis = now
            )
        )
        posDao.insertLoanTransaction(
            LoanTransactionEntity(
                loanId = loanId,
                type = if (direction == "GIVEN") "LEND" else "BORROW",
                amount = amount,
                paymentMethod = "CASH",
                dateMillis = now,
                businessDate = businessDate
            )
        )
        return loanId
    }

    suspend fun repayLoan(
        loanId: Long,
        repaymentAmount: Int,
        paymentMethod: String,
        businessDate: String
    ) {
        val loan = posDao.getLoanById(loanId) ?: return
        val newBalance = (loan.remainingBalance - repaymentAmount).coerceAtLeast(0)
        val newStatus = if (newBalance == 0) "SETTLED" else "OPEN"
        posDao.updateLoan(
            loan.copy(
                remainingBalance = newBalance,
                status = newStatus
            )
        )
        posDao.insertLoanTransaction(
            LoanTransactionEntity(
                loanId = loanId,
                type = "REPAYMENT",
                amount = repaymentAmount,
                paymentMethod = paymentMethod,
                dateMillis = System.currentTimeMillis(),
                businessDate = businessDate
            )
        )
    }

    suspend fun saveDayClose(record: DayCloseRecordEntity) {
        posDao.insertDayCloseRecord(record)
    }

    suspend fun updateLoanBalance(loanId: Long, newBalance: Int) {
        val loan = posDao.getLoanById(loanId) ?: return
        posDao.updateLoan(loan.copy(remainingBalance = newBalance, status = if (newBalance <= 0) "SETTLED" else "OPEN"))
    }

    suspend fun insertLoanTransaction(
        loanId: Long,
        type: String,
        amount: Int,
        paymentMethod: String,
        businessDate: String,
        notes: String = ""
    ) {
        posDao.insertLoanTransaction(
            LoanTransactionEntity(
                loanId = loanId,
                type = type,
                amount = amount,
                paymentMethod = paymentMethod,
                dateMillis = System.currentTimeMillis(),
                businessDate = businessDate
            )
        )
    }

    suspend fun updateProductPrice(productId: Long, newPrice: Int) {
        posDao.updateProductPrice(productId, newPrice)
    }

    suspend fun updateProduct(product: ProductEntity) {
        posDao.updateProduct(product)
    }

    suspend fun deleteProduct(productId: Long) {
        posDao.deleteProductById(productId)
    }

    suspend fun addNewProduct(
        name: String,
        category: String,
        price: Int,
        costPrice: Int,
        sku: String,
        code: String,
        initialStock: Double,
        imageUrl: String = ""
    ): Long {
        val finalImage = if (imageUrl.isNotBlank()) imageUrl else when {
            name.contains("burger", ignoreCase = true) || category.equals("Burgers", ignoreCase = true) -> "food_zinger"
            name.contains("shawarma", ignoreCase = true) || category.equals("Shawarma", ignoreCase = true) -> "food_shawarma"
            name.contains("fries", ignoreCase = true) || category.equals("Fries", ignoreCase = true) -> "food_fries"
            category.equals("Drinks", ignoreCase = true) || name.contains("dew", ignoreCase = true) || name.contains("coke", ignoreCase = true) || name.contains("pepsi", ignoreCase = true) -> "food_drink"
            else -> ""
        }
        val prod = ProductEntity(
            name = name,
            category = category,
            price = price,
            costPrice = costPrice,
            sku = sku,
            code = code,
            currentStock = initialStock,
            lowStockThreshold = 10.0,
            imageUrl = finalImage
        )
        return posDao.insertProduct(prod)
    }

    suspend fun getProductByName(name: String): ProductEntity? = posDao.getProductByName(name)
    suspend fun getOrderByNumber(orderNumber: String): OrderEntity? = posDao.getOrderByNumber(orderNumber)
    suspend fun getLoanByName(personName: String): LoanEntity? = posDao.getLoanByName(personName)

    suspend fun restoreOrder(order: OrderEntity, items: List<OrderItemEntity> = emptyList(), paymentMethod: String = "CASH"): Long {
        val existing = posDao.getOrderByNumber(order.orderNumber)
        val orderId = if (existing != null) {
            posDao.insertOrder(order.copy(id = existing.id))
            existing.id
        } else {
            posDao.insertOrder(order)
        }
        if (items.isNotEmpty()) {
            posDao.insertOrderItems(items.map { it.copy(orderId = orderId) })
        }
        val payment = PaymentEntity(
            orderId = orderId,
            method = paymentMethod,
            amount = order.total,
            cashReceived = order.total,
            changeReturned = 0,
            dateMillis = order.dateMillis,
            businessDate = order.businessDate
        )
        posDao.insertPayment(payment)
        return orderId
    }

    suspend fun restoreExpense(expense: ExpenseEntity): Long {
        return posDao.insertExpense(expense)
    }

    suspend fun restoreProduct(product: ProductEntity): Long {
        val existing = posDao.getProductByName(product.name)
        return if (existing != null) {
            val updated = existing.copy(
                currentStock = product.currentStock,
                costPrice = if (product.costPrice > 0) product.costPrice else existing.costPrice,
                price = if (product.price > 0) product.price else existing.price,
                sku = if (product.sku.isNotBlank()) product.sku else existing.sku,
                category = if (product.category.isNotBlank()) product.category else existing.category
            )
            posDao.updateProduct(updated)
            existing.id
        } else {
            posDao.insertProduct(product)
        }
    }

    suspend fun restoreStockMovement(movement: StockMovementEntity): Long {
        return posDao.insertStockMovement(movement)
    }

    suspend fun restoreLoan(loan: LoanEntity): Long {
        val existing = posDao.getLoanByName(loan.personName)
        return if (existing != null) {
            val updated = existing.copy(
                remainingBalance = loan.remainingBalance,
                status = loan.status,
                dueDate = if (loan.dueDate.isNotBlank()) loan.dueDate else existing.dueDate
            )
            posDao.updateLoan(updated)
            existing.id
        } else {
            posDao.insertLoan(loan)
        }
    }

    /**
     * Seeds or replaces with the EXACT user business values starting October 1, 2026.
     */
    suspend fun initializeSeedDataIfNeeded(businessDate: String = "2026-10-07", forceReset: Boolean = false) {
        if (!forceReset && posDao.getProductCount() > 0) return

        posDao.clearProducts()
        posDao.clearOrders()
        posDao.clearOrderItems()
        posDao.clearPayments()
        posDao.clearStockMovements()
        posDao.clearExpenses()
        posDao.clearWasteLogs()
        posDao.clearLoans()
        posDao.clearLoanTransactions()
        posDao.clearDayCloseRecords()

        val oct7 = 1791331200000L // Approx timestamp for Oct 7, 2026
        val oct6 = oct7 - 86400000L

        // 1. PRODUCTS & MENU:
        // Including current stock balances as of 7-Oct-2026
        val realProducts = listOf(
            ProductEntity(name = "Zinger Burger", category = "Burgers", price = 300, costPrice = 180, sku = "BRG-01", code = "#01", currentStock = 20.0, lowStockThreshold = 10.0, imageUrl = "food_zinger"),
            ProductEntity(name = "Double Egg", category = "Burgers", price = 190, costPrice = 95, sku = "BRG-02", code = "#02", currentStock = 18.0, lowStockThreshold = 10.0, imageUrl = "food_zinger"),
            ProductEntity(name = "Shawr Special", category = "Shawarma", price = 200, costPrice = 100, sku = "SHW-01", code = "#03", currentStock = 15.0, lowStockThreshold = 8.0, imageUrl = "food_shawarma"),
            ProductEntity(name = "Shawarma Normal", category = "Shawarma", price = 160, costPrice = 80, sku = "SHW-02", code = "#04", currentStock = 12.0, lowStockThreshold = 8.0, imageUrl = "food_shawarma"),
            ProductEntity(name = "Burger bread", category = "Raw Prep & Bakery", price = 30, costPrice = 15, sku = "BAK-04", code = "#05", unit = "pcs", currentStock = 17.0, lowStockThreshold = 12.0),
            ProductEntity(name = "Dew regular", category = "Drinks", price = 60, costPrice = 40, sku = "BEV-01", code = "#06", unit = "bottles", currentStock = 5.0, lowStockThreshold = 10.0, imageUrl = "food_drink"),
            ProductEntity(name = "Fries in Kg/g", category = "Raw Prep & Bakery", price = 100, costPrice = 45, sku = "FRI-01", code = "#07", unit = "Kg", currentStock = 1.4, lowStockThreshold = 1.0, imageUrl = "food_fries"),
            ProductEntity(name = "Eggs", category = "Raw Prep & Bakery", price = 40, costPrice = 20, sku = "EGG-01", code = "#08", unit = "pcs", currentStock = 40.0, lowStockThreshold = 15.0),
            ProductEntity(name = "Dew Plastic", category = "Drinks", price = 80, costPrice = 55, sku = "BEV-02", code = "#09", unit = "bottles", currentStock = 9.0, lowStockThreshold = 10.0, imageUrl = "food_drink"),
            ProductEntity(name = "Coke regular", category = "Drinks", price = 60, costPrice = 40, sku = "BEV-03", code = "#10", unit = "bottles", currentStock = 24.0, lowStockThreshold = 10.0, imageUrl = "food_drink"),
            ProductEntity(name = "Pepsi Plastic", category = "Drinks", price = 80, costPrice = 55, sku = "BEV-04", code = "#11", unit = "bottles", currentStock = 8.0, lowStockThreshold = 8.0, imageUrl = "food_drink"),
            ProductEntity(name = "Zinger burger Bread", category = "Raw Prep & Bakery", price = 50, costPrice = 20, sku = "BAK-09", code = "#12", unit = "pcs", currentStock = 13.0, lowStockThreshold = 10.0),
            ProductEntity(name = "Shawarma Bread", category = "Raw Prep & Bakery", price = 20, costPrice = 10, sku = "BAK-12", code = "#13", unit = "pcs", currentStock = 23.0, lowStockThreshold = 10.0),
            ProductEntity(name = "Chicken Patties", category = "Raw Prep & Bakery", price = 100, costPrice = 60, sku = "PAT-01", code = "#14", unit = "pcs", currentStock = 10.0, lowStockThreshold = 5.0),
            ProductEntity(name = "Zinger meat(pieces)", category = "Raw Prep & Bakery", price = 150, costPrice = 80, sku = "MEAT-01", code = "#15", unit = "pieces", currentStock = 8.0, lowStockThreshold = 5.0),
            ProductEntity(name = "Shami Burger", category = "Burgers", price = 150, costPrice = 75, sku = "BRG-03", code = "#16", currentStock = 20.0, imageUrl = "food_zinger"),
            ProductEntity(name = "Chicken Patty Burger", category = "Burgers", price = 250, costPrice = 130, sku = "BRG-05", code = "#17", currentStock = 15.0, imageUrl = "food_zinger"),
            ProductEntity(name = "Double Chicken Patty Burger", category = "Burgers", price = 350, costPrice = 190, sku = "BRG-06", code = "#18", currentStock = 12.0, imageUrl = "food_zinger"),
            ProductEntity(name = "Special Double Chicken Patty Burger", category = "Burgers", price = 370, costPrice = 205, sku = "BRG-07", code = "#19", currentStock = 10.0, imageUrl = "food_zinger"),
            ProductEntity(name = "Zinger Shawarma", category = "Shawarma", price = 280, costPrice = 140, sku = "SHW-03", code = "#20", currentStock = 15.0, imageUrl = "food_shawarma"),
            ProductEntity(name = "Full Fries", category = "Fries", price = 200, costPrice = 90, sku = "FRI-02", code = "#21", currentStock = 15.0, imageUrl = "food_fries"),
            ProductEntity(name = "Family Fries", category = "Fries", price = 350, costPrice = 160, sku = "FRI-03", code = "#22", currentStock = 10.0, imageUrl = "food_fries")
        )
        posDao.insertProducts(realProducts)

        // Record Initial Opening Stock Movements for 7-Oct-2026
        val openingStocks = listOf(
            "Dew regular" to 5.0,
            "Dew Plastic" to 9.0,
            "Coke regular" to 24.0,
            "Pepsi Plastic" to 8.0,
            "Burger bread" to 17.0,
            "Zinger burger Bread" to 13.0,
            "Shawarma Bread" to 23.0,
            "Eggs" to 40.0,
            "Fries in Kg/g" to 1.4,
            "Zinger meat(pieces)" to 8.0
        )
        openingStocks.forEach { (name, qty) ->
            posDao.insertStockMovement(
                StockMovementEntity(
                    productId = 0,
                    productName = name,
                    movementType = "OPENING",
                    quantity = qty,
                    unitCost = 0,
                    reason = "Carried over from 06-10-2026",
                    dateMillis = oct6,
                    businessDate = "2026-10-06"
                )
            )
        }

        // 2. RECEIVABLES & PAYABLES (Carried over from 6-Oct-2026)
        val receivables = listOf(
            "Aliyan" to 390,
            "Dogar" to 1800,
            "Azan" to 3760,
            "Zaigham" to 300,
            "Faizan (Mamo Street)" to 200,
            "Sir Zahir" to 200,
            "Zernish" to 3860,
            "Dogar Auntie" to 620,
            "Ali Clinic Girl" to 270
        )
        receivables.forEach { (person, amt) ->
            val loanId = posDao.insertLoan(
                LoanEntity(
                    personName = person,
                    direction = "GIVEN",
                    originalAmount = amt,
                    remainingBalance = amt,
                    dueDate = "2026-10-15",
                    notes = "Receivable loan / credit balance",
                    status = "OPEN",
                    dateMillis = oct6
                )
            )
            posDao.insertLoanTransaction(
                LoanTransactionEntity(
                    loanId = loanId,
                    type = "LEND",
                    amount = amt,
                    paymentMethod = "CASH",
                    dateMillis = oct6,
                    businessDate = "2026-10-06"
                )
            )
        }

        val payableId = posDao.insertLoan(
            LoanEntity(
                personName = "Mena",
                direction = "RECEIVED",
                originalAmount = 1400,
                remainingBalance = 1400,
                dueDate = "2026-10-15",
                notes = "Shop borrowing payable to Mena",
                status = "OPEN",
                dateMillis = oct6
            )
        )
        posDao.insertLoanTransaction(
            LoanTransactionEntity(
                loanId = payableId,
                type = "BORROW",
                amount = 1400,
                paymentMethod = "CASH",
                dateMillis = oct6,
                businessDate = "2026-10-06"
            )
        )

        // 3. SEED DAY CLOSE FOR 6-OCT TO CARRY CASH OVER TO 7-OCT
        // This ensures opening float for Oct 7 is exactly Cash 2400, JazzCash 1810
        posDao.insertDayCloseRecord(
            DayCloseRecordEntity(
                businessDate = "2026-10-06",
                openingCash = 2400,
                cashSales = 0,
                jazzCashSales = 0,
                totalOrders = 0,
                totalGrossSales = 0,
                purchasesCash = 0,
                otherExpensesCash = 0,
                wasteCost = 0,
                personalCost = 0,
                loansReceivedCash = 0,
                loansGivenCash = 0,
                systemExpectedCash = 2400,
                actualCountedCash = 2400,    // Carries over as opening cash for Oct 7
                cashVariance = 0,
                expectedJazzCash = 1810,
                actualJazzCash = 1810,       // Carries over as opening JazzCash for Oct 7
                estimatedNetProfit = 0,
                cashierName = "System",
                shiftDetails = "Initial Balances Setup",
                closedAtMillis = oct6 + 50000000
            )
        )
    }
}
