const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_owner_1";
const ADMIN_EMAIL = "MonirKb96@gmail.com";
const TENANT_UID = "tenant_rahim_1";
const TENANT_EMAIL = "rahim@example.com";
const OTHER_TENANT_UID = "tenant_karim_2";
const OTHER_TENANT_EMAIL = "karim@example.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function tenantContext(uid = TENANT_UID, email = TENANT_EMAIL, verified = true) {
  return testEnv.authenticatedContext(uid, {
    email,
    email_verified: verified,
  });
}

function safePastDate() {
  return new Date(Date.now() - 2000);
}

async function seedCoreData() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = safePastDate();

    await db.collection("settings").doc("app_config").set({
      ownerUid: ADMIN_UID,
      appName: "Smart Rent Manager",
      defaultLanguage: "bn",
      sslCommerzSandboxMode: true,
      bkashNumber: "01700000000",
      autoBillDay: 1,
      carryForwardEnabled: true,
      createdAt: now,
      updatedAt: now,
    });

    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: ADMIN_EMAIL,
      createdAt: now,
    });

    await db.collection("buildings").doc("bldg_1").set({
      ownerId: ADMIN_UID,
      name: "Green View Tower",
      address: "Dhanmondi 27, Dhaka",
      floorsCount: 6,
      totalFlats: 12,
      contactInfo: "01711111111",
      notes: "Main residential property",
      createdAt: now,
      updatedAt: now,
    });

    await db.collection("flats").doc("flat_2a").set({
      ownerId: ADMIN_UID,
      buildingId: "bldg_1",
      buildingName: "Green View Tower",
      flatNumber: "2A",
      floorNumber: 2,
      monthlyRent: 23000,
      serviceCharge: 3500,
      electricityBill: 1200,
      waterBill: 500,
      gasBill: 1080,
      otherCharges: 0,
      totalMonthlyBill: 29280,
      amountPaid: 0,
      remainingDue: 29280,
      paymentStatus: "unpaid",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      tenantPhone: "01811111111",
      isOccupied: true,
      createdAt: now,
      updatedAt: now,
    });

    await db.collection("monthly_bills").doc("flat_2a_2026-11").set({
      ownerId: ADMIN_UID,
      buildingId: "bldg_1",
      buildingName: "Green View Tower",
      flatId: "flat_2a",
      flatNumber: "2A",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      billingMonth: "11-2026",
      monthKey: "2026-11",
      monthlyRent: 23000,
      serviceCharge: 3500,
      electricityBill: 1200,
      waterBill: 500,
      gasBill: 1080,
      otherCharges: 0,
      previousDueCarriedForward: 0,
      totalBill: 29280,
      amountPaid: 0,
      remainingDue: 29280,
      status: "unpaid",
      dueDate: "10-11-2026",
      createdAt: now,
      updatedAt: now,
    });

    await db.collection("messages").doc("msg_1").set({
      ownerId: ADMIN_UID,
      senderUid: ADMIN_UID,
      senderName: "Owner",
      senderRole: "admin",
      recipientUid: TENANT_UID,
      recipientEmail: TENANT_EMAIL,
      tenantId: "tenant_1",
      buildingId: "bldg_1",
      flatId: "flat_2a",
      flatNumber: "2A",
      title: "Rent Reminder",
      body: "Please clear November rent by 10-11-2026.",
      type: "reminder",
      isRead: false,
      createdAt: now,
      updatedAt: now,
    });

    await db.collection("announcements").doc("ann_1").set({
      ownerId: ADMIN_UID,
      targetScope: "all",
      buildingId: "",
      buildingName: "All Buildings",
      title: "Water Tank Maintenance",
      body: "Tank cleaning tomorrow 10 AM to 12 PM.",
      category: "maintenance",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
  });
}

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

