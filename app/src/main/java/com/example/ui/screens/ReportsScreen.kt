package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TransactionType
import com.example.ui.theme.LocalLedgerColors
import com.example.ui.viewmodel.FinanceDashboardSnapshot
import com.example.ui.viewmodel.ReportPeriod
import com.example.util.Formatters
import com.example.util.ReportExporter
import java.util.Calendar

data class CategorySlice(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

data class BarComparisonPoint(
    val label: String,
    val income: Double,
    val expense: Double
)

@Composable
fun ReportsScreen(
    state: FinanceDashboardSnapshot,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val semantic = LocalLedgerColors.current

    var period by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    var selectedDayMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedMonthKey by remember { mutableStateOf(Formatters.currentMonthKey()) }
    var selectedYear by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))
    }

    val periodLabel = when (period) {
        ReportPeriod.DAILY -> Formatters.formatDate(selectedDayMillis)
        ReportPeriod.MONTHLY -> Formatters.formatMonthDisplay(selectedMonthKey)
        ReportPeriod.YEARLY -> "বছর $selectedYear"
    }

    val filteredTx = remember(state.transactions, period, selectedDayMillis, selectedMonthKey, selectedYear) {
        val targetDayKey = Formatters.dayKeyFromMillis(selectedDayMillis)
        val targetYearStr = selectedYear.toString()
        state.transactions.filter { tx ->
            when (period) {
                ReportPeriod.DAILY -> Formatters.dayKeyFromMillis(tx.dateEpochMillis) == targetDayKey
                ReportPeriod.MONTHLY -> Formatters.monthKeyFromMillis(tx.dateEpochMillis) == selectedMonthKey
                ReportPeriod.YEARLY -> Formatters.yearKeyFromMillis(tx.dateEpochMillis) == targetYearStr
            }
        }
    }

    val totalIncome = remember(filteredTx) {
        filteredTx.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredTx) {
        filteredTx.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    val categoryColorLookup = remember(state.categories) {
        state.categories.associate { it.id to Color(it.colorHex) }
    }
    val fallbackPalette = listOf(
        Color(0xFF085B4B),
        Color(0xFFD4921D),
        Color(0xFF28666E),
        Color(0xFFD14343),
        Color(0xFF5C6BC0),
        Color(0xFFE2136E),
        Color(0xFF00897B)
    )

    val expenseSlices = remember(filteredTx, totalExpense, categoryColorLookup) {
        if (totalExpense <= 0.0) emptyList()
        else {
            filteredTx
                .filter { it.parsedType == TransactionType.EXPENSE }
                .groupBy { it.categoryId to it.categoryName }
                .entries
                .mapIndexed { idx, (key, list) ->
                    val sum = list.sumOf { it.amount }
                    val col = categoryColorLookup[key.first] ?: fallbackPalette[idx % fallbackPalette.size]
                    CategorySlice(
                        name = key.second,
                        amount = sum,
                        percentage = (sum / totalExpense).toFloat(),
                        color = col
                    )
                }
                .sortedByDescending { it.amount }
        }
    }

    val barData = remember(state.transactions, period, selectedYear, selectedMonthKey) {
        when (period) {
            ReportPeriod.YEARLY -> {
                (0..11).map { m ->
                    val mKey = String.format("%04d-%02d", selectedYear, m + 1)
                    val mTx = state.transactions.filter { Formatters.monthKeyFromMillis(it.dateEpochMillis) == mKey }
                    BarComparisonPoint(
                        label = Formatters.monthNameShortBn(m),
                        income = mTx.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount },
                        expense = mTx.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
            }
            else -> {
                // Show last 6 months comparison
                (-5..0).map { offset ->
                    val mk = Formatters.shiftMonthKey(selectedMonthKey, offset)
                    val mTx = state.transactions.filter { Formatters.monthKeyFromMillis(it.dateEpochMillis) == mk }
                    val mNum = mk.split("-").getOrNull(1)?.toIntOrNull()?.minus(1) ?: 0
                    BarComparisonPoint(
                        label = Formatters.monthNameShortBn(mNum),
                        income = mTx.filter { it.parsedType == TransactionType.INCOME }.sumOf { it.amount },
                        expense = mTx.filter { it.parsedType == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "রিপোর্ট ও বিশ্লেষণ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Period Toggle
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ReportPeriod.entries.forEachIndexed { idx, p ->
                        SegmentedButton(
                            selected = period == p,
                            onClick = { period = p },
                            shape = SegmentedButtonDefaults.itemShape(idx, ReportPeriod.entries.size),
                            modifier = Modifier.testTag("report_period_${p.name.lowercase()}")
                        ) {
                            Text(p.labelBn, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Date/Month/Year Stepper
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            when (period) {
                                ReportPeriod.DAILY -> selectedDayMillis = Formatters.shiftDayMillis(selectedDayMillis, -1)
                                ReportPeriod.MONTHLY -> selectedMonthKey = Formatters.shiftMonthKey(selectedMonthKey, -1)
                                ReportPeriod.YEARLY -> selectedYear -= 1
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "পূর্ববর্তী")
                        }
                        Text(
                            text = periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            when (period) {
                                ReportPeriod.DAILY -> selectedDayMillis = Formatters.shiftDayMillis(selectedDayMillis, 1)
                                ReportPeriod.MONTHLY -> selectedMonthKey = Formatters.shiftMonthKey(selectedMonthKey, 1)
                                ReportPeriod.YEARLY -> selectedYear += 1
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "পরবর্তী")
                        }
                    }
                }
            }

            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReportSummaryCard(
                        title = "মোট আয়",
                        value = Formatters.formatTaka(totalIncome),
                        color = semantic.income,
                        bgColor = semantic.incomeContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ReportSummaryCard(
                        title = "মোট ব্যয়",
                        value = Formatters.formatTaka(totalExpense),
                        color = semantic.expense,
                        bgColor = semantic.expenseContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ReportSummaryCard(
                        title = "নিট ব্যালেন্স",
                        value = Formatters.formatTaka(netBalance),
                        color = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Donut Chart: Category-wise Expense
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "খাতভিত্তিক ব্যয়ের পাই চার্ট",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (expenseSlices.isEmpty()) {
                            Text(
                                text = "এই সময়কালে কোনো খরচের তথ্য নেই।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(180.dp)) {
                                    var startAngle = -90f
                                    val strokeWidth = 42.dp.toPx()
                                    expenseSlices.forEach { slice ->
                                        val sweep = slice.percentage * 360f
                                        drawArc(
                                            color = slice.color,
                                            startAngle = startAngle,
                                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "মোট ব্যয়",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatCompactTaka(totalExpense),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                expenseSlices.forEach { slice ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(slice.color)
                                            )
                                            Text(
                                                text = slice.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = "${Formatters.formatTaka(slice.amount)} (${(slice.percentage * 100).toInt()}%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bar Chart: Income vs Expense Trend
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "আয় বনাম ব্যয় (বার গ্রাফ)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LegendDot(color = semantic.income, label = "আয়")
                                LegendDot(color = semantic.expense, label = "ব্যয়")
                            }
                        }

                        val maxVal = barData.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val groupCount = barData.size.coerceAtLeast(1)
                            val groupWidth = size.width / groupCount
                            val barWidth = (groupWidth * 0.28f).coerceAtMost(20.dp.toPx())
                            val gap = 4.dp.toPx()

                            barData.forEachIndexed { index, point ->
                                val centerX = groupWidth * index + groupWidth / 2f
                                val incH = ((point.income / maxVal) * (size.height - 12f)).toFloat()
                                val expH = ((point.expense / maxVal) * (size.height - 12f)).toFloat()

                                if (incH > 0f) {
                                    drawRoundRect(
                                        color = semantic.income,
                                        topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incH),
                                        size = Size(barWidth, incH),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }
                                if (expH > 0f) {
                                    drawRoundRect(
                                        color = semantic.expense,
                                        topLeft = Offset(centerX + gap / 2f, size.height - expH),
                                        size = Size(barWidth, expH),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            barData.forEach { point ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = point.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Export Data (PDF & CSV)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ডাটা এক্সপোর্ট ও ডাউনলোড",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "বর্তমান সময়কালের (${periodLabel}) হিসাব বিবরণী PDF অথবা Excel/CSV ফরম্যাটে এক্সপোর্ট ও শেয়ার করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    ReportExporter.exportPdfAndShare(
                                        context, periodLabel, filteredTx, totalIncome, totalExpense
                                    ).onFailure {
                                        onFeedback("PDF তৈরি করতে সমস্যা হয়েছে: ${it.localizedMessage}")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_pdf_button")
                            ) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("PDF এক্সপোর্ট")
                            }

                            OutlinedButton(
                                onClick = {
                                    ReportExporter.exportCsvAndShare(
                                        context, periodLabel, filteredTx, totalIncome, totalExpense
                                    ).onFailure {
                                        onFeedback("CSV তৈরি করতে সমস্যা হয়েছে: ${it.localizedMessage}")
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("export_csv_button")
                            ) {
                                Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("CSV (Excel)")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ReportSummaryCard(
    title: String,
    value: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
