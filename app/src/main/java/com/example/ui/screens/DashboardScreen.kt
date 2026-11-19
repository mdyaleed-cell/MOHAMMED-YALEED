package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.data.model.WalletBalanceItem
import com.example.ui.theme.LocalLedgerColors
import com.example.ui.viewmodel.FinanceDashboardSnapshot
import com.example.util.CategoryIcons
import com.example.util.Formatters

@Composable
fun DashboardScreen(
    state: FinanceDashboardSnapshot,
    isOnline: Boolean,
    isGuestMode: Boolean = false,
    onSwitchToGoogleSignIn: () -> Unit = {},
    onQuickAdd: (TransactionType) -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenPlan: (Int) -> Unit,
    onOpenManageCategoriesAndWallets: () -> Unit
) {
    val semantic = LocalLedgerColors.current
    val budgetAlerts = state.currentMonthBudgetStatuses.filter { it.exceeded || it.nearLimit }

    if (state.isLoading && state.transactions.isEmpty() && state.paymentMethods.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Greeting & Cloud Sync Status Pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "হিসাব খাতা",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = Formatters.formatMonthDisplay(Formatters.currentMonthKey()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SyncStatusBadge(
                        isOnline = isOnline,
                        hasPendingSync = state.hasPendingSync,
                        isGuestMode = isGuestMode
                    )
                }
            }

            if (state.errorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Hero Emerald Balance Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF06483B),
                                        Color(0xFF0A6B58),
                                        Color(0xFF12806A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "মোট অবশিষ্ট ব্যালেন্স",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.82f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Formatters.formatTaka(state.totalNetBalance),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("hero_total_balance")
                                    )
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD57E),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HeroSummaryTile(
                                    label = "এই মাসের আয়",
                                    amount = state.currentMonthIncome,
                                    icon = Icons.Rounded.ArrowDownward,
                                    accentColor = Color(0xFF7BF2BE),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroSummaryTile(
                                    label = "এই মাসের ব্যয়",
                                    amount = state.currentMonthExpense,
                                    icon = Icons.Rounded.ArrowUpward,
                                    accentColor = Color(0xFFFFA5A5),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Today's strip
                            Surface(
                                color = Color.Black.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "আজকের আয়: ${Formatters.formatTaka(state.todayIncome)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "আজকের ব্যয়: ${Formatters.formatTaka(state.todayExpense)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        label = "আয় যোগ",
                        icon = Icons.Rounded.Add,
                        containerColor = semantic.incomeContainer,
                        contentColor = semantic.income,
                        onClick = { onQuickAdd(TransactionType.INCOME) },
                        testTag = "quick_add_income",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        label = "ব্যয় যোগ",
                        icon = Icons.Rounded.Remove,
                        containerColor = semantic.expenseContainer,
                        contentColor = semantic.expense,
                        onClick = { onQuickAdd(TransactionType.EXPENSE) },
                        testTag = "quick_add_expense",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        label = "দেনা-পাওনা",
                        icon = Icons.Rounded.SwapHoriz,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = { onOpenPlan(2) },
                        testTag = "quick_open_debts",
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        label = "বিল",
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { onOpenPlan(3) },
                        testTag = "quick_open_bills",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Budget Alerts & Unpaid Bills Banner
            if (budgetAlerts.isNotEmpty() || state.unpaidBillsThisMonth.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        budgetAlerts.take(2).forEach { alert ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (alert.exceeded) semantic.expenseContainer
                                    else semantic.warningContainer
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPlan(0) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = "বাজেট সতর্কতা",
                                        tint = if (alert.exceeded) semantic.expense else semantic.warning
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (alert.exceeded)
                                                "${alert.budget.categoryName} বাজেট সীমা অতিক্রম করেছে!"
                                            else
                                                "${alert.budget.categoryName} বাজেটের ${(alert.ratio * 100).toInt()}% খরচ হয়েছে",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { alert.ratio.coerceIn(0f, 1f) },
                                            color = if (alert.exceeded) semantic.expense else semantic.warning,
                                            trackColor = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                    }
                                    Text(
                                        text = "${Formatters.formatTaka(alert.spentAmount)} / ${Formatters.formatTaka(alert.budget.limitAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (state.unpaidBillsThisMonth.isNotEmpty()) {
                            val nextBill = state.unpaidBillsThisMonth.first()
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPlan(3) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = "বকেয়া বিল",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "এই মাসে ${state.unpaidBillsThisMonth.size}টি বিল বাকি আছে",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "${nextBill.title}: ${Formatters.formatTaka(nextBill.amount)} (মাসের ${nextBill.dueDayOfMonth} তারিখ)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment Methods / Wallets Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পেমেন্ট মেথড ও ওয়ালেট",
                            style = MaterialTheme.typography.titleLarge
                        )
                        TextButton(
                            onClick = onOpenManageCategoriesAndWallets,
                            modifier = Modifier.testTag("manage_wallets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পরিচালনা")
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.walletBalances, key = { it.method.id }) { item ->
                            WalletBalanceCard(item = item)
                        }
                    }
                }
            }

            // Debts Quick Snapshot
            if (state.totalLentActive > 0.0 || state.totalBorrowedActive > 0.0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenPlan(2) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "মোট পাওনা (পাবো)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatTaka(state.totalLentActive),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = semantic.income,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenPlan(2) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "মোট দেনা (দেবো)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatTaka(state.totalBorrowedActive),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = semantic.expense,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সাম্প্রতিক লেনদেন",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onOpenTransactions) {
                        Text("সব দেখুন")
                    }
                }
            }

            if (state.transactions.isEmpty()) {
                item {
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
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "এখনো কোনো আয় বা ব্যয় যোগ করা হয়নি",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "উপরের 'আয় যোগ' বা 'ব্যয় যোগ' বাটনে চাপ দিয়ে আজকের প্রথম হিসাব লিখুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.transactions.take(8), key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        onClick = { onEditTransaction(tx) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
fun SyncStatusBadge(
    isOnline: Boolean,
    hasPendingSync: Boolean,
    isGuestMode: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val label = when {
        isGuestMode -> "লোকাল মোড"
        !isOnline -> "অফলাইন মোড"
        hasPendingSync -> "সিঙ্ক হচ্ছে..."
        else -> "ক্লাউড সিঙ্কড"
    }
    val container = when {
        isGuestMode -> MaterialTheme.colorScheme.secondaryContainer
        !isOnline -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when {
        isGuestMode -> MaterialTheme.colorScheme.onSecondaryContainer
        !isOnline -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("sync_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isOnline && !hasPendingSync) Icons.Rounded.CloudDone else Icons.Rounded.CloudQueue,
                contentDescription = label,
                tint = content,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HeroSummaryTile(
    label: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.13f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.22f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = Formatters.formatTaka(amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = containerColor,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WalletBalanceCard(item: WalletBalanceItem) {
    val accent = Color(item.method.colorHex)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.width(156.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = CategoryIcons.iconForPaymentMethod(item.method.parsedMethodType),
                            contentDescription = item.method.name,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = item.method.name,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = Formatters.formatTaka(item.currentBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: TransactionRecord,
    onClick: () -> Unit
) {
    val semantic = LocalLedgerColors.current
    val isIncome = transaction.parsedType == TransactionType.INCOME
    val amountColor = if (isIncome) semantic.income else semantic.expense
    val badgeBg = if (isIncome) semantic.incomeContainer else semantic.expenseContainer

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("tx_card_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = badgeBg,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = transaction.parsedType.labelBn,
                        tint = amountColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = buildString {
                    append(transaction.paymentMethodName)
                    append(" • ")
                    append(Formatters.formatDate(transaction.dateEpochMillis))
                }
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = Formatters.formatTaka(
                    if (isIncome) transaction.amount else -transaction.amount,
                    showSign = true
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
