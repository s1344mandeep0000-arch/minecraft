package com.example.data

import android.content.Context
import com.example.R
import com.example.bedrock.BedrockPackEngine
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AttachmentType
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.ModFileEntry
import com.example.model.ModProject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ModProjectRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeProjects(userId: String): Flow<List<ModProject>> = flow {
        if (userId.isBlank()) throw IllegalStateException("Missing authenticated user ID.")
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users").document(userId).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ModProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(userId: String, projectId: String): Flow<List<ChatMessage>> = flow {
        if (userId.isBlank() || projectId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/projects/$projectId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("projects").document(projectId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeFiles(userId: String, projectId: String): Flow<List<ModFileEntry>> = flow {
        if (userId.isBlank() || projectId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/projects/$projectId/files"
        emitAll(
            db.collection("users").document(userId)
                .collection("projects").document(projectId)
                .collection("files")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ModFileEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(compareBy<ModFileEntry> { it.packType }.thenBy { it.relativePath })
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProjectWithWelcome(
        name: String = "New Bedrock Mod",
        namespace: String = "custom_mod",
        description: String = "AI-crafted Minecraft Bedrock Add-on"
    ): Result<String> {
        var docPath = "users/unauthenticated/projects"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim().take(120).ifEmpty { "New Bedrock Mod" }
            val cleanNamespace = BedrockPackEngine.sanitizeNamespace(namespace)
            val cleanDesc = description.trim().take(1000)

            val projRef = db.collection("users").document(uid).collection("projects").document()
            val projectId = projRef.id
            docPath = projRef.path

            val bpUuid = UUID.randomUUID().toString()
            val rpUuid = UUID.randomUUID().toString()

            val projectPayload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "namespace" to cleanNamespace,
                "description" to cleanDesc,
                "targetVersion" to "1.21.50",
                "bpUuid" to bpUuid,
                "rpUuid" to rpUuid,
                "blocksCount" to 0L,
                "mobsCount" to 0L,
                "weaponsCount" to 0L,
                "armorCount" to 0L,
                "itemsCount" to 0L,
                "structuresCount" to 0L,
                "recipesCount" to 0L,
                "texturesCount" to 0L,
                "validationErrorCount" to 0L,
                "validationSummary" to "Ready to generate Behavior & Resource Packs.",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val welcomeRef = projRef.collection("messages").document()
            val welcomePayload = mapOf(
                "userId" to uid,
                "projectId" to projectId,
                "role" to MessageRole.ASSISTANT.name,
                "text" to "Welcome to Minecraft AI Mod Maker Studio! Describe any mod or add-on idea below—like \"Create a Dragon World Mod\" or \"Add a glowing Emerald Katana with crafting recipe\"—and I'll build the Behavior Pack, Resource Pack, JSON files, and pixel textures automatically.",
                "attachmentName" to "",
                "attachmentType" to AttachmentType.NONE.name,
                "modSnapshotSummary" to "",
                "generatedFileCount" to 0L,
                "validationErrorCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(projRef, projectPayload)
            batch.set(welcomeRef, welcomePayload)
            batch.commit().await()

            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveUserAndAssistantTurn(
        project: ModProject,
        userText: String,
        attachmentName: String,
        attachmentType: AttachmentType,
        assistantText: String,
        modSnapshotSummary: String,
        updatedFiles: List<ModFileEntry>,
        existingFileIds: Set<String>
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/${project.id}"
        return try {
            val uid = requireUserId()
            require(project.id.isNotBlank()) { "Project ID must not be blank." }
            val projRef = db.collection("users").document(uid)
                .collection("projects").document(project.id)
            docPath = projRef.path

            val batch = db.batch()

            // 1. Update project metadata & counts (preserving createdAt & userId)
            val projectUpdates = mapOf(
                "name" to project.name.trim().take(120).ifEmpty { "Untitled Mod" },
                "namespace" to BedrockPackEngine.sanitizeNamespace(project.namespace),
                "description" to project.description.trim().take(1000),
                "targetVersion" to project.targetVersion.ifBlank { "1.21.50" }.take(32),
                "bpUuid" to project.bpUuid,
                "rpUuid" to project.rpUuid,
                "blocksCount" to project.blocksCount,
                "mobsCount" to project.mobsCount,
                "weaponsCount" to project.weaponsCount,
                "armorCount" to project.armorCount,
                "itemsCount" to project.itemsCount,
                "structuresCount" to project.structuresCount,
                "recipesCount" to project.recipesCount,
                "texturesCount" to project.texturesCount,
                "validationErrorCount" to project.validationErrorCount,
                "validationSummary" to project.validationSummary.take(2000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.update(projRef, projectUpdates)

            // 2. Append user message
            val userMsgRef = projRef.collection("messages").document()
            batch.set(
                userMsgRef,
                mapOf(
                    "userId" to uid,
                    "projectId" to project.id,
                    "role" to MessageRole.USER.name,
                    "text" to userText.take(32000),
                    "attachmentName" to attachmentName.take(200),
                    "attachmentType" to attachmentType.name,
                    "modSnapshotSummary" to "",
                    "generatedFileCount" to 0L,
                    "validationErrorCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 3. Append assistant message
            val aiMsgRef = projRef.collection("messages").document()
            batch.set(
                aiMsgRef,
                mapOf(
                    "userId" to uid,
                    "projectId" to project.id,
                    "role" to MessageRole.ASSISTANT.name,
                    "text" to assistantText.take(32000),
                    "attachmentName" to "",
                    "attachmentType" to AttachmentType.NONE.name,
                    "modSnapshotSummary" to modSnapshotSummary.take(2000),
                    "generatedFileCount" to updatedFiles.size.toLong(),
                    "validationErrorCount" to project.validationErrorCount,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // 4. Upsert modified/new files (stay well within Firestore 500-op batch limit)
            for (file in updatedFiles.take(400)) {
                val docId = file.id.ifBlank {
                    BedrockPackEngine.toFirestoreDocId(file.resolvedPackType, file.relativePath)
                }
                val fileRef = projRef.collection("files").document(docId)
                if (existingFileIds.contains(docId)) {
                    batch.update(
                        fileRef,
                        mapOf(
                            "packType" to file.packType,
                            "relativePath" to file.relativePath.take(240),
                            "contentText" to file.contentText.take(200000),
                            "textureBase64" to file.textureBase64.take(100000),
                            "valid" to file.valid,
                            "validationNotes" to file.validationNotes.take(1000),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        fileRef,
                        mapOf(
                            "userId" to uid,
                            "projectId" to project.id,
                            "packType" to file.packType,
                            "relativePath" to file.relativePath.take(240),
                            "contentText" to file.contentText.take(200000),
                            "textureBase64" to file.textureBase64.take(100000),
                            "valid" to file.valid,
                            "validationNotes" to file.validationNotes.take(1000),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSingleTextureFile(
        project: ModProject,
        file: ModFileEntry,
        isExistingFile: Boolean
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/${project.id}/files/${file.id}"
        return try {
            val uid = requireUserId()
            val projRef = db.collection("users").document(uid)
                .collection("projects").document(project.id)
            val docId = file.id.ifBlank {
                BedrockPackEngine.toFirestoreDocId(file.resolvedPackType, file.relativePath)
            }
            val fileRef = projRef.collection("files").document(docId)
            docPath = fileRef.path

            val batch = db.batch()
            if (isExistingFile) {
                batch.update(
                    fileRef,
                    mapOf(
                        "packType" to file.packType,
                        "relativePath" to file.relativePath.take(240),
                        "contentText" to file.contentText.take(200000),
                        "textureBase64" to file.textureBase64.take(100000),
                        "valid" to file.valid,
                        "validationNotes" to file.validationNotes.take(1000),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    fileRef,
                    mapOf(
                        "userId" to uid,
                        "projectId" to project.id,
                        "packType" to file.packType,
                        "relativePath" to file.relativePath.take(240),
                        "contentText" to file.contentText.take(200000),
                        "textureBase64" to file.textureBase64.take(100000),
                        "valid" to file.valid,
                        "validationNotes" to file.validationNotes.take(1000),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                projRef,
                mapOf(
                    "texturesCount" to project.texturesCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            val projRef = db.collection("users").document(uid)
                .collection("projects").document(projectId)
            docPath = projRef.path

            val msgs = projRef.collection("messages").get().await()
            val files = projRef.collection("files").get().await()
            val allRefs = msgs.documents.map { it.reference } + files.documents.map { it.reference } + projRef

            allRefs.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
