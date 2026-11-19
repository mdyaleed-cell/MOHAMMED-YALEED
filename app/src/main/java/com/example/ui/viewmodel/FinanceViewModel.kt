package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountingRepository
import com.example.data.model.BillReminderRecord
import com.example.data.model.BudgetRecord
import com.example.data.model.BudgetStatusItem
import com.example.data.model.CategoryRecord
import com.example.data.model.DebtRecord
import com.example.data.model.DebtStatus
import com.example.data.model.DebtType
import com.example.data.model.PaymentMethodRecord
import com.example.data.model.PaymentMethodType
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.model.UiState
import com.example.data.model.WalletBalanceItem
import com.example.util.Formatters
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReportPeriod(val labelBn: String) {
    DAILY("দৈনিক"),
    MONTHLY("মাসিক"),
    YEARLY("বাৎসরিক")
}

data class FinanceDashboardSnapshot(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val transactions: List<TransactionRecord> = emptyList(),
    val categories: List<CategoryRecord> = emptyList(),
    val paymentMethods: List<PaymentMethodRecord> = emptyList(),
    val budgets: List<BudgetRecord> = emptyList(),
    val savingsGoals: List<SavingsGoalRecord> = emptyList(),
    val debts: List<DebtRecord> = emptyList(),
    val billReminders: List<BillReminderRecord> = emptyList(),
    val walletBalances: List<WalletBalanceItem> = emptyList(),
    val totalNetBalance: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val currentMonthExpense: Double = 0.0,
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val currentMonthBudgetStatuses: List<BudgetStatusItem> = emptyList(),
    val unpaidBillsThisMonth: List<BillReminderRecord> = emptyList(),
    val totalLentActive: Double = 0.0,
    val totalBorrowedActive: Double = 0.0,
    val hasPendingSync: Boolean = false
)