test("Unauthenticated user: denied read on buildings, flats, bills, payments, bank_accounts", async () => {
  await seedCoreData();
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("buildings").get());
  await assertFails(db.collection("flats").doc("flat_2a").get());
  await assertFails(db.collection("monthly_bills").doc("flat_2a_2026-11").get());
  await assertFails(db.collection("bank_accounts").get());
});

test("Privilege escalation: tenant cannot self-assign admin role or write /admins", async () => {
  await seedCoreData();
  const db = tenantContext().firestore();
  const now = safePastDate();
  await assertFails(
    db.collection("admins").doc(TENANT_UID).set({
      userId: TENANT_UID,
      email: TENANT_EMAIL,
      createdAt: now,
    })
  );
  await assertFails(
    db.collection("users").doc(TENANT_UID).set({
      userId: TENANT_UID,
      email: TENANT_EMAIL,
      displayName: "Rahim",
      role: "admin",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Tenant isolation: tenant can query own flat and bills, cannot read other tenant or admin financial records", async () => {
  await seedCoreData();
  const rahimDb = tenantContext().firestore();
  const karimDb = tenantContext(OTHER_TENANT_UID, OTHER_TENANT_EMAIL).firestore();

  await assertSucceeds(
    rahimDb.collection("flats").where("tenantUserId", "==", TENANT_UID).get()
  );
  await assertSucceeds(
    rahimDb.collection("monthly_bills").where("tenantUserId", "==", TENANT_UID).get()
  );

  await assertFails(karimDb.collection("flats").doc("flat_2a").get());
  await assertFails(karimDb.collection("monthly_bills").doc("flat_2a_2026-11").get());
  await assertFails(karimDb.collection("messages").doc("msg_1").get());

  await assertFails(rahimDb.collection("buildings").get());
  await assertFails(rahimDb.collection("expenses").get());
  await assertFails(rahimDb.collection("bank_transactions").get());
  await assertFails(rahimDb.collection("cash_transactions").get());
});

test("Email spoofing defense: unverified email token cannot read tenant records", async () => {
  await seedCoreData();
  const unverifiedDb = tenantContext("spoof_uid", TENANT_EMAIL, false).firestore();
  await assertFails(unverifiedDb.collection("flats").doc("flat_2a").get());
  await assertFails(unverifiedDb.collection("monthly_bills").doc("flat_2a_2026-11").get());
});

test("Payment security: tenant can submit pending payment, CANNOT submit verified payment", async () => {
  await seedCoreData();
  const rahimDb = tenantContext().firestore();
  const now = safePastDate();

  await assertFails(
    rahimDb.collection("payments").doc("pay_forged").set({
      ownerId: ADMIN_UID,
      billId: "flat_2a_2026-11",
      buildingId: "bldg_1",
      flatId: "flat_2a",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      amount: 29280,
      method: "bkash",
      transactionId: "TXN999",
      gatewayStatus: "verified",
      verificationStatus: "verified",
      paymentDate: "14-11-2026",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    rahimDb.collection("payments").doc("pay_pending_1").set({
      ownerId: ADMIN_UID,
      billId: "flat_2a_2026-11",
      buildingId: "bldg_1",
      flatId: "flat_2a",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      amount: 10000,
      method: "bkash",
      transactionId: "TRX123456",
      referenceNumber: "BKS-8899",
      gatewayStatus: "pending",
      verificationStatus: "pending",
      paymentDate: "14-11-2026",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Shadow update & immutable audit log defense", async () => {
  await seedCoreData();
  const adminDb = adminContext().firestore();
  const rahimDb = tenantContext().firestore();
  const now = safePastDate();

  await assertFails(
    adminDb.collection("buildings").doc("bldg_ghost").set({
      ownerId: ADMIN_UID,
      name: "Test Tower",
      address: "Banani, Dhaka",
      floorsCount: 5,
      totalFlats: 10,
      ghostField: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    rahimDb.collection("messages").doc("msg_1").update({
      isRead: true,
      updatedAt: now,
    })
  );
  await assertFails(
    rahimDb.collection("messages").doc("msg_1").update({
      body: "Tampered body",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log_1").set({
      ownerId: ADMIN_UID,
      actorUid: ADMIN_UID,
      actorEmail: ADMIN_EMAIL,
      actorRole: "admin",
      action: "CREATE_BUILDING",
      entityType: "building",
      entityId: "bldg_1",
      summary: "Created Green View Tower",
      createdAt: now,
    })
  );
  await assertFails(adminDb.collection("audit_logs").doc("log_1").delete());
});

test("Tenant can read broadcast announcements with targetScope == all", async () => {
  await seedCoreData();
  const rahimDb = tenantContext().firestore();
  await assertSucceeds(
    rahimDb.collection("announcements").where("targetScope", "==", "all").get()
  );
});

test("Tenant can initialize missing current-month bill matching assigned flat and submit bank transfer with receipt slip", async () => {
  await seedCoreData();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = safePastDate();
    await db.collection("flats").doc("flat_g2").set({
      ownerId: ADMIN_UID,
      buildingId: "bldg_1",
      buildingName: "Adrita Manson",
      flatNumber: "G2",
      floorNumber: 0,
      monthlyRent: 1000,
      serviceCharge: 1000,
      electricityBill: 0,
      waterBill: 0,
      gasBill: 0,
      otherCharges: 100,
      totalMonthlyBill: 2100,
      amountPaid: 0,
      remainingDue: 2100,
      paymentStatus: "unpaid",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      tenantPhone: "01811111111",
      isOccupied: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const adminDb = adminContext().firestore();
  const rahimDb = tenantContext().firestore();
  const now = safePastDate();

  await assertSucceeds(
    adminDb.collection("settings").doc("app_config").update({
      bankAccountsJson: '[{"id":"bank_1","accountName":"MD ABDUR RASHED MANJU","bankName":"UCBL","accountNumber":"7863241000893151","accountType":"Current"}]',
      updatedAt: now,
    })
  );

  // Tenant CANNOT forge lower totalBill
  await assertFails(
    rahimDb.collection("monthly_bills").doc("flat_g2_2026-11").set({
      ownerId: ADMIN_UID,
      buildingId: "bldg_1",
      buildingName: "Adrita Manson",
      flatId: "flat_g2",
      flatNumber: "G2",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      billingMonth: "11-2026",
      monthKey: "2026-11",
      monthlyRent: 1000,
      serviceCharge: 0,
      otherCharges: 0,
      totalBill: 500,
      amountPaid: 0,
      remainingDue: 500,
      status: "unpaid",
      dueDate: "10-11-2026",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Tenant CAN check missing bill via .get() before initializing it
  await assertSucceeds(
    rahimDb.collection("monthly_bills").doc("flat_g2_2026-11").get()
  );

  // Tenant CAN initialize valid bill matching assigned flat
  await assertSucceeds(
    rahimDb.collection("monthly_bills").doc("flat_g2_2026-11").set({
      ownerId: ADMIN_UID,
      buildingId: "bldg_1",
      buildingName: "Adrita Manson",
      flatId: "flat_g2",
      flatNumber: "G2",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      billingMonth: "11-2026",
      monthKey: "2026-11",
      monthlyRent: 1000,
      serviceCharge: 1000,
      electricityBill: 0,
      waterBill: 0,
      gasBill: 0,
      otherCharges: 100,
      previousDueCarriedForward: 0,
      totalBill: 2100,
      amountPaid: 0,
      remainingDue: 2100,
      status: "unpaid",
      dueDate: "10-11-2026",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Tenant CAN submit bank transfer with targetBankAccountId + receiptImageBase64
  await assertSucceeds(
    rahimDb.collection("payments").doc("pay_bank_slip_1").set({
      ownerId: ADMIN_UID,
      billId: "flat_g2_2026-11",
      buildingId: "bldg_1",
      buildingName: "Adrita Manson",
      flatId: "flat_g2",
      flatNumber: "G2",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      amount: 2100,
      method: "bank_transfer",
      transactionId: "BNK123456",
      referenceNumber: "SLIP-BNK123456",
      targetBankAccountId: "bank_1",
      targetBankAccountName: "MD ABDUR RASHED MANJU (UCBL)",
      receiptImageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
      gatewayStatus: "pending",
      verificationStatus: "pending",
      paymentDate: "15-11-2026",
      notes: "Paid via UCBL branch",
      verifiedBy: "",
      receiptId: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Multi-owner isolation: neighbor can provision isolated owner workspace and cannot read Owner 1 data", async () => {
  await seedCoreData();
  const NEIGHBOR_UID = "neighbor_owner_2";
  const NEIGHBOR_EMAIL = "neighbor@example.com";
  const neighborDb = testEnv.authenticatedContext(NEIGHBOR_UID, {
    email: NEIGHBOR_EMAIL,
    email_verified: true,
  }).firestore();
  const now = safePastDate();

  // Neighbor atomically provisions settings/owner_{uid}, admins/{uid}, and users/{uid}
  const batch = neighborDb.batch();
  batch.set(neighborDb.collection("settings").doc(`owner_${NEIGHBOR_UID}`), {
    ownerUid: NEIGHBOR_UID,
    appName: "Smart Rent Manager",
    defaultLanguage: "bn",
    sslCommerzSandboxMode: true,
    bankAccountsJson: "",
    autoBillDay: 1,
    carryForwardEnabled: true,
    createdAt: now,
    updatedAt: now,
  });
  batch.set(neighborDb.collection("admins").doc(NEIGHBOR_UID), {
    userId: NEIGHBOR_UID,
    email: NEIGHBOR_EMAIL,
    createdAt: now,
  });
  batch.set(neighborDb.collection("users").doc(NEIGHBOR_UID), {
    userId: NEIGHBOR_UID,
    email: NEIGHBOR_EMAIL,
    displayName: "Neighbor Owner",
    phone: "",
    role: "admin",
    status: "active",
    tenantId: "",
    buildingId: "",
    flatId: "",
    preferredLanguage: "bn",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Neighbor can create & query their own building
  await assertSucceeds(
    neighborDb.collection("buildings").doc("bldg_neighbor_1").set({
      ownerId: NEIGHBOR_UID,
      name: "Neighbor Villa",
      address: "Mirpur 10, Dhaka",
      floorsCount: 4,
      totalFlats: 8,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    neighborDb.collection("buildings").where("ownerId", "==", NEIGHBOR_UID).get()
  );

  // Neighbor CANNOT read or modify Owner 1's building or flat
  await assertFails(neighborDb.collection("buildings").doc("bldg_1").get());
  await assertFails(neighborDb.collection("flats").doc("flat_2a").get());
  await assertFails(
    neighborDb.collection("buildings").where("ownerId", "==", ADMIN_UID).get()
  );

  // Assigned tenant CANNOT upgrade to owner even with atomic batch
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(TENANT_UID).set({
      userId: TENANT_UID,
      email: TENANT_EMAIL,
      displayName: "Abdur Rahim",
      phone: "01811111111",
      role: "tenant",
      status: "active",
      tenantId: "tenant_1",
      buildingId: "bldg_1",
      flatId: "flat_2a",
      preferredLanguage: "bn",
      createdAt: now,
      updatedAt: now,
    });
  });
  const rahimDb = tenantContext().firestore();
  const exploitBatch = rahimDb.batch();
  exploitBatch.set(rahimDb.collection("settings").doc(`owner_${TENANT_UID}`), {
    ownerUid: TENANT_UID,
    appName: "Smart Rent Manager",
    defaultLanguage: "bn",
    sslCommerzSandboxMode: true,
    autoBillDay: 1,
    carryForwardEnabled: true,
    createdAt: now,
    updatedAt: now,
  });
  exploitBatch.set(rahimDb.collection("admins").doc(TENANT_UID), {
    userId: TENANT_UID,
    email: TENANT_EMAIL,
    createdAt: now,
  });
  await assertFails(exploitBatch.commit());
});

test("Tenant can submit hand-to-hand cash payment with proof photo and notify Owner", async () => {
  await seedCoreData();
  const rahimDb = tenantContext().firestore();
  const now = safePastDate();

  const batch = rahimDb.batch();
  batch.set(rahimDb.collection("payments").doc("pay_cash_proof_1"), {
    ownerId: ADMIN_UID,
    billId: "flat_2a_2026-11",
    buildingId: "bldg_1",
    buildingName: "Green View Tower",
    flatId: "flat_2a",
    flatNumber: "2A",
    tenantId: "tenant_1",
    tenantUserId: TENANT_UID,
    tenantEmail: TENANT_EMAIL,
    tenantName: "Abdur Rahim",
    amount: 29280,
    method: "cash",
    transactionId: "CSH998877",
    referenceNumber: "SLIP-CSH998877",
    targetBankAccountId: "",
    targetBankAccountName: "",
    receiptImageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
    gatewayStatus: "pending",
    verificationStatus: "pending",
    paymentDate: "15-11-2026",
    notes: "Handed cash slip to owner",
    verifiedBy: "",
    receiptId: "",
    createdAt: now,
    updatedAt: now,
  });
  batch.set(rahimDb.collection("notifications").doc("notif_cash_1"), {
    ownerId: ADMIN_UID,
    recipientUid: ADMIN_UID,
    recipientEmail: "",
    title: "নতুন পেমেন্ট ও প্রমাণ (2A)",
    body: "Abdur Rahim ৳29280 (নগদ) দিয়েছেন।",
    type: "payment_submitted",
    relatedId: "pay_cash_proof_1",
    isRead: false,
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());
});

test("Admin can safely check existence of missing documents while tenant is denied", async () => {
  await seedCoreData();
  const ownerDb = adminContext().firestore();
  const rahimDb = tenantContext().firestore();

  const missingFlat = await assertSucceeds(ownerDb.collection("flats").doc("nonexistent_flat_id").get());
  const missingRcp = await assertSucceeds(ownerDb.collection("receipts").doc("nonexistent_rcp_id").get());
  const missingPay = await assertSucceeds(ownerDb.collection("payments").doc("nonexistent_pay_id").get());
  if (missingFlat.exists || missingRcp.exists || missingPay.exists) {
    throw new Error("Expected nonexistent docs not to exist");
  }

  await assertFails(rahimDb.collection("flats").doc("nonexistent_flat_id").get());
  await assertFails(rahimDb.collection("receipts").doc("nonexistent_rcp_id").get());
});

test("Payment supports billingMonth and monthKey fields", async () => {
  await seedCoreData();
  const rahimDb = tenantContext().firestore();
  const now = safePastDate();

  await assertSucceeds(
    rahimDb.collection("payments").doc("pay_with_month_1").set({
      ownerId: ADMIN_UID,
      billId: "flat_2a_2026-11",
      buildingId: "bldg_1",
      buildingName: "Green View Tower",
      flatId: "flat_2a",
      flatNumber: "2A",
      tenantId: "tenant_1",
      tenantUserId: TENANT_UID,
      tenantEmail: TENANT_EMAIL,
      tenantName: "Abdur Rahim",
      amount: 10000,
      method: "bank_transfer",
      transactionId: "TXMONTH01",
      referenceNumber: "REFMONTH01",
      billingMonth: "11-2026",
      monthKey: "2026-11",
      gatewayStatus: "pending",
      verificationStatus: "pending",
      paymentDate: "16-11-2026",
      notes: "November rent",
      verifiedBy: "",
      receiptId: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

