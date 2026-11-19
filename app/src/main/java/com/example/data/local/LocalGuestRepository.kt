package com.example.data.local

import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LocalGuestRepository(context: Context) : AccountingRepository {
    private val storageFile = File(context.applicationContext.filesDir, "hisab_khata_guest_data.json")
    private val mutex = Mutex()

    private val transactionsFlow = MutableStateFlow<List<TransactionRecord>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<CategoryRecord>>(emptyList())
    private val paymentMethodsFlow = MutableStateFlow<List<PaymentMethodRecord>>(emptyList())
    private val budgetsFlow = MutableStateFlow<List<BudgetRecord>>(emptyList())
    private val goalsFlow = MutableStateFlow<List<SavingsGoalRecord>>(emptyList())
    private val debtsFlow = MutableStateFlow<List<DebtRecord>>(emptyList())
    private val billsFlow = MutableStateFlow<List<BillReminderRecord>>(emptyList())

    init {
        loadFromDiskOrSeed()
    }

    override fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = transactionsFlow
    override fun observeCategories(userId: String): Flow<List<CategoryRecord>> = categoriesFlow
    override fun observePaymentMethods(userId: String): Flow<List<PaymentMethodRecord>> = paymentMethodsFlow
    override fun observeBudgets(userId: String): Flow<List<BudgetRecord>> = budgetsFlow
    override fun observeSavingsGoals(userId: String): Flow<List<SavingsGoalRecord>> = goalsFlow
    override fun observeDebts(userId: String): Flow<List<DebtRecord>> = debtsFlow
    override fun observeBillReminders(userId: String): Flow<List<BillReminderRecord>> = billsFlow
    override fun observeHasPendingSync(userId: String): Flow<Boolean> = flowOf(false)

    override suspend fun ensureDefaultSetup(userId: String): Result<Unit> = mutex.withLock {
        runCatching {
            if (categoriesFlow.value.isEmpty()) {
                categoriesFlow.value = defaultCategories(userId)
            }
            if (paymentMethodsFlow.value.isEmpty()) {
                paymentMethodsFlow.value = defaultPaymentMethods(userId)
            }
            persistToDisk()
        }
    }

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
    ): Result<Unit> = mutex.withLock {
        runCatching {
            require(amount > 0.0 && amount <= 1_000_000_000.0) { "সঠিক পরিমাণ লিখুন" }
            val record = TransactionRecord(
                id = existingId ?: newId("tx"),
                userId = userId,
                type = type.name,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName.take(60),
                paymentMethodId = paymentMethodId,
                paymentMethodName = paymentMethodName.take(60),
                note = note.trim().take(200),
                dateEpochMillis = dateEpochMillis
            )
            val current = transactionsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == record.id }
            if (idx >= 0) current[idx] = record else current.add(record)
            transactionsFlow.value = current.sortedByDescending { it.dateEpochMillis }
            persistToDisk()
        }
    }

    override suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> = mutex.withLock {
        runCatching {
            transactionsFlow.value = transactionsFlow.value.filterNot { it.id == transactionId }
            persistToDisk()
        }
    }

    override suspend fun saveCategory(
        userId: String,
        existingId: String?,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val clean = name.trim().take(50)
            require(clean.isNotEmpty()) { "ক্যাটাগরির নাম লিখুন" }
            val record = CategoryRecord(
                id = existingId ?: newId("cat"),
                userId = userId,
                name = clean,
                type = type.name,
                iconKey = iconKey.ifBlank { "category" },
                colorHex = colorHex,
                defaultCategory = false
            )
            val current = categoriesFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == record.id }
            if (idx >= 0) current[idx] = record else current.add(record)
            categoriesFlow.value = current
            persistToDisk()
        }
    }

    override suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit> = mutex.withLock {
        runCatching {
            categoriesFlow.value = categoriesFlow.value.filterNot { it.id == categoryId }
            persistToDisk()
        }
    }

    override suspend fun savePaymentMethod(
        userId: String,
        existingId: String?,
        name: String,
        methodType: PaymentMethodType,
        openingBalance: Double,
        colorHex: Long
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val clean = name.trim().take(50)
            require(clean.isNotEmpty()) { "অ্যাকাউন্টের নাম লিখুন" }
            val record = PaymentMethodRecord(
                id = existingId ?: newId("pm"),
                userId = userId,
                name = clean,
                methodType = methodType.name,
                openingBalance = openingBalance.coerceIn(0.0, 1_000_000_000.0),
                colorHex = colorHex
            )
            val current = paymentMethodsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == record.id }
            if (idx >= 0) current[idx] = record else current.add(record)
            paymentMethodsFlow.value = current
            persistToDisk()
        }
    }

    override suspend fun deletePaymentMethod(userId: String, methodId: String): Result<Unit> = mutex.withLock {
        runCatching {
            paymentMethodsFlow.value = paymentMethodsFlow.value.filterNot { it.id == methodId }
            persistToDisk()
        }
    }

    override suspend fun saveBudget(
        userId: String,
        categoryId: String,
        categoryName: String,
        monthKey: String,
        limitAmount: Double
    ): Result<Unit> = mutex.withLock {
        runCatching {
            require(limitAmount > 0.0) { "বাজেটের সীমা ০ থেকে বেশি হতে হবে" }
            val docId = "${monthKey}_$categoryId"
            val record = BudgetRecord(
                id = docId,
                userId = userId,
                categoryId = categoryId,
                categoryName = categoryName.take(60),
                monthKey = monthKey,
                limitAmount = limitAmount
            )
            val current = budgetsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == docId }
            if (idx >= 0) current[idx] = record else current.add(record)
            budgetsFlow.value = current
            persistToDisk()
        }
    }

    override suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> = mutex.withLock {
        runCatching {
            budgetsFlow.value = budgetsFlow.value.filterNot { it.id == budgetId }
            persistToDisk()
        }
    }

    override suspend fun saveSavingsGoal(
        userId: String,
        existingId: String?,
        title: String,
        targetAmount: Double,
        savedAmount: Double,
        targetDateEpochMillis: Long,
        colorHex: Long
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val clean = title.trim().take(80)
            require(clean.isNotEmpty()) { "সঞ্চয়ের লক্ষ্য লিখুন" }
            require(targetAmount > 0.0) { "সঠিক লক্ষ্যমাত্রা লিখুন" }
            val record = SavingsGoalRecord(
                id = existingId ?: newId("goal"),
                userId = userId,
                title = clean,
                targetAmount = targetAmount,
                savedAmount = savedAmount.coerceIn(0.0, 1_000_000_000.0),
                targetDateEpochMillis = targetDateEpochMillis.coerceAtLeast(0L),
                colorHex = colorHex
            )
            val current = goalsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == record.id }
            if (idx >= 0) current[idx] = record else current.add(record)
            goalsFlow.value = current
            persistToDisk()
        }
    }

    override suspend fun addSavingsContribution(
        userId: String,
        goal: SavingsGoalRecord,
        deltaAmount: Double
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val updated = goal.copy(
                savedAmount = (goal.savedAmount + deltaAmount).coerceIn(0.0, 1_000_000_000.0)
            )
            goalsFlow.value = goalsFlow.value.map { if (it.id == goal.id) updated else it }
            persistToDisk()
        }
    }

    override suspend fun deleteSavingsGoal(userId: String, goalId: String): Result<Unit> = mutex.withLock {
        runCatching {
            goalsFlow.value = goalsFlow.value.filterNot { it.id == goalId }
            persistToDisk()
        }
    }

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
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val clean = personName.trim().take(80)
            require(clean.isNotEmpty()) { "ব্যক্তির নাম লিখুন" }
            require(amount > 0.0) { "সঠিক টাকার পরিমাণ লিখুন" }
            val safePaid = paidAmount.coerceIn(0.0, amount)
            val record = DebtRecord(
                id = existingId ?: newId("debt"),
                userId = userId,
                personName = clean,
                phone = phone.trim().take(30),
                debtType = debtType.name,
                amount = amount,
                paidAmount = safePaid,
                note = note.trim().take(200),
                dueDateEpochMillis = dueDateEpochMillis.coerceAtLeast(0L),
                status = if (safePaid >= amount) DebtStatus.SETTLED.name else DebtStatus.ACTIVE.name
            )
            val current = debtsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == record.id }
            if (idx >= 0) current[idx] = record else current.add(record)
            debtsFlow.value = current
            persistToDisk()
        }
    }

    override suspend fun recordDebtPayment(
        userId: String,
        debt: DebtRecord,
        paymentAmount: Double
    ): Result<Unit> = mutex.withLock {
        runCatching {
            require(paymentAmount > 0.0) { "সঠিক পরিশোধের পরিমাণ দিন" }
            val newPaid = (debt.paidAmount + paymentAmount).coerceIn(0.0, debt.amount)
            val updated = debt.copy(
                paidAmount = newPaid,
                status = if (newPaid >= debt.amount) DebtStatus.SETTLED.name else DebtStatus.ACTIVE.name
            )
            debtsFlow.value = debtsFlow.value.map { if (it.id == debt.id) updated else it }
            persistToDisk()
        }
    }

    override suspend fun deleteDebt(userId: String, debtId: String): Result<Unit> = mutex.withLock {
        runCatching {
            debtsFlow.value = debtsFlow.value.filterNot { it.id == debtId }
            persistToDisk()
        }
    }

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
    ): Result<Unit> = mutex.withLock {
        runCatching {
            val clean = title.trim().take(80)
            require(clean.isNotEmpty()) { "বিলের নাম লিখুন" }
            require(amount > 0.0) { "বিলের পরিমাণ সঠিক নয়" }
            val record = BillReminderRecord(
                id = existingId ?: newId("bill"),
                userId = userId,
                title = clean,
                amount = amount,
                dueDayOfMonth = dueDayOfMonth.coerceIn(1L, 31L),
                categoryName = categoryName.ifBlank { "বিল ও ইউটিলিটি" }.take(60),
                recurringMonthly = recurringMonthly,
                reminderEnabled = reminderEnabled,
                lastPaidMonthKey = lastPaidMonthKey
            )
            val current = billsFlow.value.toMutableList()
            val idx = current.indexOfFirst { it.id == record.id }
            if (idx >= 0) current[idx] = record else current.add(record)
            billsFlow.value = current
            persistToDisk()
        }
    }

    override suspend fun markBillPaidForMonth(
        userId: String,
        billId: String,
        monthKey: String
    ): Result<Unit> = mutex.withLock {
        runCatching {
            billsFlow.value = billsFlow.value.map {
                if (it.id == billId) it.copy(lastPaidMonthKey = monthKey) else it
            }
            persistToDisk()
        }
    }

    override suspend fun deleteBillReminder(userId: String, billId: String): Result<Unit> = mutex.withLock {
        runCatching {
            billsFlow.value = billsFlow.value.filterNot { it.id == billId }
            persistToDisk()
        }
    }

    private fun newId(prefix: String): String =
        "${prefix}_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    private fun loadFromDiskOrSeed() {
        val loaded = runCatching {
            if (!storageFile.exists()) return@runCatching false
            val root = JSONObject(storageFile.readText(Charsets.UTF_8))

            transactionsFlow.value = root.optJSONArray("transactions")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    TransactionRecord(
                        id = o.getString("id"),
                        userId = o.optString("userId", "guest_local"),
                        type = o.getString("type"),
                        amount = o.getDouble("amount"),
                        categoryId = o.getString("categoryId"),
                        categoryName = o.getString("categoryName"),
                        paymentMethodId = o.getString("paymentMethodId"),
                        paymentMethodName = o.getString("paymentMethodName"),
                        note = o.optString("note", ""),
                        dateEpochMillis = o.getLong("dateEpochMillis")
                    )
                }.sortedByDescending { it.dateEpochMillis }
            } ?: emptyList()

            categoriesFlow.value = root.optJSONArray("categories")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    CategoryRecord(
                        id = o.getString("id"),
                        userId = o.optString("userId", "guest_local"),
                        name = o.getString("name"),
                        type = o.getString("type"),
                        iconKey = o.getString("iconKey"),
                        colorHex = o.getLong("colorHex"),
                        defaultCategory = o.optBoolean("defaultCategory", false)
                    )
                }
            } ?: emptyList()

            paymentMethodsFlow.value = root.optJSONArray("payment_methods")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    PaymentMethodRecord(
                        id = o.getString("id"),
                        userId = o.optString("userId", "guest_local"),
                        name = o.getString("name"),
                        methodType = o.getString("methodType"),
                        openingBalance = o.getDouble("openingBalance"),
                        colorHex = o.getLong("colorHex")
                    )
                }
            } ?: emptyList()

            budgetsFlow.value = root.optJSONArray("budgets")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    BudgetRecord(
                        id = o.getString("id"),
                        userId = o.optString("userId", "guest_local"),
                        categoryId = o.getString("categoryId"),
                        categoryName = o.getString("categoryName"),
                        monthKey = o.getString("monthKey"),
                        limitAmount = o.getDouble("limitAmount")
                    )
                }
            } ?: emptyList()

            goalsFlow.value = root.optJSONArray("savings_goals")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    SavingsGoalRecord(
                        id = o.getString("id"),
                        userId = o.optString("userId", "guest_local"),
                        title = o.getString("title"),
                        targetAmount = o.getDouble("targetAmount"),
                        savedAmount = o.getDouble("savedAmount"),
                        targetDateEpochMillis = o.getLong("targetDateEpochMillis"),
                        colorHex = o.getLong("colorHex")
                    )
                }
            } ?: emptyList()

            debtsFlow.value = root.optJSONArray("debts")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    DebtRecord(
                        id = o.getString("id"),
                        userId = o.optString("userId", "guest_local"),
                        personName = o.getString("personName"),
                        phone = o.optString("phone", ""),
                        debtType = o.getString("debtType"),
                        amount = o.getDouble("amount"),
                        paidAmount = o.getDouble("paidAmount"),
                        note = o.optString("note", ""),
                        dueDateEpochMillis = o.getLong("dueDateEpochMillis"),
                        status = o.getString("status")
                    )
                }
            } ?: emptyList()

            billsFlow.value = root.optJSONArray("bill_reminders")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    BillReminderRecord(
                        id = o.getString("id"),
                        userId = o.optString("userId", "guest_local"),
                        title = o.getString("title"),
                        amount = o.getDouble("amount"),
                        dueDayOfMonth = o.getLong("dueDayOfMonth"),
                        categoryName = o.getString("categoryName"),
                        recurringMonthly = o.optBoolean("recurringMonthly", true),
                        reminderEnabled = o.optBoolean("reminderEnabled", true),
                        lastPaidMonthKey = o.optString("lastPaidMonthKey", "")
                    )
                }
            } ?: emptyList()

            true
        }.getOrDefault(false)

        if (!loaded) {
            categoriesFlow.value = defaultCategories("guest_local")
            paymentMethodsFlow.value = defaultPaymentMethods("guest_local")
        }
    }

    private suspend fun persistToDisk() = withContext(Dispatchers.IO) {
        val root = JSONObject().apply {
            put("transactions", JSONArray().apply {
                transactionsFlow.value.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("userId", t.userId)
                        put("type", t.type)
                        put("amount", t.amount)
                        put("categoryId", t.categoryId)
                        put("categoryName", t.categoryName)
                        put("paymentMethodId", t.paymentMethodId)
                        put("paymentMethodName", t.paymentMethodName)
                        put("note", t.note)
                        put("dateEpochMillis", t.dateEpochMillis)
                    })
                }
            })
            put("categories", JSONArray().apply {
                categoriesFlow.value.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("userId", c.userId)
                        put("name", c.name)
                        put("type", c.type)
                        put("iconKey", c.iconKey)
                        put("colorHex", c.colorHex)
                        put("defaultCategory", c.defaultCategory)
                    })
                }
            })
            put("payment_methods", JSONArray().apply {
                paymentMethodsFlow.value.forEach { pm ->
                    put(JSONObject().apply {
                        put("id", pm.id)
                        put("userId", pm.userId)
                        put("name", pm.name)
                        put("methodType", pm.methodType)
                        put("openingBalance", pm.openingBalance)
                        put("colorHex", pm.colorHex)
                    })
                }
            })
            put("budgets", JSONArray().apply {
                budgetsFlow.value.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("userId", b.userId)
                        put("categoryId", b.categoryId)
                        put("categoryName", b.categoryName)
                        put("monthKey", b.monthKey)
                        put("limitAmount", b.limitAmount)
                    })
                }
            })
            put("savings_goals", JSONArray().apply {
                goalsFlow.value.forEach { g ->
                    put(JSONObject().apply {
                        put("id", g.id)
                        put("userId", g.userId)
                        put("title", g.title)
                        put("targetAmount", g.targetAmount)
                        put("savedAmount", g.savedAmount)
                        put("targetDateEpochMillis", g.targetDateEpochMillis)
                        put("colorHex", g.colorHex)
                    })
                }
            })
            put("debts", JSONArray().apply {
                debtsFlow.value.forEach { d ->
                    put(JSONObject().apply {
                        put("id", d.id)
                        put("userId", d.userId)
                        put("personName", d.personName)
                        put("phone", d.phone)
                        put("debtType", d.debtType)
                        put("amount", d.amount)
                        put("paidAmount", d.paidAmount)
                        put("note", d.note)
                        put("dueDateEpochMillis", d.dueDateEpochMillis)
                        put("status", d.status)
                    })
                }
            })
            put("bill_reminders", JSONArray().apply {
                billsFlow.value.forEach { br ->
                    put(JSONObject().apply {
                        put("id", br.id)
                        put("userId", br.userId)
                        put("title", br.title)
                        put("amount", br.amount)
                        put("dueDayOfMonth", br.dueDayOfMonth)
                        put("categoryName", br.categoryName)
                        put("recurringMonthly", br.recurringMonthly)
                        put("reminderEnabled", br.reminderEnabled)
                        put("lastPaidMonthKey", br.lastPaidMonthKey)
                    })
                }
            })
        }
        storageFile.writeText(root.toString(), Charsets.UTF_8)
    }

    private fun defaultCategories(uid: String): List<CategoryRecord> = listOf(
        CategoryRecord(id = "cat_food", userId = uid, name = "খাবার ও বাজার", type = TransactionType.EXPENSE.name, iconKey = "restaurant", colorHex = 0xFF0E8A5D, defaultCategory = true),
        CategoryRecord(id = "cat_rent", userId = uid, name = "বাড়ি ভাড়া", type = TransactionType.EXPENSE.name, iconKey = "home", colorHex = 0xFF28666E, defaultCategory = true),
        CategoryRecord(id = "cat_transport", userId = uid, name = "যাতায়াত", type = TransactionType.EXPENSE.name, iconKey = "commute", colorHex = 0xFFD4921D, defaultCategory = true),
        CategoryRecord(id = "cat_medical", userId = uid, name = "চিকিৎসা", type = TransactionType.EXPENSE.name, iconKey = "medical", colorHex = 0xFFD14343, defaultCategory = true),
        CategoryRecord(id = "cat_bills", userId = uid, name = "বিল ও ইউটিলিটি", type = TransactionType.EXPENSE.name, iconKey = "bolt", colorHex = 0xFF5C6BC0, defaultCategory = true),
        CategoryRecord(id = "cat_education", userId = uid, name = "শিক্ষা ও বই", type = TransactionType.EXPENSE.name, iconKey = "school", colorHex = 0xFF7E57C2, defaultCategory = true),
        CategoryRecord(id = "cat_shopping", userId = uid, name = "কেনাকাটা", type = TransactionType.EXPENSE.name, iconKey = "shopping", colorHex = 0xFFE91E63, defaultCategory = true),
        CategoryRecord(id = "cat_other_exp", userId = uid, name = "অন্যান্য খরচ", type = TransactionType.EXPENSE.name, iconKey = "category", colorHex = 0xFF607D8B, defaultCategory = true),
        CategoryRecord(id = "cat_salary", userId = uid, name = "বেতন", type = TransactionType.INCOME.name, iconKey = "work", colorHex = 0xFF085B4B, defaultCategory = true),
        CategoryRecord(id = "cat_business", userId = uid, name = "ব্যবসা", type = TransactionType.INCOME.name, iconKey = "store", colorHex = 0xFF1E88E5, defaultCategory = true),
        CategoryRecord(id = "cat_freelance", userId = uid, name = "ফ্রিল্যান্সিং", type = TransactionType.INCOME.name, iconKey = "laptop", colorHex = 0xFF00897B, defaultCategory = true),
        CategoryRecord(id = "cat_gift", userId = uid, name = "বোনাস ও উপহার", type = TransactionType.INCOME.name, iconKey = "gift", colorHex = 0xFFD4921D, defaultCategory = true),
        CategoryRecord(id = "cat_other_inc", userId = uid, name = "অন্যান্য আয়", type = TransactionType.INCOME.name, iconKey = "savings", colorHex = 0xFF43A047, defaultCategory = true)
    )

    private fun defaultPaymentMethods(uid: String): List<PaymentMethodRecord> = listOf(
        PaymentMethodRecord(id = "pm_cash", userId = uid, name = "নগদ টাকা (Cash)", methodType = PaymentMethodType.CASH.name, openingBalance = 0.0, colorHex = 0xFF085B4B),
        PaymentMethodRecord(id = "pm_bank", userId = uid, name = "ব্যাংক অ্যাকাউন্ট", methodType = PaymentMethodType.BANK.name, openingBalance = 0.0, colorHex = 0xFF1E5F74),
        PaymentMethodRecord(id = "pm_bkash", userId = uid, name = "বিকাশ (bKash)", methodType = PaymentMethodType.MOBILE_BANKING.name, openingBalance = 0.0, colorHex = 0xFFE2136E),
        PaymentMethodRecord(id = "pm_nagad", userId = uid, name = "নগদ (Nagad)", methodType = PaymentMethodType.MOBILE_BANKING.name, openingBalance = 0.0, colorHex = 0xFFED6B23),
        PaymentMethodRecord(id = "pm_rocket", userId = uid, name = "রকেট (Rocket)", methodType = PaymentMethodType.MOBILE_BANKING.name, openingBalance = 0.0, colorHex = 0xFF8E24AA)
    )
}
