package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.AccountingRepository
import com.example.data.model.BillReminderRecord
import com.example.data.model.BudgetRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.DebtRecord
import com.example.data.model.DebtStatus
import com.example.data.model.DebtType
import com.example.data.model.PaymentMethodRecord
import com.example.data.model.PaymentMethodType
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : AccountingRepository {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("ব্যবহারকারী লগইন করা নেই।")
        check(uid == expectedUserId) { "অ্যাকাউন্ট পরিবর্তিত হয়েছে; অননুমোদিত লেখা বাতিল করা হয়েছে।" }
        return uid
    }

    private fun userDoc(userId: String) = db.collection("users").document(userId)

    // --- Real-time Observation Streams ---

    override fun observeTransactions(userId: String): Flow<List<TransactionRecord>> =
        userDoc(userId).collection("transactions")
            .orderBy("dateEpochMillis", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TransactionRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeCategories(userId: String): Flow<List<CategoryRecord>> =
        userDoc(userId).collection("categories")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CategoryRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observePaymentMethods(userId: String): Flow<List<PaymentMethodRecord>> =
        userDoc(userId).collection("payment_methods")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PaymentMethodRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeBudgets(userId: String): Flow<List<BudgetRecord>> =
        userDoc(userId).collection("budgets")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BudgetRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeSavingsGoals(userId: String): Flow<List<SavingsGoalRecord>> =
        userDoc(userId).collection("savings_goals")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SavingsGoalRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeDebts(userId: String): Flow<List<DebtRecord>> =
        userDoc(userId).collection("debts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DebtRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeBillReminders(userId: String): Flow<List<BillReminderRecord>> =
        userDoc(userId).collection("bill_reminders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BillReminderRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    override fun observeHasPendingSync(userId: String): Flow<Boolean> =
        userDoc(userId).collection("transactions")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snapshot -> snapshot.metadata.hasPendingWrites() }

    // --- Initial Default Setup (Categories + Payment Methods) ---

    override suspend fun ensureDefaultSetup(userId: String): Result<Unit> {
        val basePath = "users/$userId"
        return try {
            val uid = requireAuthorizedUser(userId)
            val catCol = userDoc(uid).collection("categories")
            val pmCol = userDoc(uid).collection("payment_methods")

            val catSnap = catCol.limit(1).get().await()
            val pmSnap = pmCol.limit(1).get().await()

            if (catSnap.isEmpty || pmSnap.isEmpty) {
                val batch = db.batch()
                if (catSnap.isEmpty) {
                    defaultCategories().forEach { (id, item) ->
                        val payload = mapOf(
                            "userId" to uid,
                            "name" to item.name,
                            "type" to item.type,
                            "iconKey" to item.iconKey,
                            "colorHex" to item.colorHex,
                            "defaultCategory" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        batch.set(catCol.document(id), payload)
                    }
                }
                if (pmSnap.isEmpty) {
                    defaultPaymentMethods().forEach { (id, item) ->
                        val payload = mapOf(
                            "userId" to uid,
                            "name" to item.name,
                            "methodType" to item.methodType,
                            "openingBalance" to item.openingBalance,
                            "colorHex" to item.colorHex,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        batch.set(pmCol.document(id), payload)
                    }
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    // --- Transactions CRUD ---

    override suspend fun saveTransaction(
        userId: String,
        existingId: String?,
        type: TransactionType,
        amount: Double,
        categoryId: String,
        categoryName: String,
        paymentMethodId: String,
        paymentMethodName: String,
        note: String,
        dateEpochMillis: Long
    ): Result<Unit> {
        val cleanNote = note.trim().take(200)
        val col = userDoc(userId).collection("transactions")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        val op = if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
        return try {
            val uid = requireAuthorizedUser(userId)
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "সঠিক পরিমাণ লিখুন" }
            require(categoryId.isNotBlank() && categoryName.isNotBlank()) { "ক্যাটাগরি নির্বাচন করুন" }
            require(paymentMethodId.isNotBlank() && paymentMethodName.isNotBlank()) { "পেমেন্ট মেথড নির্বাচন করুন" }
            require(dateEpochMillis > 0L) { "সঠিক তারিখ নির্বাচন করুন" }

            if (existingId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to uid,
                    "type" to type.name,
                    "amount" to amount,
                    "categoryId" to categoryId,
                    "categoryName" to categoryName.take(60),
                    "paymentMethodId" to paymentMethodId,
                    "paymentMethodName" to paymentMethodName.take(60),
                    "note" to cleanNote,
                    "dateEpochMillis" to dateEpochMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "type" to type.name,
                    "amount" to amount,
                    "categoryId" to categoryId,
                    "categoryName" to categoryName.take(60),
                    "paymentMethodId" to paymentMethodId,
                    "paymentMethodName" to paymentMethodName.take(60),
                    "note" to cleanNote,
                    "dateEpochMillis" to dateEpochMillis,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("transactions").document(transactionId)
        return try {
            requireAuthorizedUser(userId)
            require(transactionId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Categories CRUD ---

    override suspend fun saveCategory(
        userId: String,
        existingId: String?,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ): Result<Unit> {
        val cleanName = name.trim().take(50)
        val col = userDoc(userId).collection("categories")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        val op = if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
        return try {
            val uid = requireAuthorizedUser(userId)
            require(cleanName.isNotEmpty()) { "ক্যাটাগরির নাম লিখুন" }
            if (existingId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "type" to type.name,
                    "iconKey" to iconKey.ifBlank { "category" }.take(40),
                    "colorHex" to colorHex,
                    "defaultCategory" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to cleanName,
                    "type" to type.name,
                    "iconKey" to iconKey.ifBlank { "category" }.take(40),
                    "colorHex" to colorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("categories").document(categoryId)
        return try {
            requireAuthorizedUser(userId)
            require(categoryId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Payment Methods CRUD ---

    override suspend fun savePaymentMethod(
        userId: String,
        existingId: String?,
        name: String,
        methodType: PaymentMethodType,
        openingBalance: Double,
        colorHex: Long
    ): Result<Unit> {
        val cleanName = name.trim().take(50)
        val col = userDoc(userId).collection("payment_methods")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        val op = if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
        return try {
            val uid = requireAuthorizedUser(userId)
            require(cleanName.isNotEmpty()) { "অ্যাকাউন্টের নাম লিখুন" }
            require(openingBalance in 0.0..1_000_000_000.0) { "সঠিক প্রারম্ভিক ব্যালেন্স দিন" }
            if (existingId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to uid,
                    "name" to cleanName,
                    "methodType" to methodType.name,
                    "openingBalance" to openingBalance,
                    "colorHex" to colorHex,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "name" to cleanName,
                    "methodType" to methodType.name,
                    "openingBalance" to openingBalance,
                    "colorHex" to colorHex,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deletePaymentMethod(userId: String, methodId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("payment_methods").document(methodId)
        return try {
            requireAuthorizedUser(userId)
            require(methodId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Budgets CRUD ---

    override suspend fun saveBudget(
        userId: String,
        categoryId: String,
        categoryName: String,
        monthKey: String,
        limitAmount: Double
    ): Result<Unit> {
        val safeCatId = categoryId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val docId = "${monthKey}_${safeCatId}".take(128)
        val docRef = userDoc(userId).collection("budgets").document(docId)
        return try {
            val uid = requireAuthorizedUser(userId)
            require(monthKey.matches(Regex("^[0-9]{4}-[0-9]{2}$"))) { "সঠিক মাস নির্বাচন করুন" }
            require(limitAmount > 0.0 && limitAmount <= 1_000_000_000.0) { "বাজেটের সীমা ০ থেকে বেশি হতে হবে" }
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "categoryName" to categoryName.take(60),
                        "limitAmount" to limitAmount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "categoryId" to categoryId,
                        "categoryName" to categoryName.take(60),
                        "monthKey" to monthKey,
                        "limitAmount" to limitAmount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("budgets").document(budgetId)
        return try {
            requireAuthorizedUser(userId)
            require(budgetId.isNotBlank())
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Savings Goals CRUD ---

    override suspend fun saveSavingsGoal(
        userId: String,
        existingId: String?,
        title: String,
        targetAmount: Double,
        savedAmount: Double,
        targetDateEpochMillis: Long,
        colorHex: Long
    ): Result<Unit> {
        val cleanTitle = title.trim().take(80)
        val col = userDoc(userId).collection("savings_goals")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        val op = if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
        return try {
            val uid = requireAuthorizedUser(userId)
            require(cleanTitle.isNotEmpty()) { "সঞ্চয়ের লক্ষ্য লিখুন" }
            require(targetAmount > 0.0 && targetAmount <= 1_000_000_000.0) { "সঠিক লক্ষ্যমাত্রা লিখুন" }
            require(savedAmount in 0.0..1_000_000_000.0) { "সঠিক জমার পরিমাণ লিখুন" }
            if (existingId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "targetAmount" to targetAmount,
                        "savedAmount" to savedAmount,
                        "targetDateEpochMillis" to targetDateEpochMillis.coerceAtLeast(0L),
                        "colorHex" to colorHex,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "targetAmount" to targetAmount,
                        "savedAmount" to savedAmount,
                        "targetDateEpochMillis" to targetDateEpochMillis.coerceAtLeast(0L),
                        "colorHex" to colorHex,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun addSavingsContribution(
        userId: String,
        goal: SavingsGoalRecord,
        deltaAmount: Double
    ): Result<Unit> {
        val docRef = userDoc(userId).collection("savings_goals").document(goal.id)
        return try {
            requireAuthorizedUser(userId)
            val newSaved = (goal.savedAmount + deltaAmount).coerceIn(0.0, 1_000_000_000.0)
            docRef.update(
                mapOf(
                    "savedAmount" to newSaved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteSavingsGoal(userId: String, goalId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("savings_goals").document(goalId)
        return try {
            requireAuthorizedUser(userId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Debts CRUD ---

    override suspend fun saveDebt(
        userId: String,
        existingId: String?,
        personName: String,
        phone: String,
        debtType: DebtType,
        amount: Double,
        paidAmount: Double,
        note: String,
        dueDateEpochMillis: Long
    ): Result<Unit> {
        val cleanName = personName.trim().take(80)
        val col = userDoc(userId).collection("debts")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        val op = if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
        return try {
            val uid = requireAuthorizedUser(userId)
            require(cleanName.isNotEmpty()) { "ব্যক্তির নাম লিখুন" }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "সঠিক টাকার পরিমাণ লিখুন" }
            val safePaid = paidAmount.coerceIn(0.0, amount)
            val status = if (safePaid >= amount) DebtStatus.SETTLED.name else DebtStatus.ACTIVE.name
            if (existingId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "personName" to cleanName,
                        "phone" to phone.trim().take(30),
                        "debtType" to debtType.name,
                        "amount" to amount,
                        "paidAmount" to safePaid,
                        "note" to note.trim().take(200),
                        "dueDateEpochMillis" to dueDateEpochMillis.coerceAtLeast(0L),
                        "status" to status,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "personName" to cleanName,
                        "phone" to phone.trim().take(30),
                        "debtType" to debtType.name,
                        "amount" to amount,
                        "paidAmount" to safePaid,
                        "note" to note.trim().take(200),
                        "dueDateEpochMillis" to dueDateEpochMillis.coerceAtLeast(0L),
                        "status" to status,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun recordDebtPayment(
        userId: String,
        debt: DebtRecord,
        paymentAmount: Double
    ): Result<Unit> {
        val docRef = userDoc(userId).collection("debts").document(debt.id)
        return try {
            requireAuthorizedUser(userId)
            require(paymentAmount > 0.0) { "সঠিক পরিশোধের পরিমাণ দিন" }
            val newPaid = (debt.paidAmount + paymentAmount).coerceIn(0.0, debt.amount)
            val newStatus = if (newPaid >= debt.amount) DebtStatus.SETTLED.name else DebtStatus.ACTIVE.name
            docRef.update(
                mapOf(
                    "paidAmount" to newPaid,
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteDebt(userId: String, debtId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("debts").document(debtId)
        return try {
            requireAuthorizedUser(userId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Bill Reminders CRUD ---

    override suspend fun saveBillReminder(
        userId: String,
        existingId: String?,
        title: String,
        amount: Double,
        dueDayOfMonth: Long,
        categoryName: String,
        recurringMonthly: Boolean,
        reminderEnabled: Boolean,
        lastPaidMonthKey: String
    ): Result<Unit> {
        val cleanTitle = title.trim().take(80)
        val col = userDoc(userId).collection("bill_reminders")
        val docRef = if (existingId.isNullOrBlank()) col.document() else col.document(existingId)
        val op = if (existingId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
        return try {
            val uid = requireAuthorizedUser(userId)
            require(cleanTitle.isNotEmpty()) { "বিলের নাম লিখুন" }
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "বিলের পরিমাণ সঠিক নয়" }
            val safeDay = dueDayOfMonth.coerceIn(1L, 31L)
            if (existingId.isNullOrBlank()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "amount" to amount,
                        "dueDayOfMonth" to safeDay,
                        "categoryName" to categoryName.ifBlank { "বিল ও ইউটিলিটি" }.take(60),
                        "recurringMonthly" to recurringMonthly,
                        "reminderEnabled" to reminderEnabled,
                        "lastPaidMonthKey" to lastPaidMonthKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "amount" to amount,
                        "dueDayOfMonth" to safeDay,
                        "categoryName" to categoryName.ifBlank { "বিল ও ইউটিলিটি" }.take(60),
                        "recurringMonthly" to recurringMonthly,
                        "reminderEnabled" to reminderEnabled,
                        "lastPaidMonthKey" to lastPaidMonthKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun markBillPaidForMonth(
        userId: String,
        billId: String,
        monthKey: String
    ): Result<Unit> {
        val docRef = userDoc(userId).collection("bill_reminders").document(billId)
        return try {
            requireAuthorizedUser(userId)
            docRef.update(
                mapOf(
                    "lastPaidMonthKey" to monthKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    override suspend fun deleteBillReminder(userId: String, billId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("bill_reminders").document(billId)
        return try {
            requireAuthorizedUser(userId)
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun defaultCategories(): List<Pair<String, CategoryRecord>> = listOf(
        "cat_food" to CategoryRecord(name = "খাবার ও বাজার", type = TransactionType.EXPENSE.name, iconKey = "restaurant", colorHex = 0xFF0E8A5D),
        "cat_rent" to CategoryRecord(name = "বাড়ি ভাড়া", type = TransactionType.EXPENSE.name, iconKey = "home", colorHex = 0xFF28666E),
        "cat_transport" to CategoryRecord(name = "যাতায়াত", type = TransactionType.EXPENSE.name, iconKey = "commute", colorHex = 0xFFD4921D),
        "cat_medical" to CategoryRecord(name = "চিকিৎসা", type = TransactionType.EXPENSE.name, iconKey = "medical", colorHex = 0xFFD14343),
        "cat_bills" to CategoryRecord(name = "বিল ও ইউটিলিটি", type = TransactionType.EXPENSE.name, iconKey = "bolt", colorHex = 0xFF5C6BC0),
        "cat_education" to CategoryRecord(name = "শিক্ষা ও বই", type = TransactionType.EXPENSE.name, iconKey = "school", colorHex = 0xFF7E57C2),
        "cat_shopping" to CategoryRecord(name = "কেনাকাটা", type = TransactionType.EXPENSE.name, iconKey = "shopping", colorHex = 0xFFE91E63),
        "cat_other_exp" to CategoryRecord(name = "অন্যান্য খরচ", type = TransactionType.EXPENSE.name, iconKey = "category", colorHex = 0xFF607D8B),
        "cat_salary" to CategoryRecord(name = "বেতন", type = TransactionType.INCOME.name, iconKey = "work", colorHex = 0xFF085B4B),
        "cat_business" to CategoryRecord(name = "ব্যবসা", type = TransactionType.INCOME.name, iconKey = "store", colorHex = 0xFF1E88E5),
        "cat_freelance" to CategoryRecord(name = "ফ্রিল্যান্সিং", type = TransactionType.INCOME.name, iconKey = "laptop", colorHex = 0xFF00897B),
        "cat_gift" to CategoryRecord(name = "বোনাস ও উপহার", type = TransactionType.INCOME.name, iconKey = "gift", colorHex = 0xFFD4921D),
        "cat_other_inc" to CategoryRecord(name = "অন্যান্য আয়", type = TransactionType.INCOME.name, iconKey = "savings", colorHex = 0xFF43A047)
    )

    private fun defaultPaymentMethods(): List<Pair<String, PaymentMethodRecord>> = listOf(
        "pm_cash" to PaymentMethodRecord(name = "নগদ টাকা (Cash)", methodType = PaymentMethodType.CASH.name, openingBalance = 0.0, colorHex = 0xFF085B4B),
        "pm_bank" to PaymentMethodRecord(name = "ব্যাংক অ্যাকাউন্ট", methodType = PaymentMethodType.BANK.name, openingBalance = 0.0, colorHex = 0xFF1E5F74),
        "pm_bkash" to PaymentMethodRecord(name = "বিকাশ (bKash)", methodType = PaymentMethodType.MOBILE_BANKING.name, openingBalance = 0.0, colorHex = 0xFFE2136E),
        "pm_nagad" to PaymentMethodRecord(name = "নগদ (Nagad)", methodType = PaymentMethodType.MOBILE_BANKING.name, openingBalance = 0.0, colorHex = 0xFFED6B23),
        "pm_rocket" to PaymentMethodRecord(name = "রকেট (Rocket)", methodType = PaymentMethodType.MOBILE_BANKING.name, openingBalance = 0.0, colorHex = 0xFF8E24AA)
    )
}
