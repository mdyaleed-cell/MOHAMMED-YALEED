package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentMethodType
import com.example.data.model.TransactionType
import com.example.ui.viewmodel.FinanceDashboardSnapshot
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.CategoryIcons
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageCategoriesAndWalletsScreen(
    state: FinanceDashboardSnapshot,
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddCategory by remember { mutableStateOf(false) }
    var showAddWallet by remember { mutableStateOf(false) }

    if (showAddCategory) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(TransactionType.EXPENSE) }
        var iconKey by remember { mutableStateOf("category") }
        var colorHex by remember { mutableLongStateOf(CategoryIcons.presetColors.first()) }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text("নতুন ক্যাটাগরি যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TransactionType.entries.forEachIndexed { idx, t ->
                            SegmentedButton(
                                selected = type == t,
                                onClick = { type = t },
                                shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size)
                            ) { Text(t.labelBn) }
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 50) name = it },
                        label = { Text("ক্যাটাগরির নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_category_name_input")
                    )
                    Text("আইকন বেছে নিন", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.availableIcons.forEach { (key, vec) ->
                            val selected = iconKey == key
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { iconKey = key }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(vec, contentDescription = key, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    Text("রং বেছে নিন", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.presetColors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (colorHex == c) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = c }
                            )
                        }
                    }
                    if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) err = "ক্যাটাগরির নাম দিন"
                        else {
                            viewModel.saveCategory(null, name, type, iconKey, colorHex)
                            showAddCategory = false
                        }
                    },
                    modifier = Modifier.testTag("save_category_button")
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = { TextButton(onClick = { showAddCategory = false }) { Text("বাতিল") } }
        )
    }

    if (showAddWallet) {
        var name by remember { mutableStateOf("") }
        var methodType by remember { mutableStateOf(PaymentMethodType.MOBILE_BANKING) }
        var openingText by remember { mutableStateOf("0") }
        var colorHex by remember { mutableLongStateOf(CategoryIcons.presetColors[1]) }
        var err by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddWallet = false },
            title = { Text("নতুন পেমেন্ট মেথড / অ্যাকাউন্ট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 50) name = it },
                        label = { Text("নাম (যেমন: বিকাশ, রকেট, ডাচ-বাংলা)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_wallet_name_input")
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PaymentMethodType.entries.forEachIndexed { idx, mt ->
                            SegmentedButton(
                                selected = methodType == mt,
                                onClick = { methodType = mt },
                                shape = SegmentedButtonDefaults.itemShape(idx, PaymentMethodType.entries.size)
                            ) {
                                Text(
                                    when (mt) {
                                        PaymentMethodType.CASH -> "নগদ"
                                        PaymentMethodType.BANK -> "ব্যাংক"
                                        PaymentMethodType.MOBILE_BANKING -> "মোবাইল"
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = openingText,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) openingText = it },
                        label = { Text("শুরুর ব্যালেন্স (৳)") },
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
                        val opening = openingText.toDoubleOrNull() ?: 0.0
                        if (name.isBlank()) err = "অ্যাকাউন্টের নাম লিখুন"
                        else {
                            viewModel.savePaymentMethod(null, name, methodType, opening, colorHex)
                            showAddWallet = false
                        }
                    },
                    modifier = Modifier.testTag("save_wallet_button")
                ) { Text("সংরক্ষণ করুন") }
            },
            dismissButton = { TextButton(onClick = { showAddWallet = false }) { Text("বাতিল") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("ক্যাটাগরি ও পেমেন্ট মেথড", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("manage_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "ফিরে যান")
                    }
                }
            )

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("ক্যাটাগরি (${state.categories.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("পেমেন্ট মেথড (${state.paymentMethods.size})") }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddCategory = true },
                            modifier = Modifier.fillMaxWidth().testTag("open_add_category_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("নতুন ক্যাটাগরি তৈরি করুন")
                        }
                    }
                    items(state.categories, key = { it.id }) { cat ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = Color(cat.colorHex).copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIcons.iconForKey(cat.iconKey),
                                            contentDescription = cat.name,
                                            tint = Color(cat.colorHex)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        cat.parsedType.labelBn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "মুছুন")
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddWallet = true },
                            modifier = Modifier.fillMaxWidth().testTag("open_add_wallet_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("নতুন পেমেন্ট মেথড যুক্ত করুন")
                        }
                    }
                    items(state.walletBalances, key = { it.method.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = Color(item.method.colorHex).copy(alpha = 0.16f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = CategoryIcons.iconForPaymentMethod(item.method.parsedMethodType),
                                            contentDescription = item.method.name,
                                            tint = Color(item.method.colorHex)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.method.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${item.method.parsedMethodType.labelBn} • বর্তমান: ${Formatters.formatTaka(item.currentBalance)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deletePaymentMethod(item.method.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "মুছুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
