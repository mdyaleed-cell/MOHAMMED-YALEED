package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.example.ui.theme.LocalLedgerColors
import com.example.ui.viewmodel.FinanceDashboardSnapshot
import com.example.util.Formatters

@Composable
fun TransactionsScreen(
    state: FinanceDashboardSnapshot,
    onEditTransaction: (TransactionRecord) -> Unit
) {
    val semantic = LocalLedgerColors.current
    var selectedMonthKey by remember { mutableStateOf(Formatters.currentMonthKey()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<TransactionType?>(null) }
    var filterMethodId by remember { mutableStateOf<String?>(null) }

    val filtered = remember(state.transactions, selectedMonthKey, searchQuery, filterType, filterMethodId) {
        state.transactions.filter { tx ->
            val matchesMonth = Formatters.monthKeyFromMillis(tx.dateEpochMillis) == selectedMonthKey
            val matchesType = filterType == null || tx.parsedType == filterType
            val matchesMethod = filterMethodId == null || tx.paymentMethodId == filterMethodId
            val matchesSearch = searchQuery.isBlank() ||
                tx.categoryName.contains(searchQuery, ignoreCase = true) ||
                tx.note.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMethodName.contains(searchQuery, ignoreCase = true)
            matchesMonth && matchesType && matchesMethod && matchesSearch
        }
    }

    val totalInc = remember(filtered) {
        filtered.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExp = remember(filtered) {
        filtered.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Header + Month Navigator
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "আয়-ব্যয় খাতা",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedMonthKey = Formatters.shiftMonthKey(selectedMonthKey, -1) },
                                modifier = Modifier.testTag("tx_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "আগের মাস")
                            }
                            Text(
                                text = Formatters.formatMonthDisplay(selectedMonthKey),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { selectedMonthKey = Formatters.shiftMonthKey(selectedMonthKey, 1) },
                                modifier = Modifier.testTag("tx_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরের মাস")
                            }
                        }
                    }

                    // Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ক্যাটাগরি, পেমেন্ট মেথড বা নোট খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "পরিষ্কার করুন")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tx_search_input")
                    )

                    // Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filterType == null,
                            onClick = { filterType = null },
                            label = { Text("সব") }
                        )
                        FilterChip(
                            selected = filterType == TransactionType.INCOME,
                            onClick = { filterType = TransactionType.INCOME },
                            label = { Text("আয়") }
                        )
                        FilterChip(
                            selected = filterType == TransactionType.EXPENSE,
                            onClick = { filterType = TransactionType.EXPENSE },
                            label = { Text("ব্যয়") }
                        )
                        state.paymentMethods.forEach { pm ->
                            FilterChip(
                                selected = filterMethodId == pm.id,
                                onClick = {
                                    filterMethodId = if (filterMethodId == pm.id) null else pm.id
                                },
                                label = { Text(pm.name) }
                            )
                        }
                    }

                    // Summary Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = semantic.incomeContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("মোট আয়", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    Formatters.formatTaka(totalInc),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = semantic.income,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Surface(
                            color = semantic.expenseContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("মোট ব্যয়", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    Formatters.formatTaka(totalExp),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = semantic.expense,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "এই ফিল্টারে কোনো হিসাব পাওয়া যায়নি",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "নিচের + বাটনে চাপ দিয়ে নতুন আয় বা ব্যয় যোগ করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { tx ->
                        TransactionRowCard(
                            transaction = tx,
                            onClick = { onEditTransaction(tx) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }
}
