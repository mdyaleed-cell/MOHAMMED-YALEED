package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LocalGuestRepository
import com.example.data.model.DebtStatus
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.data.preferences.SecurityPreferences
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.HisabKhataTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app resources and google sign in client id are packaged`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("হিসাব খাতা", appName)

        val webClientId = context.getString(R.string.default_web_client_id)
        assertTrue(webClientId.endsWith(".apps.googleusercontent.com"))
    }

    @Test
    fun `local guest repository supports full accounting flow and disk persistence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "hisab_khata_guest_data.json").delete()

        val repo = LocalGuestRepository(context)
        repo.ensureDefaultSetup("guest_local").getOrThrow()

        val categories = repo.observeCategories("guest_local").first()
        val wallets = repo.observePaymentMethods("guest_local").first()
        assertEquals(13, categories.size)
        assertEquals(5, wallets.size)

        val now = System.currentTimeMillis()
        repo.saveTransaction(
            userId = "guest_local",
            existingId = null,
            type = TransactionType.INCOME,
            amount = 25000.0,
            categoryId = "cat_salary",
            categoryName = "বেতন",
            paymentMethodId = "pm_bank",
            paymentMethodName = "ব্যাংক অ্যাকাউন্ট",
            note = "মাসিক বেতন",
            dateEpochMillis = now
        ).getOrThrow()

        repo.saveTransaction(
            userId = "guest_local",
            existingId = null,
            type = TransactionType.EXPENSE,
            amount = 1500.0,
            categoryId = "cat_food",
            categoryName = "খাবার ও বাজার",
            paymentMethodId = "pm_bkash",
            paymentMethodName = "বিকাশ (bKash)",
            note = "বাজার",
            dateEpochMillis = now
        ).getOrThrow()

        repo.saveBudget("guest_local", "cat_food", "খাবার ও বাজার", Formatters.currentMonthKey(), 5000.0).getOrThrow()
        repo.saveSavingsGoal("guest_local", null, "জরুরি তহবিল", 50000.0, 5000.0, now + 86_400_000L, 0xFF085B4BL).getOrThrow()
        repo.saveDebt("guest_local", null, "করিম", "01700000000", DebtType.LENT, 2000.0, 0.0, "ধার", now).getOrThrow()
        val debt = repo.observeDebts("guest_local").first().single()
        repo.recordDebtPayment("guest_local", debt, 2000.0).getOrThrow()
        repo.saveBillReminder("guest_local", null, "বিদ্যুৎ বিল", 1200.0, 10L, "বিল ও ইউটিলিটি", true, true, "").getOrThrow()

        // Reload from persisted JSON file in a fresh instance
        val reloaded = LocalGuestRepository(context)
        assertEquals(2, reloaded.observeTransactions("guest_local").first().size)
        assertEquals(1, reloaded.observeBudgets("guest_local").first().size)
        assertEquals(1, reloaded.observeSavingsGoals("guest_local").first().size)
        assertEquals(DebtStatus.SETTLED.name, reloaded.observeDebts("guest_local").first().single().status)
        assertEquals(1, reloaded.observeBillReminders("guest_local").first().size)
    }

    @Test
    fun `pin verification accepts matching 4 digit pin and rejects wrong pin`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = SecurityPreferences(context)
        prefs.setPin("4829")
        val settings = prefs.settingsFlow.first()
        assertTrue(settings.pinLockEnabled)
        assertTrue(prefs.verifyPin("4829", settings.pinHash))
        assertFalse(prefs.verifyPin("1234", settings.pinHash))
    }

    @Test
    fun `auth screen exposes google sign in and guest preview mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(context)
        }
        var guestEntered = false
        composeRule.setContent {
            HisabKhataTheme {
                AuthScreen(
                    onAuthSuccess = {},
                    onContinueAsGuest = { guestEntered = true }
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("guest_preview_button").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(guestEntered)
    }

    @Test
    fun `dashboard renders guest badge and allows quick navigation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "hisab_khata_guest_data.json").delete()
        val repo = LocalGuestRepository(context)
        val vm = FinanceViewModel(repo, "guest_local")

        composeRule.setContent {
            HisabKhataTheme {
                DashboardScreen(
                    state = vm.dashboardState.value.copy(isLoading = false),
                    isOnline = true,
                    isGuestMode = true,
                    onSwitchToGoogleSignIn = {},
                    onQuickAdd = {},
                    onEditTransaction = {},
                    onOpenTransactions = {},
                    onOpenPlan = {},
                    onOpenManageCategoriesAndWallets = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sync_status_badge").assertIsDisplayed()
        composeRule.onNodeWithText("লোকাল মোড").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_income").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_expense").assertIsDisplayed()
    }
}

