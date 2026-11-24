const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_modder_01";
const BOB_UID = "bob_modder_02";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProjectPayload(userId, ts) {
  return {
    userId,
    name: "Dragon World Mod",
    namespace: "dragon_world",
    description: "Dragons, dragon armor, weapons, eggs, and boss fights.",
    targetVersion: "1.21.50",
    bpUuid: "11111111-2222-4333-8444-555555555555",
    rpUuid: "66666666-7777-4888-8999-aaaaaaaaaaaa",
    blocksCount: 2,
    mobsCount: 3,
    weaponsCount: 2,
    armorCount: 4,
    itemsCount: 3,
    structuresCount: 1,
    recipesCount: 5,
    texturesCount: 4,
    validationErrorCount: 0,
    validationSummary: "All 14 pack files validated cleanly.",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validMessagePayload(userId, projectId, ts) {
  return {
    userId,
    projectId,
    role: "USER",
    text: "Create a Dragon World Mod",
    attachmentName: "",
    attachmentType: "NONE",
    modSnapshotSummary: "",
    generatedFileCount: 0,
    validationErrorCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validFilePayload(userId, projectId, ts) {
  return {
    userId,
    projectId,
    packType: "BEHAVIOR_PACK",
    relativePath: "manifest.json",
    contentText: "{\"format_version\":2}",
    textureBase64: "",
    valid: true,
    validationNotes: "",
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated user cannot read or write projects, messages, or files", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/projects`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/projects`).doc("proj_1").set(validProjectPayload(ALICE_UID, ts))
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/projects/proj_1/messages`).doc("msg_1").set(validMessagePayload(ALICE_UID, "proj_1", ts))
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/projects/proj_1/files`).doc("file_1").set(validFilePayload(ALICE_UID, "proj_1", ts))
  );
});

test("Owner can create, list, update, and delete project, messages, and files", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);
  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_dragon");

  await assertSucceeds(projRef.set(validProjectPayload(ALICE_UID, ts)));
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/projects`).get());

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    projRef.update({
      mobsCount: 4,
      validationSummary: "Added Fire Dragon Boss.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Create message & file under existing project
  const msgRef = projRef.collection("messages").doc("msg_1");
  await assertSucceeds(msgRef.set(validMessagePayload(ALICE_UID, "proj_dragon", ts)));
  await assertSucceeds(projRef.collection("messages").get());

  const fileRef = projRef.collection("files").doc("file_manifest");
  await assertSucceeds(fileRef.set(validFilePayload(ALICE_UID, "proj_dragon", ts)));
  await assertSucceeds(projRef.collection("files").get());

  // Partial file update
  await assertSucceeds(
    fileRef.update({
      contentText: "{\"format_version\":2,\"updated\":true}",
      valid: true,
      validationNotes: "",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(fileRef.delete());
  await assertSucceeds(msgRef.delete());
  await assertSucceeds(projRef.delete());
});

test("Atomic batch create of project + welcome message succeeds via existsAfter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 3000);
  const batch = aliceDb.batch();
  const projRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_batch");
  const msgRef = projRef.collection("messages").doc("msg_welcome");

  batch.set(projRef, validProjectPayload(ALICE_UID, ts));
  batch.set(msgRef, validMessagePayload(ALICE_UID, "proj_batch", ts));
  await assertSucceeds(batch.commit());
});

test("Cross-user access and privilege/schema violations are rejected", async () => {
  const ts = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection(`users/${BOB_UID}/projects`).doc("bob_proj").set(validProjectPayload(BOB_UID, ts));
    await adminDb.collection(`users/${BOB_UID}/projects/bob_proj/messages`).doc("bob_msg").set(validMessagePayload(BOB_UID, "bob_proj", ts));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection(`users/${BOB_UID}/projects`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/projects`).doc("bob_proj").get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/projects/bob_proj/messages`).doc("bob_msg").get());
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/projects`).doc("bob_proj").update({ name: "Hijacked" })
  );

  // Ghost field / @DocumentId `id` field / immutable createdAt mutation rejected
  const aliceProjRef = aliceDb.collection(`users/${ALICE_UID}/projects`).doc("proj_bad");
  await assertFails(
    aliceProjRef.set({
      ...validProjectPayload(ALICE_UID, ts),
      id: "proj_bad",
    })
  );
  await assertFails(
    aliceProjRef.set({
      ...validProjectPayload(ALICE_UID, new Date(Date.now() + 60000)),
    })
  );

  // Orphaned message without parent project rejected
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/projects/missing_proj/messages`).doc("msg_orphan")
      .set(validMessagePayload(ALICE_UID, "missing_proj", ts))
  );
});
