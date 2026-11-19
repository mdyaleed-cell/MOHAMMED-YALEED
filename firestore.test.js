const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied across all accounting collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("categories").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("payment_methods").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("budgets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("savings_goals").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("debts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bill_reminders").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 250,
      categoryId: "cat_food",
      categoryName: "খাবার",
      paymentMethodId: "pm_cash",
      paymentMethodName: "নগদ",
      note: "Lunch",
      dateEpochMillis: Date.now(),
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's collections", async () => {
  const past = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 500,
      categoryId: "cat_food",
      categoryName: "খাবার",
      paymentMethodId: "pm_cash",
      paymentMethodName: "নগদ",
      note: "Bazar",
      dateEpochMillis: Date.now(),
      createdAt: past,
      updatedAt: past,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").update({
    amount: 999,
    updatedAt: new Date(Date.now() - 1000),
  }));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").delete());
});

test("Transactions: valid create, partial update, list, delete; reject ghost id, future timestamp, negative amount", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const txCol = aliceDb.collection("users").doc(ALICE_UID).collection("transactions");

  // Empty collection list succeeds
  await assertSucceeds(txCol.get());

  await assertSucceeds(
    txCol.doc("tx_valid_1").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 1250.5,
      categoryId: "cat_food",
      categoryName: "খাবার ও বাজার",
      paymentMethodId: "pm_bkash",
      paymentMethodName: "বিকাশ",
      note: "সাপ্তাহিক বাজার",
      dateEpochMillis: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update preserving createdAt
  await assertSucceeds(
    txCol.doc("tx_valid_1").update({
      amount: 1400.0,
      note: "সাপ্তাহিক বাজার ও ফল",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost `id` field
  await assertFails(
    txCol.doc("tx_bad_id").set({
      id: "tx_bad_id",
      userId: ALICE_UID,
      type: "INCOME",
      amount: 5000,
      categoryId: "cat_salary",
      categoryName: "বেতন",
      paymentMethodId: "pm_bank",
      paymentMethodName: "ব্যাংক",
      note: "",
      dateEpochMillis: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    txCol.doc("tx_future").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 100,
      categoryId: "cat_food",
      categoryName: "খাবার",
      paymentMethodId: "pm_cash",
      paymentMethodName: "নগদ",
      note: "",
      dateEpochMillis: Date.now(),
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject non-positive amount
  await assertFails(
    txCol.doc("tx_zero").set({
      userId: ALICE_UID,
      type: "EXPENSE",
      amount: 0,
      categoryId: "cat_food",
      categoryName: "খাবার",
      paymentMethodId: "pm_cash",
      paymentMethodName: "নগদ",
      note: "",
      dateEpochMillis: Date.now(),
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(txCol.doc("tx_valid_1").delete());
});

test("Categories & Payment Methods: CRUD succeeds for owner and rejects invalid enum", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const catCol = aliceDb.collection("users").doc(ALICE_UID).collection("categories");
  const pmCol = aliceDb.collection("users").doc(ALICE_UID).collection("payment_methods");

  await assertSucceeds(
    catCol.doc("cat_food").set({
      userId: ALICE_UID,
      name: "খাবার ও বাজার",
      type: "EXPENSE",
      iconKey: "restaurant",
      colorHex: 0xFF0D7C66,
      defaultCategory: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    catCol.doc("cat_bad").set({
      userId: ALICE_UID,
      name: "অবৈধ",
      type: "TRANSFER",
      iconKey: "restaurant",
      colorHex: 0xFF0D7C66,
      defaultCategory: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    pmCol.doc("pm_bkash").set({
      userId: ALICE_UID,
      name: "বিকাশ",
      methodType: "MOBILE_BANKING",
      openingBalance: 3500.0,
      colorHex: 0xFFE2136E,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Budgets, Savings Goals, Debts, and Bill Reminders: CRUD and state rules pass", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  // Budget
  await assertSucceeds(
    userDoc.collection("budgets").doc("2026-11_cat_food").set({
      userId: ALICE_UID,
      categoryId: "cat_food",
      categoryName: "খাবার ও বাজার",
      monthKey: "2026-11",
      limitAmount: 15000,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    userDoc.collection("budgets").doc("bad_month").set({
      userId: ALICE_UID,
      categoryId: "cat_food",
      categoryName: "খাবার",
      monthKey: "2026/11",
      limitAmount: 15000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Savings Goal
  await assertSucceeds(
    userDoc.collection("savings_goals").doc("goal_1").set({
      userId: ALICE_UID,
      title: "জরুরি তহবিল",
      targetAmount: 100000,
      savedAmount: 25000,
      targetDateEpochMillis: Date.now() + 864000000,
      colorHex: 0xFF0D7C66,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    userDoc.collection("savings_goals").doc("goal_1").update({
      savedAmount: 30000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Debt
  await assertSucceeds(
    userDoc.collection("debts").doc("debt_1").set({
      userId: ALICE_UID,
      personName: "করিম ভাই",
      phone: "01711000000",
      debtType: "LENT",
      amount: 5000,
      paidAmount: 0,
      note: "অফিসের বাইরে ধার",
      dueDateEpochMillis: Date.now() + 604800000,
      status: "ACTIVE",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    userDoc.collection("debts").doc("debt_bad").set({
      userId: ALICE_UID,
      personName: "রহিম",
      phone: "",
      debtType: "LENT",
      amount: 1000,
      paidAmount: 2000,
      note: "",
      dueDateEpochMillis: 0,
      status: "ACTIVE",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bill Reminder
  await assertSucceeds(
    userDoc.collection("bill_reminders").doc("bill_1").set({
      userId: ALICE_UID,
      title: "বিদ্যুৎ বিল",
      amount: 1800,
      dueDayOfMonth: 10,
      categoryName: "বিল ও ইউটিলিটি",
      recurringMonthly: true,
      reminderEnabled: true,
      lastPaidMonthKey: "",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    userDoc.collection("bill_reminders").doc("bill_1").update({
      lastPaidMonthKey: "2026-11",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
