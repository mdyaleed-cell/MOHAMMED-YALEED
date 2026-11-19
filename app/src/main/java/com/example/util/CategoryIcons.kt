package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.PaymentMethodType

object CategoryIcons {
    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "home" to Icons.Rounded.Home,
        "commute" to Icons.Rounded.DirectionsBus,
        "medical" to Icons.Rounded.LocalHospital,
        "bolt" to Icons.Rounded.Bolt,
        "school" to Icons.Rounded.School,
        "shopping" to Icons.Rounded.ShoppingBag,
        "work" to Icons.Rounded.Work,
        "store" to Icons.Rounded.Storefront,
        "laptop" to Icons.Rounded.LaptopMac,
        "gift" to Icons.Rounded.CardGiftcard,
        "savings" to Icons.Rounded.Savings,
        "category" to Icons.Rounded.Category
    )

    val presetColors: List<Long> = listOf(
        0xFF085B4B,
        0xFF0E8A5D,
        0xFF28666E,
        0xFFD4921D,
        0xFFD14343,
        0xFF5C6BC0,
        0xFF7E57C2,
        0xFFE2136E,
        0xFFED6B23,
        0xFF00897B
    )

    fun iconForKey(key: String): ImageVector =
        availableIcons.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Category

    fun iconForPaymentMethod(type: PaymentMethodType): ImageVector = when (type) {
        PaymentMethodType.CASH -> Icons.Rounded.Payments
        PaymentMethodType.BANK -> Icons.Rounded.AccountBalance
        PaymentMethodType.MOBILE_BANKING -> Icons.Rounded.PhoneAndroid
    }

    fun walletIcon(): ImageVector = Icons.Rounded.AccountBalanceWallet
}
