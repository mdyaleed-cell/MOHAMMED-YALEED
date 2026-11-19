package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val enLocale = Locale.ENGLISH

    private val numberFormat = object : ThreadLocal<DecimalFormat>() {
        override fun initialValue(): DecimalFormat =
            DecimalFormat("#,##,##0.##", DecimalFormatSymbols(enLocale))
    }

    fun formatTaka(amount: Double, showSign: Boolean = false): String {
        val sign = when {
            !showSign -> if (amount < 0) "-" else ""
            amount > 0 -> "+"
            amount < 0 -> "-"
            else -> ""
        }
        val formatted = numberFormat.get()?.format(abs(amount)) ?: abs(amount).toString()
        return "${sign}৳ $formatted"
    }

    fun formatCompactTaka(amount: Double): String {
        val absVal = abs(amount)
        val prefix = if (amount < 0) "-৳" else "৳"
        return when {
            absVal >= 100_000 -> "$prefix${DecimalFormat("0.#", DecimalFormatSymbols(enLocale)).format(absVal / 100_000)}L"
            absVal >= 1_000 -> "$prefix${DecimalFormat("0.#", DecimalFormatSymbols(enLocale)).format(absVal / 1_000)}K"
            else -> "$prefix${DecimalFormat("0", DecimalFormatSymbols(enLocale)).format(absVal)}"
        }
    }

    fun monthKeyFromMillis(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM", enLocale)
        return sdf.format(Date(epochMillis))
    }

    fun yearKeyFromMillis(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy", enLocale)
        return sdf.format(Date(epochMillis))
    }

    fun dayKeyFromMillis(epochMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", enLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatDate(epochMillis: Long): String {
        if (epochMillis <= 0L) return "তারিখ নির্ধারিত নয়"
        val sdf = SimpleDateFormat("dd MMM yyyy", enLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatMonthDisplay(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val year = parts[0]
        val monthIndex = parts[1].toIntOrNull()?.minus(1) ?: return monthKey
        val monthNamesBn = listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )
        val name = monthNamesBn.getOrElse(monthIndex) { monthKey }
        return "$name $year"
    }

    fun shiftMonthKey(monthKey: String, deltaMonths: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM", enLocale)
        val date = runCatching { sdf.parse(monthKey) }.getOrNull() ?: Date()
        val cal = Calendar.getInstance().apply {
            time = date
            add(Calendar.MONTH, deltaMonths)
        }
        return sdf.format(cal.time)
    }

    fun shiftDayMillis(epochMillis: Long, deltaDays: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            add(Calendar.DAY_OF_MONTH, deltaDays)
        }
        return cal.timeInMillis
    }

    fun currentDayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    fun currentMonthKey(): String = monthKeyFromMillis(System.currentTimeMillis())

    fun monthNameShortBn(monthZeroBased: Int): String {
        val names = listOf("জানু", "ফেব", "মার্চ", "এপ্রি", "মে", "জুন", "জুলা", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
        return names.getOrElse(monthZeroBased) { "" }
    }
}