class FinanceViewModel(
    private val repository: AccountingRepository,
    val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _selectedBudgetMonth = MutableStateFlow(Formatters.currentMonthKey())
    val selectedBudgetMonth: StateFlow<String> = _selectedBudgetMonth.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSetup(currentUserId)
                .onFailure { e ->
                    Log.w(TAG, "Default setup failed (will retry when online)", e)
                }
        }
    }

    private fun <T> Flow<T>.toSafeUiState(fallback: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, fallback, root)
                emit(UiState.Error(root.localizedMessage ?: fallback))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val transactionsState = repository.observeTransactions(currentUserId)
        .toSafeUiState("লেনদেন লোড করা যায়নি")
    private val categoriesState = repository.observeCategories(currentUserId)
        .toSafeUiState("ক্যাটাগরি লোড করা যায়নি")
    private val paymentMethodsState = repository.observePaymentMethods(currentUserId)
        .toSafeUiState("পেমেন্ট মেথড লোড করা যায়নি")
    private val budgetsState = repository.observeBudgets(currentUserId)
        .toSafeUiState("বাজেট লোড করা যায়নি")
    private val goalsState = repository.observeSavingsGoals(currentUserId)
        .toSafeUiState("সঞ্চয় লক্ষ্য লোড করা যায়নি")
    private val debtsState = repository.observeDebts(currentUserId)
        .toSafeUiState("দেনা-পাওনা লোড করা যায়নি")
    private val billsState = repository.observeBillReminders(currentUserId)
        .toSafeUiState("বিল রিমাইন্ডার লোড করা যায়নি")

    private val pendingSyncFlow: StateFlow<Boolean> = repository.observeHasPendingSync(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false
        )

    private val coreStreamsFlow = combine(
        transactionsState,
        categoriesState,
        paymentMethodsState,
        budgetsState
    ) { tx, cat, pm, b -> CoreStreams(tx, cat, pm, b) }

    private val planStreamsFlow = combine(
        goalsState,
        debtsState,
        billsState,
        pendingSyncFlow
    ) { g, d, bill, sync -> PlanStreams(g, d, bill, sync) }

    val dashboardState: StateFlow<FinanceDashboardSnapshot> = combine(
        coreStreamsFlow,
        planStreamsFlow,
        _selectedBudgetMonth
    ) { core, plan, budgetMonth ->
        val errors = listOfNotNull(
            (core.transactions as? UiState.Error)?.message,
            (core.categories as? UiState.Error)?.message,
            (core.paymentMethods as? UiState.Error)?.message,
            (core.budgets as? UiState.Error)?.message,
            (plan.goals as? UiState.Error)?.message,
            (plan.debts as? UiState.Error)?.message,
            (plan.bills as? UiState.Error)?.message
        )
        val loading = core.transactions is UiState.Loading ||
            core.categories is UiState.Loading ||
            core.paymentMethods is UiState.Loading

        val txList = (core.transactions as? UiState.Success)?.data ?: emptyList()
        val catList = (core.categories as? UiState.Success)?.data ?: emptyList()
        val pmList = (core.paymentMethods as? UiState.Success)?.data ?: emptyList()
        val budgetList = (core.budgets as? UiState.Success)?.data ?: emptyList()
        val goalList = (plan.goals as? UiState.Success)?.data ?: emptyList()
        val debtList = (plan.debts as? UiState.Success)?.data ?: emptyList()
        val billList = (plan.bills as? UiState.Success)?.data ?: emptyList()

        val thisMonthKey = Formatters.currentMonthKey()
        val todayKey = Formatters.dayKeyFromMillis(System.currentTimeMillis())

        var monthInc = 0.0
        var monthExp = 0.0
        var todayInc = 0.0
        var todayExp = 0.0

        val inByMethod = mutableMapOf<String, Double>()
        val outByMethod = mutableMapOf<String, Double>()
        val budgetMonthExpenseByCat = mutableMapOf<String, Double>()

        txList.forEach { tx ->
            val txMonth = Formatters.monthKeyFromMillis(tx.dateEpochMillis)
            val txDay = Formatters.dayKeyFromMillis(tx.dateEpochMillis)
            val isIncome = tx.parsedType == TransactionType.INCOME

            if (txMonth == thisMonthKey) {
                if (isIncome) monthInc += tx.amount else monthExp += tx.amount
            }
            if (txDay == todayKey) {
                if (isIncome) todayInc += tx.amount else todayExp += tx.amount
            }
            if (isIncome) {
                inByMethod[tx.paymentMethodId] = (inByMethod[tx.paymentMethodId] ?: 0.0) + tx.amount
            } else {
                outByMethod[tx.paymentMethodId] = (outByMethod[tx.paymentMethodId] ?: 0.0) + tx.amount
                if (txMonth == budgetMonth) {
                    budgetMonthExpenseByCat[tx.categoryId] =
                        (budgetMonthExpenseByCat[tx.categoryId] ?: 0.0) + tx.amount
                }
            }
        }

        val wallets = pmList.map { method ->
            val totalIn = inByMethod[method.id] ?: 0.0
            val totalOut = outByMethod[method.id] ?: 0.0
            WalletBalanceItem(
                method = method,
                currentBalance = method.openingBalance + totalIn - totalOut,
                totalIn = totalIn,
                totalOut = totalOut
            )
        }
        val netBalance = wallets.sumOf { it.currentBalance }

        val budgetStatuses = budgetList
            .filter { it.monthKey == budgetMonth }
            .map { b ->
                val spent = budgetMonthExpenseByCat[b.categoryId] ?: 0.0
                val ratio = if (b.limitAmount > 0.0) (spent / b.limitAmount).toFloat() else 0f
                BudgetStatusItem(
                    budget = b,
                    spentAmount = spent,
                    ratio = ratio,
                    exceeded = spent > b.limitAmount,
                    nearLimit = ratio >= 0.8f && spent <= b.limitAmount
                )
            }
            .sortedByDescending { it.ratio }

        val unpaidBills = billList
            .filter { it.lastPaidMonthKey != thisMonthKey }
            .sortedBy { it.dueDayOfMonth }

        val lentActive = debtList
            .filter { it.parsedDebtType == DebtType.LENT && it.parsedStatus == DebtStatus.ACTIVE }
            .sumOf { it.remainingAmount }
        val borrowedActive = debtList
            .filter { it.parsedDebtType == DebtType.BORROWED && it.parsedStatus == DebtStatus.ACTIVE }
            .sumOf { it.remainingAmount }

        FinanceDashboardSnapshot(
            isLoading = loading,
            errorMessage = errors.firstOrNull(),
            transactions = txList,
            categories = catList,
            paymentMethods = pmList,
            budgets = budgetList,
            savingsGoals = goalList,
            debts = debtList,
            billReminders = billList,
            walletBalances = wallets,
            totalNetBalance = netBalance,
            currentMonthIncome = monthInc,
            currentMonthExpense = monthExp,
            todayIncome = todayInc,
            todayExpense = todayExp,
            currentMonthBudgetStatuses = budgetStatuses,
            unpaidBillsThisMonth = unpaidBills,
            totalLentActive = lentActive,
            totalBorrowedActive = borrowedActive,
            hasPendingSync = plan.hasPendingSync
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = FinanceDashboardSnapshot()
    )

    fun setBudgetMonth(monthKey: String) {
        _selectedBudgetMonth.value = monthKey
    }

    fun saveTransaction(
        existingId: String?,
        type: TransactionType,
        amount: Double,
        category: CategoryRecord,
        paymentMethod: PaymentMethodRecord,
        note: String,
        dateEpochMillis: Long
    ) {
        viewModelScope.launch {
            repository.saveTransaction(
                userId = currentUserId,
                existingId = existingId,
                type = type,
                amount = amount,
                categoryId = category.id,
                categoryName = category.name,
                paymentMethodId = paymentMethod.id,
                paymentMethodName = paymentMethod.name,
                note = note,
                dateEpochMillis = dateEpochMillis
            ).onSuccess {
                _events.tryEmit(if (existingId == null) "হিসাব সংরক্ষিত হয়েছে" else "হিসাব হালনাগাদ হয়েছে")
            }.onFailure { e ->
                _events.tryEmit(e.localizedMessage ?: "হিসাব সংরক্ষণ করা যায়নি")
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(currentUserId, id)
                .onSuccess { _events.tryEmit("এন্ট্রি মুছে ফেলা হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "মুছে ফেলা যায়নি") }
        }
    }

    fun saveCategory(
        existingId: String?,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ) {
        viewModelScope.launch {
            repository.saveCategory(currentUserId, existingId, name, type, iconKey, colorHex)
                .onSuccess { _events.tryEmit("ক্যাটাগরি সংরক্ষিত হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "ক্যাটাগরি সংরক্ষণ ব্যর্থ") }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(currentUserId, categoryId)
                .onSuccess { _events.tryEmit("ক্যাটাগরি মুছে ফেলা হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "মুছে ফেলা যায়নি") }
        }
    }

    fun savePaymentMethod(
        existingId: String?,
        name: String,
        methodType: PaymentMethodType,
        openingBalance: Double,
        colorHex: Long
    ) {
        viewModelScope.launch {
            repository.savePaymentMethod(currentUserId, existingId, name, methodType, openingBalance, colorHex)
                .onSuccess { _events.tryEmit("পেমেন্ট মেথড সংরক্ষিত হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun deletePaymentMethod(methodId: String) {
        viewModelScope.launch {
            repository.deletePaymentMethod(currentUserId, methodId)
                .onSuccess { _events.tryEmit("পেমেন্ট মেথড মুছে ফেলা হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "মুছে ফেলা যায়নি") }
        }
    }

    fun saveBudget(category: CategoryRecord, monthKey: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.saveBudget(currentUserId, category.id, category.name, monthKey, limitAmount)
                .onSuccess { _events.tryEmit("${category.name} বাজেট সেট হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "বাজেট সেভ করা যায়নি") }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(currentUserId, budgetId)
                .onSuccess { _events.tryEmit("বাজেট মুছে ফেলা হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "মুছে ফেলা যায়নি") }
        }
    }

    fun saveSavingsGoal(
        existingId: String?,
        title: String,
        targetAmount: Double,
        savedAmount: Double,
        targetDateEpochMillis: Long,
        colorHex: Long
    ) {
        viewModelScope.launch {
            repository.saveSavingsGoal(
                currentUserId, existingId, title, targetAmount, savedAmount, targetDateEpochMillis, colorHex
            ).onSuccess { _events.tryEmit("সঞ্চয় লক্ষ্য সংরক্ষিত হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun addSavingsContribution(goal: SavingsGoalRecord, deltaAmount: Double) {
        viewModelScope.launch {
            repository.addSavingsContribution(currentUserId, goal, deltaAmount)
                .onSuccess { _events.tryEmit("সঞ্চয় আপডেট হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "আপডেট করা যায়নি") }
        }
    }

    fun deleteSavingsGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(currentUserId, goalId)
                .onSuccess { _events.tryEmit("সঞ্চয় লক্ষ্য মুছে ফেলা হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "মুছে ফেলা যায়নি") }
        }
    }

    fun saveDebt(
        existingId: String?,
        personName: String,
        phone: String,
        debtType: DebtType,
        amount: Double,
        paidAmount: Double,
        note: String,
        dueDateEpochMillis: Long
    ) {
        viewModelScope.launch {
            repository.saveDebt(
                currentUserId, existingId, personName, phone, debtType, amount, paidAmount, note, dueDateEpochMillis
            ).onSuccess { _events.tryEmit("দেনা-পাওনার হিসাব সংরক্ষিত হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun recordDebtPayment(debt: DebtRecord, paymentAmount: Double) {
        viewModelScope.launch {
            repository.recordDebtPayment(currentUserId, debt, paymentAmount)
                .onSuccess { _events.tryEmit("পরিশোধের তথ্য সংরক্ষিত হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "আপডেট করা যায়নি") }
        }
    }

    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            repository.deleteDebt(currentUserId, debtId)
                .onSuccess { _events.tryEmit("দেনা-পাওনা মুছে ফেলা হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "মুছে ফেলা যায়নি") }
        }
    }

    fun saveBillReminder(
        existingId: String?,
        title: String,
        amount: Double,
        dueDayOfMonth: Long,
        categoryName: String,
        recurringMonthly: Boolean,
        reminderEnabled: Boolean,
        lastPaidMonthKey: String
    ) {
        viewModelScope.launch {
            repository.saveBillReminder(
                currentUserId, existingId, title, amount, dueDayOfMonth, categoryName, recurringMonthly, reminderEnabled, lastPaidMonthKey
            ).onSuccess { _events.tryEmit("বিল রিমাইন্ডার সংরক্ষিত হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "সংরক্ষণ করা যায়নি") }
        }
    }

    fun markBillPaidThisMonth(bill: BillReminderRecord, alsoRecordExpense: Boolean, paymentMethod: PaymentMethodRecord?) {
        val monthKey = Formatters.currentMonthKey()
        viewModelScope.launch {
            repository.markBillPaidForMonth(currentUserId, bill.id, monthKey)
                .onSuccess {
                    if (alsoRecordExpense && paymentMethod != null) {
                        repository.saveTransaction(
                            userId = currentUserId,
                            existingId = null,
                            type = TransactionType.EXPENSE,
                            amount = bill.amount,
                            categoryId = "cat_bills",
                            categoryName = bill.categoryName.ifBlank { "বিল ও ইউটিলিটি" },
                            paymentMethodId = paymentMethod.id,
                            paymentMethodName = paymentMethod.name,
                            note = "বিল পরিশোধ: ${bill.title}",
                            dateEpochMillis = System.currentTimeMillis()
                        )
                    }
                    _events.tryEmit("${bill.title} পরিশোধিত হিসেবে চিহ্নিত হয়েছে")
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "আপডেট করা যায়নি") }
        }
    }

    fun deleteBillReminder(billId: String) {
        viewModelScope.launch {
            repository.deleteBillReminder(currentUserId, billId)
                .onSuccess { _events.tryEmit("বিল রিমাইন্ডার মুছে ফেলা হয়েছে") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "মুছে ফেলা যায়নি") }
        }
    }

    fun notifyMessage(message: String) {
        _events.tryEmit(message)
    }

    private data class CoreStreams(
        val transactions: UiState<List<TransactionRecord>>,
        val categories: UiState<List<CategoryRecord>>,
        val paymentMethods: UiState<List<PaymentMethodRecord>>,
        val budgets: UiState<List<BudgetRecord>>
    )

    private data class PlanStreams(
        val goals: UiState<List<SavingsGoalRecord>>,
        val debts: UiState<List<DebtRecord>>,
        val bills: UiState<List<BillReminderRecord>>,
        val hasPendingSync: Boolean
    )

    private companion object {
        const val TAG = "FinanceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
