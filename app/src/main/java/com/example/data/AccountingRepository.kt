package com.example.data

import com.example.data.model.BillReminderRecord
import com.example.data.model.BudgetRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.DebtRecord
import com.example.data.model.DebtType
import com.example.data.model.PaymentMethodRecord
import com.example.data.model.PaymentMethodType
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface AccountingRepository {
    fun observeTransactions(userId: String): Flow<List<TransactionRecord>>
    fun observeCategories(userId: String): Flow<List<CategoryRecord>>
    fun observePaymentMethods(userId: String): Flow<List<PaymentMethodRecord>>
    fun observeBudgets(userId: String): Flow<List<BudgetRecord>>
    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoalRecord>>
    fun observeDebts(userId: String): Flow<List<DebtRecord>>
    fun observeBillReminders(userId: String): Flow<List<BillReminderRecord>>
    fun observeHasPendingSync(userId: String): Flow<Boolean>

    suspend fun ensureDefaultSetup(userId: String): Result<Unit>

    suspend fun saveTransaction(
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
    ): Result<Unit>

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit>

    suspend fun saveCategory(
        userId: String,
        existingId: String?,
        name: String,
        type: TransactionType,
        iconKey: String,
        colorHex: Long
    ): Result<Unit>

    suspend fun deleteCategory(userId: String, categoryId: String): Result<Unit>

    suspend fun savePaymentMethod(
        userId: String,
        existingId: String?,
        name: String,
        methodType: PaymentMethodType,
        openingBalance: Double,
        colorHex: Long
    ): Result<Unit>

    suspend fun deletePaymentMethod(userId: String, methodId: String): Result<Unit>

    suspend fun saveBudget(
        userId: String,
        categoryId: String,
        categoryName: String,
        monthKey: String,
        limitAmount: Double
    ): Result<Unit>

    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit>

    suspend fun saveSavingsGoal(
        userId: String,
        existingId: String?,
        title: String,
        targetAmount: Double,
        savedAmount: Double,
        targetDateEpochMillis: Long,
        colorHex: Long
    ): Result<Unit>

    suspend fun addSavingsContribution(
        userId: String,
        goal: SavingsGoalRecord,
        deltaAmount: Double
    ): Result<Unit>

    suspend fun deleteSavingsGoal(userId: String, goalId: String): Result<Unit>

    suspend fun saveDebt(
        userId: String,
        existingId: String?,
        personName: String,
        phone: String,
        debtType: DebtType,
        amount: Double,
        paidAmount: Double,
        note: String,
        dueDateEpochMillis: Long
    ): Result<Unit>

    suspend fun recordDebtPayment(
        userId: String,
        debt: DebtRecord,
        paymentAmount: Double
    ): Result<Unit>

    suspend fun deleteDebt(userId: String, debtId: String): Result<Unit>

    suspend fun saveBillReminder(
        userId: String,
        existingId: String?,
        title: String,
        amount: Double,
        dueDayOfMonth: Long,
        categoryName: String,
        recurringMonthly: Boolean,
        reminderEnabled: Boolean,
        lastPaidMonthKey: String
    ): Result<Unit>

    suspend fun markBillPaidForMonth(
        userId: String,
        billId: String,
        monthKey: String
    ): Result<Unit>

    suspend fun deleteBillReminder(userId: String, billId: String): Result<Unit>
}
