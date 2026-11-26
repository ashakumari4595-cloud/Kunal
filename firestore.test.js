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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: cannot read or write conversations or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/conversations`).get()
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/conversations`).doc("conv_1").set({
      userId: ALICE_UID,
      title: "Unauthorized",
      lastMessagePreview: "",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/conversations/conv_1/messages`)
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        conversationId: "conv_1",
        fromUser: true,
        text: "Unauthorized",
        placeholderResponse: false,
        createdAt: new Date(Date.now() - 1000),
      })
  );
});

test("Authenticated owner: valid conversation create, partial update, message write, and query succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const convRef = aliceDb
    .collection(`users/${ALICE_UID}/conversations`)
    .doc("conv_1");

  await assertSucceeds(
    convRef.set({
      userId: ALICE_UID,
      title: "Morning Check-in",
      lastMessagePreview: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Batch-style message create + conversation preview update
  const msgRef = convRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      conversationId: "conv_1",
      fromUser: true,
      text: "Namaste Avyakt!",
      placeholderResponse: false,
      createdAt: new Date(Date.now() - 2000),
    })
  );

  await assertSucceeds(
    convRef.update({
      lastMessagePreview: "Namaste Avyakt!",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    convRef.update({
      title: "Renamed Chat",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Exact queries from FirestoreChatRepository
  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/conversations`)
      .orderBy("updatedAt", "desc")
      .get()
  );
  await assertSucceeds(
    convRef.collection("messages").orderBy("createdAt", "asc").get()
  );
});

test("Security & schema validation: rejects @DocumentId 'id', immutable createdAt mutation, future timestamps, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/conversations`).doc("conv_1").set({
      userId: ALICE_UID,
      title: "Alice Chat",
      lastMessagePreview: "Hi",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject extra 'id' key (@DocumentId must not be written into document body)
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/conversations`).doc("conv_bad").set({
      id: "conv_bad",
      userId: ALICE_UID,
      title: "Bad Chat",
      lastMessagePreview: "",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/conversations`).doc("conv_future").set({
      userId: ALICE_UID,
      title: "Future Chat",
      lastMessagePreview: "",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/conversations`)
      .doc("conv_1")
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Reject cross-user read, write, or delete
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/conversations`).get()
  );
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/conversations`).doc("conv_1").delete()
  );
  await assertFails(
    bobDb
      .collection(`users/${ALICE_UID}/conversations/conv_1/messages`)
      .doc("msg_bob")
      .set({
        userId: BOB_UID,
        conversationId: "conv_1",
        fromUser: true,
        text: "Hijack",
        placeholderResponse: false,
        createdAt: new Date(Date.now() - 1000),
      })
  );
});
