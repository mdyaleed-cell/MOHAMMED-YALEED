package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BillReminderRecord
import com.example.data.model.CategoryRecord
import com.example.data.model.DebtRecord
import com.example.data.model.DebtStatus
import com.example.data.model.DebtType
import com.example.data.model.PaymentMethodRecord
import com.example.data.model.SavingsGoalRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.LocalLedgerColors
import com.example.ui.viewmodel.FinanceDashboardSnapshot
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.BillReminderScheduler
import com.example.util.CategoryIcons
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    state: FinanceDashboardSnapshot,
    selectedBudgetMonth: String,
    initialTab: Int,
    viewModel: FinanceViewModel
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = listOf("বাজেট", "সঞ্চয় লক্ষ্য", "দেনা-পাওনা", "বিল রিমাইন্ডার")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "পরিকল্পনা ও ব্যবস্থাপনা",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                    PrimaryScrollableTabRow(
                        selectedTabIndex = activeTab,
                        edgePadding = 12.dp
                    ) {
                        tabs.forEachIndexed { idx, title ->
                            Tab(
                                selected = activeTab == idx,
                                onClick = { activeTab = idx },
                                text = { Text(title, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("plan_tab_$idx")
                            )
                        }
                    }
                }
            }

            when (activeTab) {
                0 -> BudgetsTab(
                    state = state,
                    selectedMonth = selectedBudgetMonth,
                    onChangeMonth = { viewModel.setBudgetMonth(it) },
                    onSaveBudget = { cat, month, limit -> viewModel.saveBudget(cat, month, limit) },
                    onDeleteBudget = { viewModel.deleteBudget(it) }
                )
                1 -> SavingsGoalsTab(
                    goals = state.savingsGoals,
                    onSaveGoal = { id, title, target, saved, dateMillis, color ->
                        viewModel.saveSavingsGoal(id, title, target, saved, dateMillis, color)
                    },
                    onContribute = { goal, delta -> viewModel.addSavingsContribution(goal, delta) },
                    onDelete = { viewModel.deleteSavingsGoal(it) }
                )
                2 -> DebtsTab(
                    debts = state.debts,
                    onSaveDebt = { id, name, phone, type, amount, paid, note, due ->
                        viewModel.saveDebt(id, name, phone, type, amount, paid, note, due)
                    },
                    onRecordPayment = { debt, pay -> viewModel.recordDebtPayment(debt, pay) },
                    onDelete = { viewModel.deleteDebt(it) }
                )
                3 -> BillsTab(
                    bills = state.billReminders,
                    paymentMethods = state.paymentMethods,
                    onSaveBill = { id, title, amt, day, cat, rec, rem, lastPaid ->
                        viewModel.saveBillReminder(id, title, amt, day, cat, rec, rem, lastPaid)
                    },
                    onMarkPaid = { bill, logTx, pm -> viewModel.markBillPaidThisMonth(bill, logTx, pm) },
                    onDelete = { viewModel.deleteBillReminder(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetsTab(
    state: FinanceDashboardSnapshot,
    selectedMonth: String,
    onChangeMonth: (String) -> Unit,
    onSaveBudget: (CategoryRecord, String, Double) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    val semantic = LocalLedgerColors.current
    val expenseCategories = remember(state.categories) {
        state.categories.filter { it.parsedType == TransactionType.EXPENSE }
    }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var expanded by remember { mutableStateOf(false) }
        var chosenCat by remember(expenseCategories) { mutableStateOf(expenseCategories.firstOrNull()) }
        var limitText by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("মাসিক বাজেট সেট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = chosenCat?.name ?: "ক্যাটাগরি নেই",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("খরচের ক্যাটাগরি") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            expenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        chosenCat = cat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) limitText = it },
                        label = { Text("মাসিক সীমা (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_limit_input")
                    )
                    if (err != null) {
                        Text(err!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = limitText.toDoubleOrNull()
                        val cat = chosenCat
                        when {
                            cat == null -> err = "ক্যাটাগরি নির্বাচন করুন"
                            amt == null || amt <= 0.0 -> err = "সঠিক বাজেট সীমা লিখুন"
                            else -> {
                                onSaveBudget(cat, selectedMonth, amt)
                                showAddDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_budget_button")
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("বাতিল") }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onChangeMonth(Formatters.shiftMonthKey(selectedMonth, -1)) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "আগের মাস")
                    }
                    Text(
                        text = Formatters.formatMonthDisplay(selectedMonth),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onChangeMonth(Formatters.shiftMonthKey(selectedMonth, 1)) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের মাস")
                    }
                }
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_budget_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("বাজেট সেট")
                }
            }
        }

        if (state.currentMonthBudgetStatuses.isEmpty()) {
            item {
                EmptyPlanCard(
                    title = "এই মাসে কোনো বাজেট সেট করা হয়নি",
                    subtitle = "খাবার, যাতায়াত বা বাজারের মাসিক সীমা নির্ধারণ করলে খরচ বেশি হওয়ার আগেই সতর্কবার্তা পাবেন।"
                )
            }
        } else {
            items(state.currentMonthBudgetStatuses, key = { it.budget.id }) { status ->
                val barColor = when {
                    status.exceeded -> semantic.expense
                    status.nearLimit -> semantic.warning
                    else -> MaterialTheme.colorScheme.primary
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (status.exceeded || status.nearLimit) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = "সতর্কতা",
                                        tint = barColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = status.budget.categoryName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { onDeleteBudget(status.budget.id) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "বাজেট মুছুন",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { status.ratio.coerceIn(0f, 1f) },
                            color = barColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "খরচ: ${Formatters.formatTaka(status.spentAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = barColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "সীমা: ${Formatters.formatTaka(status.budget.limitAmount)} (${(status.ratio * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (status.exceeded) {
                            Text(
                                text = "সতর্কবার্তা: সীমার চেয়ে ${Formatters.formatTaka(status.spentAmount - status.budget.limitAmount)} বেশি খরচ হয়েছে!",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.expense,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (status.nearLimit) {
                            Text(
                                text = "সতর্কবার্তা: বাজেটের ৮০% এর বেশি খরচ হয়ে গেছে।",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.warning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun SavingsGoalsTab(
    goals: List<SavingsGoalRecord>,
    onSaveGoal: (String?, String, Double, Double, Long, Long) -> Unit,
    onContribute: (SavingsGoalRecord, Double) -> Unit,
    onDelete: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalRecord?>(null) }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        var initialSaved by remember { mutableStateOf("0") }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("নতুন সঞ্চয় লক্ষ্য তৈরি করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
                        label = { Text("লক্ষ্যের নাম (যেমন: জরুরি তহবিল)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                    )
                    OutlinedTextField(
                        value = target,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) target = it },
                        label = { Text("লক্ষ্যমাত্রা (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("goal_target_input")
                    )
                    OutlinedTextField(
                        value = initialSaved,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) initialSaved = it },
                        label = { Text("বর্তমান জমা (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = target.toDoubleOrNull()
                        val s = initialSaved.toDoubleOrNull() ?: 0.0
                        when {
                            title.isBlank() -> err = "লক্ষ্যের নাম লিখুন"
                            t == null || t <= 0.0 -> err = "সঠিক লক্ষ্যমাত্রা দিন"
                            else -> {
                                val sixMonthsLater = System.currentTimeMillis() + 180L * 86_400_000L
                                onSaveGoal(null, title, t, s, sixMonthsLater, CategoryIcons.presetColors.first())
                                showCreateDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_goal_button")
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("বাতিল") } }
        )
    }

    if (contributingGoal != null) {
        val goal = contributingGoal!!
        var addText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { contributingGoal = null },
            title = { Text("${goal.title} - টাকা জমা") },
            text = {
                OutlinedTextField(
                    value = addText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) addText = it },
                    label = { Text("কত টাকা জমা করবেন? (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_deposit_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = addText.toDoubleOrNull()
                        if (amt != null && amt > 0.0) {
                            onContribute(goal, amt)
                            contributingGoal = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_goal_deposit_button")
                ) { Text("জমা করুন") }
            },
            dismissButton = { TextButton(onClick = { contributingGoal = null }) { Text("বাতিল") } }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("আপনার সঞ্চয় পরিকল্পনা", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("add_goal_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("নতুন লক্ষ্য")
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                EmptyPlanCard(
                    title = "কোনো সঞ্চয় লক্ষ্য তৈরি করা হয়নি",
                    subtitle = "জরুরি তহবিল, ভ্রমণ বা ভবিষ্যতের বড় কেনাকাটার জন্য সঞ্চয়ের লক্ষ্য নির্ধারণ করুন।"
                )
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                val ratio = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount).toFloat() else 0f
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Savings,
                                    contentDescription = null,
                                    tint = Color(goal.colorHex)
                                )
                                Column {
                                    Text(goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "অর্জিত: ${(ratio * 100).toInt().coerceAtMost(100)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onDelete(goal.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "মুছুন")
                            }
                        }

                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${Formatters.formatTaka(goal.savedAmount)} / ${Formatters.formatTaka(goal.targetAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedButton(onClick = { contributingGoal = goal }) {
                                Text("+ টাকা জমা")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtsTab(
    debts: List<DebtRecord>,
    onSaveDebt: (String?, String, String, DebtType, Double, Double, String, Long) -> Unit,
    onRecordPayment: (DebtRecord, Double) -> Unit,
    onDelete: (String) -> Unit
) {
    val semantic = LocalLedgerColors.current
    var filterType by remember { mutableStateOf<DebtType?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<DebtRecord?>(null) }

    val filtered = remember(debts, filterType) {
        debts.filter { filterType == null || it.parsedDebtType == filterType }
            .sortedBy { it.parsedStatus == DebtStatus.SETTLED }
    }

    if (showCreateDialog) {
        var personName by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var debtType by remember { mutableStateOf(DebtType.LENT) }
        var amountText by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var dueMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 7L * 86_400_000L) }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("নতুন দেনা-পাওনা যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        DebtType.entries.forEachIndexed { i, dt ->
                            SegmentedButton(
                                selected = debtType == dt,
                                onClick = { debtType = dt },
                                shape = SegmentedButtonDefaults.itemShape(i, DebtType.entries.size)
                            ) {
                                Text(if (dt == DebtType.LENT) "পাওনা" else "দেনা")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { if (it.length <= 80) personName = it },
                        label = { Text("ব্যক্তির নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("debt_person_input")
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = it },
                        label = { Text("টাকার পরিমাণ (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("debt_amount_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 30) phone = it },
                        label = { Text("ফোন নম্বর (ঐচ্ছিক)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= 200) note = it },
                        label = { Text("নোট (ঐচ্ছিক)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        when {
                            personName.isBlank() -> err = "ব্যক্তির নাম লিখুন"
                            amt == null || amt <= 0.0 -> err = "সঠিক টাকার পরিমাণ লিখুন"
                            else -> {
                                onSaveDebt(null, personName, phone, debtType, amt, 0.0, note, dueMillis)
                                showCreateDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_debt_button")
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("বাতিল") } }
        )
    }

    if (payingDebt != null) {
        val debt = payingDebt!!
        var payText by remember {
            mutableStateOf(
                if (debt.remainingAmount % 1.0 == 0.0) debt.remainingAmount.toLong().toString()
                else debt.remainingAmount.toString()
            )
        }
        AlertDialog(
            onDismissRequest = { payingDebt = null },
            title = { Text("${debt.personName} - পরিশোধ / আদায়") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("বাকি আছে: ${Formatters.formatTaka(debt.remainingAmount)}")
                    OutlinedTextField(
                        value = payText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) payText = it },
                        label = { Text("পরিশোধিত পরিমাণ (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = payText.toDoubleOrNull()
                    if (amt != null && amt > 0.0) {
                        onRecordPayment(debt, amt)
                        payingDebt = null
                    }
                }) { Text("নিশ্চিত করুন") }
            },
            dismissButton = { TextButton(onClick = { payingDebt = null }) { Text("বাতিল") } }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("সব") })
                    FilterChip(selected = filterType == DebtType.LENT, onClick = { filterType = DebtType.LENT }, label = { Text("পাওনা") })
                    FilterChip(selected = filterType == DebtType.BORROWED, onClick = { filterType = DebtType.BORROWED }, label = { Text("দেনা") })
                }
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.testTag("add_debt_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("নতুন")
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyPlanCard(
                    title = "দেনা-পাওনার কোনো হিসাব নেই",
                    subtitle = "কাকে কত টাকা ধার দিয়েছেন বা কার কাছ থেকে নিয়েছেন, তা পরিশোধের তারিখসহ এখানে লিখে রাখুন।"
                )
            }
        } else {
            items(filtered, key = { it.id }) { debt ->
                val isLent = debt.parsedDebtType == DebtType.LENT
                val settled = debt.parsedStatus == DebtStatus.SETTLED
                val accent = if (settled) MaterialTheme.colorScheme.primary else if (isLent) semantic.income else semantic.expense

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = debt.personName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${debt.parsedDebtType.labelBn} • ${debt.parsedStatus.labelBn}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                            Text(
                                text = Formatters.formatTaka(debt.remainingAmount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }

                        if (debt.note.isNotBlank() || debt.phone.isNotBlank()) {
                            Text(
                                text = listOf(debt.phone, debt.note).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "মোট: ${Formatters.formatTaka(debt.amount)} | পরিশোধ: ${Formatters.formatTaka(debt.paidAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                if (!settled) {
                                    TextButton(onClick = { payingDebt = debt }) {
                                        Text("পরিশোধ")
                                    }
                                }
                                IconButton(onClick = { onDelete(debt.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "মুছুন")
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun BillsTab(
    bills: List<BillReminderRecord>,
    paymentMethods: List<PaymentMethodRecord>,
    onSaveBill: (String?, String, Double, Long, String, Boolean, Boolean, String) -> Unit,
    onMarkPaid: (BillReminderRecord, Boolean, PaymentMethodRecord?) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    val currentMonthKey = Formatters.currentMonthKey()
    var showAddDialog by remember { mutableStateOf(false) }
    var billToPay by remember { mutableStateOf<BillReminderRecord?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var dayText by remember { mutableStateOf("10") }
        var reminderOn by remember { mutableStateOf(true) }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("নতুন বিল রিমাইন্ডার") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 80) title = it },
                        label = { Text("বিলের নাম (বিদ্যুৎ, গ্যাস, ইন্টারনেট...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bill_title_input")
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amountText = it },
                        label = { Text("আনুমানিক পরিমাণ (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bill_amount_input")
                    )
                    OutlinedTextField(
                        value = dayText,
                        onValueChange = { if (it.length <= 2 && it.all(Char::isDigit)) dayText = it },
                        label = { Text("প্রতি মাসের কত তারিখ (১-৩১)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("bill_day_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("নোটিফিকেশন অ্যালার্ট চালু রাখুন")
                        Switch(checked = reminderOn, onCheckedChange = { reminderOn = it })
                    }
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull()
                        val day = dayText.toLongOrNull()
                        when {
                            title.isBlank() -> err = "বিলের নাম লিখুন"
                            amt == null || amt <= 0.0 -> err = "সঠিক টাকার পরিমাণ লিখুন"
                            day == null || day !in 1L..31L -> err = "১ থেকে ৩১ এর মধ্যে তারিখ দিন"
                            else -> {
                                if (reminderOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                if (reminderOn) {
                                    BillReminderScheduler.scheduleMonthlyBillReminder(
                                        context = context,
                                        billId = "${title}_${day}",
                                        title = title,
                                        amount = amt,
                                        dueDayOfMonth = day.toInt()
                                    )
                                }
                                onSaveBill(null, title, amt, day, "বিল ও ইউটিলিটি", true, reminderOn, "")
                                showAddDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_bill_button")
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("বাতিল") } }
        )
    }

    if (billToPay != null) {
        val bill = billToPay!!
        var alsoRecordExpense by remember { mutableStateOf(true) }
        var selectedMethod by remember(paymentMethods) { mutableStateOf(paymentMethods.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { billToPay = null },
            title = { Text("${bill.title} পরিশোধ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("পরিমাণ: ${Formatters.formatTaka(bill.amount)}")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { alsoRecordExpense = !alsoRecordExpense }
                    ) {
                        Checkbox(checked = alsoRecordExpense, onCheckedChange = { alsoRecordExpense = it })
                        Text("ব্যয়ের খাতায় স্বয়ংক্রিয়ভাবে যোগ করুন")
                    }
                    if (alsoRecordExpense && paymentMethods.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            paymentMethods.take(3).forEach { pm ->
                                FilterChip(
                                    selected = selectedMethod?.id == pm.id,
                                    onClick = { selectedMethod = pm },
                                    label = { Text(pm.name) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onMarkPaid(bill, alsoRecordExpense, selectedMethod)
                    billToPay = null
                }) { Text("পরিশোধিত নিশ্চিত করুন") }
            },
            dismissButton = { TextButton(onClick = { billToPay = null }) { Text("বাতিল") } }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("মাসিক বিল ও রিমাইন্ডার", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_bill_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("বিল যোগ")
                }
            }
        }

        if (bills.isEmpty()) {
            item {
                EmptyPlanCard(
                    title = "কোনো বিল রিমাইন্ডার সেট করা নেই",
                    subtitle = "বিদ্যুৎ, গ্যাস, ইন্টারনেট বা বাসা ভাড়ার তারিখ যুক্ত করলে প্রতি মাসে সময়মতো রিমাইন্ডার পাবেন।"
                )
            }
        } else {
            items(bills, key = { it.id }) { bill ->
                val paidThisMonth = bill.lastPaidMonthKey == currentMonthKey
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (paidThisMonth) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = if (paidThisMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bill.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "মাসের ${bill.dueDayOfMonth} তারিখ • ${Formatters.formatTaka(bill.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (paidThisMonth) "এই মাসে পরিশোধিত" else "এখনো পরিশোধ বাকি",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (paidThisMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!paidThisMonth) {
                            OutlinedButton(onClick = { billToPay = bill }) {
                                Text("পরিশোধ")
                            }
                        }
                        IconButton(onClick = { onDelete(bill.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "বিল মুছুন")
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun EmptyPlanCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
