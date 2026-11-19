package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.preferences.SecurityPreferences
import com.example.data.preferences.SecuritySettings
import com.example.util.BiometricHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    user: FirebaseUser?,
    isOnline: Boolean,
    hasPendingSync: Boolean,
    isGuestMode: Boolean = false,
    onSwitchToGoogleSignIn: () -> Unit = {},
    securitySettings: SecuritySettings,
    securityPreferences: SecurityPreferences,
    onOpenManageCategoriesAndWallets: () -> Unit,
    onFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val biometricSupported = remember(context) { BiometricHelper.isBiometricAvailable(context) }

    var showSetupPinDialog by remember { mutableStateOf(false) }

    if (showSetupPinDialog) {
        SetupPinDialog(
            onDismiss = { showSetupPinDialog = false },
            onSavePin = { pin ->
                scope.launch {
                    securityPreferences.setPin(pin)
                    showSetupPinDialog = false
                    onFeedback("৪-সংখ্যার পিন লক সক্রিয় করা হয়েছে")
                }
            }
        )
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
                    text = "সেটিংস ও ব্যাকআপ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Account & Cloud Sync Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGuestMode) "টেস্ট / গেস্ট ব্যবহারকারী" else (user?.displayName ?: "গুগল ব্যবহারকারী"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isGuestMode) "লগইন ছাড়া লোকাল ডাটা মোড" else (user?.email ?: ""),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SyncStatusBadge(
                                isOnline = isOnline,
                                hasPendingSync = hasPendingSync,
                                isGuestMode = isGuestMode
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isGuestMode)
                                    "আপনি এখন লগইন ছাড়া টেস্ট মোডে আছেন। সব হিসাব ফোনে সংরক্ষিত থাকছে। ক্লাউড ব্যাকআপের জন্য Google দিয়ে সাইন ইন করুন।"
                                else
                                    "সব আয়-ব্যয়, বাজেট, সঞ্চয় ও দেনা-পাওনা স্বয়ংক্রিয়ভাবে ক্লাউডে ব্যাকআপ থাকে এবং অফলাইনেও কাজ করে।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Categories & Wallets shortcut
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenManageCategoriesAndWallets)
                        .testTag("settings_manage_categories_card")
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ক্যাটাগরি ও পেমেন্ট মেথড পরিচালনা",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "নতুন ক্যাটাগরি, নগদ, ব্যাংক, বিকাশ বা নগদ ওয়ালেট যোগ/মুছুন",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }

            // Security Lock Card
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
                            text = "নিরাপত্তা ও পিন লক",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        SettingsToggleRow(
                            icon = Icons.Rounded.Lock,
                            title = "অ্যাপ পিন লক",
                            subtitle = if (securitySettings.pinLockEnabled) "অ্যাপ খোলার সময় ৪-সংখ্যার পিন চাইবে"
                            else "ব্যক্তিগত হিসাব সুরক্ষিত রাখতে পিন চালু করুন",
                            checked = securitySettings.pinLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    showSetupPinDialog = true
                                } else {
                                    scope.launch {
                                        securityPreferences.disablePinLock()
                                        onFeedback("পিন লক বন্ধ করা হয়েছে")
                                    }
                                }
                            },
                            testTag = "pin_lock_switch"
                        )

                        if (securitySettings.pinLockEnabled) {
                            TextButton(
                                onClick = { showSetupPinDialog = true },
                                modifier = Modifier.testTag("change_pin_button")
                            ) {
                                Text("পিন পরিবর্তন করুন")
                            }
                        }

                        if (biometricSupported) {
                            SettingsToggleRow(
                                icon = Icons.Rounded.Fingerprint,
                                title = "বায়োমেট্রিক / ফিঙ্গারপ্রিন্ট আনলক",
                                subtitle = "পিনের পাশাপাশি ফিঙ্গারপ্রিন্ট দিয়ে দ্রুত খুলুন",
                                checked = securitySettings.biometricEnabled,
                                onCheckedChange = { enable ->
                                    if (!securitySettings.pinLockEnabled && enable) {
                                        onFeedback("প্রথমে পিন লক চালু করুন")
                                    } else {
                                        scope.launch {
                                            securityPreferences.setBiometricEnabled(enable)
                                        }
                                    }
                                },
                                testTag = "biometric_switch"
                            )
                        }
                    }
                }
            }

            // Theme Mode Card
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.DarkMode, contentDescription = null)
                            Text(
                                text = "থিম মোড",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val options = listOf(null to "সিস্টেম", false to "লাইট", true to "ডার্ক")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            options.forEachIndexed { idx, (value, label) ->
                                SegmentedButton(
                                    selected = securitySettings.darkThemeOverride == value,
                                    onClick = {
                                        scope.launch { securityPreferences.setDarkThemeOverride(value) }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(idx, options.size)
                                ) { Text(label) }
                            }
                        }
                    }
                }
            }

            // Offline Note + Sign Out
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.OfflinePin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "অফলাইন অ্যাক্সেস সক্রিয়: ইন্টারনেট সংযোগ ছাড়াই যেকোনো হিসাব এন্ট্রি করতে পারবেন। সংযোগ পেলেই ক্লাউডে সিঙ্ক হবে।",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                if (isGuestMode) {
                    Button(
                        onClick = onSwitchToGoogleSignIn,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("switch_to_google_signin_button")
                    ) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Google (Gmail) দিয়ে সাইন ইন করুন", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("অ্যাকাউন্ট থেকে সাইন আউট করুন", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
