package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryRecord
import com.example.data.model.PaymentMethodRecord
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.LocalLedgerColors
import com.example.util.CategoryIcons
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEntrySheet(
    initialType: TransactionType,
    editingTransaction: TransactionRecord?,
    categories: List<CategoryRecord>,
    paymentMethods: List<PaymentMethodRecord>,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        type: TransactionType,
        amount: Double,
        category: CategoryRecord,
        paymentMethod: PaymentMethodRecord,
        note: String,
        dateEpochMillis: Long
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = LocalLedgerColors.current

    var selectedType by remember(editingTransaction, initialType) {
        mutableStateOf(editingTransaction?.parsedType ?: initialType)
    }
    var amountText by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var noteText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }
    var dateMillis by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.dateEpochMillis ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.parsedType == selectedType }
    }

    var selectedCategoryId by remember(editingTransaction, selectedType, filteredCategories) {
        mutableStateOf(
            editingTransaction?.takeIf { it.parsedType == selectedType }?.categoryId
                ?: filteredCategories.firstOrNull()?.id.orEmpty()
        )
    }
    var selectedMethodId by remember(editingTransaction, paymentMethods) {
        mutableStateOf(
            editingTransaction?.paymentMethodId ?: paymentMethods.firstOrNull()?.id.orEmpty()
        )
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction == null) "নতুন হিসাব যোগ করুন" else "হিসাব সম্পাদনা",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (editingTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(editingTransaction.id) },
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "এন্ট্রি মুছুন",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Type Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { idx, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            selectedCategoryId = categories
                                .firstOrNull { it.parsedType == type }?.id.orEmpty()
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = idx,
                            count = TransactionType.entries.size
                        ),
                        modifier = Modifier.testTag("tx_type_${type.name.lowercase()}")
                    ) {
                        Text(type.labelBn, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                        errorMsg = null
                    }
                },
                label = { Text("টাকার পরিমাণ (৳)") },
                prefix = { Text("৳ ", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            // Quick amount suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("50", "100", "200", "500", "1000", "5000").forEach { preset ->
                    SuggestionChip(
                        onClick = {
                            amountText = preset
                            errorMsg = null
                        },
                        label = { Text("৳$preset") }
                    )
                }
            }

            // Date Picker row
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("tx_date_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("তারিখ: ${Formatters.formatDate(dateMillis)}")
            }

            // Category selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ক্যাটাগরি নির্বাচন করুন",
                    style = MaterialTheme.typography.titleMedium
                )
                if (filteredCategories.isEmpty()) {
                    Text(
                        text = "কোনো ক্যাটাগরি পাওয়া যায়নি। সেটিংস থেকে ক্যাটাগরি যোগ করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCategories.forEach { cat ->
                            val selected = cat.id == selectedCategoryId
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIcons.iconForKey(cat.iconKey),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else Color(cat.colorHex)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("cat_chip_${cat.id}")
                            )
                        }
                    }
                }
            }

            // Payment Method selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "পেমেন্ট মেথড (মাধ্যম)",
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { pm ->
                        val selected = pm.id == selectedMethodId
                        FilterChip(
                            selected = selected,
                            onClick = { selectedMethodId = pm.id },
                            label = { Text(pm.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIcons.iconForPaymentMethod(pm.parsedMethodType),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("pm_chip_${pm.id}")
                        )
                    }
                }
            }

            // Note
            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 200) noteText = it },
                label = { Text("বিবরণ / নোট (ঐচ্ছিক)") },
                placeholder = { Text("যেমন: দুপুরের খাবার, রিকশা ভাড়া, মাসিক বেতন...") },
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    val category = filteredCategories.firstOrNull { it.id == selectedCategoryId }
                    val method = paymentMethods.firstOrNull { it.id == selectedMethodId }
                    when {
                        amount == null || amount <= 0.0 -> errorMsg = "অনুগ্রহ করে সঠিক টাকার পরিমাণ লিখুন"
                        category == null -> errorMsg = "একটি ক্যাটাগরি নির্বাচন করুন"
                        method == null -> errorMsg = "একটি পেমেন্ট মেথড নির্বাচন করুন"
                        else -> {
                            onSave(
                                editingTransaction?.id,
                                selectedType,
                                amount,
                                category,
                                method,
                                noteText,
                                dateMillis
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TransactionType.INCOME) semantic.income
                    else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button")
            ) {
                Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingTransaction == null) "হিসাব সংরক্ষণ করুন" else "হালনাগাদ করুন",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
