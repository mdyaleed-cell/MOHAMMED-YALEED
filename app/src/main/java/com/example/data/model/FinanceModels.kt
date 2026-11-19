package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType(val labelBn: String) {
    INCOME("আয়"),
    EXPENSE("ব্যয়");

    companion object {
        fun fromRaw(raw: String): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class PaymentMethodType(val labelBn: String) {
    CASH("নগদ টাকা"),
    BANK("ব্যাংক অ্যাকাউন্ট"),
    MOBILE_BANKING("মোবাইল ব্যাংকিং");

    companion object {
        fun fromRaw(raw: String): PaymentMethodType =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

enum class DebtType(val labelBn: String) {
    LENT("পাওনা (ধার দিয়েছি)"),
    BORROWED("দেনা (ধার নিয়েছি)");

    companion object {
        fun fromRaw(raw: String): DebtType =
            entries.firstOrNull { it.name == raw } ?: LENT
    }
}

enum class DebtStatus(val labelBn: String) {
    ACTIVE("বাকি আছে"),
    SETTLED("পরিশোধিত");

    companion object {
        fun fromRaw(raw: String): DebtStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double = 0.0,
    val categoryId: String = "",
    val categoryName: String = "",
    val paymentMethodId: String = "",
    val paymentMethodName: String = "",
    val note: String = "",
    val dateEpochMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: TransactionType get() = TransactionType.fromRaw(type)
}

@Keep
@IgnoreExtraProperties
data class CategoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val iconKey: String = "category",
    val colorHex: Long = 0xFF085B4B,
    val defaultCategory: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedType: TransactionType get() = TransactionType.fromRaw(type)
}

@Keep
@IgnoreExtraProperties
data class PaymentMethodRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val methodType: String = PaymentMethodType.CASH.name,
    val openingBalance: Double = 0.0,
    val colorHex: Long = 0xFF085B4B,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedMethodType: PaymentMethodType get() = PaymentMethodType.fromRaw(methodType)
}

@Keep
@IgnoreExtraProperties
data class BudgetRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val monthKey: String = "",
    val limitAmount: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavingsGoalRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val savedAmount: Double = 0.0,
    val targetDateEpochMillis: Long = 0L,
    val colorHex: Long = 0xFF085B4B,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DebtRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val personName: String = "",
    val phone: String = "",
    val debtType: String = DebtType.LENT.name,
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val note: String = "",
    val dueDateEpochMillis: Long = 0L,
    val status: String = DebtStatus.ACTIVE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val parsedDebtType: DebtType get() = DebtType.fromRaw(debtType)
    val parsedStatus: DebtStatus get() = DebtStatus.fromRaw(status)
    val remainingAmount: Double get() = (amount - paidAmount).coerceAtLeast(0.0)
}

@Keep
@IgnoreExtraProperties
data class BillReminderRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val dueDayOfMonth: Long = 1L,
    val categoryName: String = "",
    val recurringMonthly: Boolean = true,
    val reminderEnabled: Boolean = true,
    val lastPaidMonthKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class BudgetStatusItem(
    val budget: BudgetRecord,
    val spentAmount: Double,
    val ratio: Float,
    val exceeded: Boolean,
    val nearLimit: Boolean
)

data class WalletBalanceItem(
    val method: PaymentMethodRecord,
    val currentBalance: Double,
    val totalIn: Double,
    val totalOut: Double
)
