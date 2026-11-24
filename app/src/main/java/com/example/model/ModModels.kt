package com.example.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM;

    companion object {
        fun fromRaw(raw: String): MessageRole =
            entries.firstOrNull { it.name == raw } ?: ASSISTANT
    }
}

enum class AttachmentType {
    NONE,
    IMAGE,
    FILE;

    companion object {
        fun fromRaw(raw: String): AttachmentType =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}

enum class PackType(val label: String, val folderPrefix: String) {
    BEHAVIOR_PACK("Behavior Pack", "BP"),
    RESOURCE_PACK("Resource Pack", "RP");

    companion object {
        fun fromRaw(raw: String): PackType =
            entries.firstOrNull { it.name == raw } ?: BEHAVIOR_PACK
    }
}

enum class ExportFormat(val extension: String, val title: String, val subtitle: String, val mimeType: String) {
    MCADDON("mcaddon", "Minecraft Add-on (.mcaddon)", "Combined Behavior & Resource Pack ready to import into Minecraft Bedrock", "application/octet-stream"),
    MCPACK_BP("mcpack", "Behavior Pack (.mcpack)", "Standalone Behavior Pack with blocks, mobs, items, weapons & recipes", "application/octet-stream"),
    MCPACK_RP("mcpack", "Resource Pack (.mcpack)", "Standalone Resource Pack with pixel textures, models & language files", "application/octet-stream"),
    ZIP("zip", "Full Studio Project (.zip)", "Complete source folder archive with BP, RP, manifest & validation report", "application/zip")
}

@Keep
@IgnoreExtraProperties
data class ModProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "Untitled Mod",
    val namespace: String = "custom_mod",
    val description: String = "",
    val targetVersion: String = "1.21.50",
    val bpUuid: String = "",
    val rpUuid: String = "",
    val blocksCount: Long = 0L,
    val mobsCount: Long = 0L,
    val weaponsCount: Long = 0L,
    val armorCount: Long = 0L,
    val itemsCount: Long = 0L,
    val structuresCount: Long = 0L,
    val recipesCount: Long = 0L,
    val texturesCount: Long = 0L,
    val validationErrorCount: Long = 0L,
    val validationSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalComponents: Long
        get() = blocksCount + mobsCount + weaponsCount + armorCount + itemsCount + structuresCount + recipesCount
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val role: String = MessageRole.ASSISTANT.name,
    val text: String = "",
    val attachmentName: String = "",
    val attachmentType: String = AttachmentType.NONE.name,
    val modSnapshotSummary: String = "",
    val generatedFileCount: Long = 0L,
    val validationErrorCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModFileEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val packType: String = PackType.BEHAVIOR_PACK.name,
    val relativePath: String = "",
    val contentText: String = "",
    val textureBase64: String = "",
    val valid: Boolean = true,
    val validationNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedPackType: PackType
        get() = PackType.fromRaw(packType)

    val isTexture: Boolean
        get() = relativePath.endsWith(".png", ignoreCase = true) || textureBase64.isNotBlank()
}

data class PendingAttachment(
    val name: String,
    val type: AttachmentType,
    val bitmap: Bitmap? = null,
    val textContent: String? = null
)

enum class AiGenerationStage(val label: String) {
    UNDERSTANDING("Understanding mod idea…"),
    ARCHITECTING("Building Behavior & Resource Packs…"),
    GENERATING_TEXTURES("Crafting pixel textures…"),
    VALIDATING("Validating JSON & pack links…"),
    SAVING("Syncing project to cloud…")
}

data class ComponentCounts(
    val blocksCount: Long = 0L,
    val mobsCount: Long = 0L,
    val weaponsCount: Long = 0L,
    val armorCount: Long = 0L,
    val itemsCount: Long = 0L,
    val structuresCount: Long = 0L,
    val recipesCount: Long = 0L,
    val texturesCount: Long = 0L
)

data class GeneratedFileSpec(
    val packType: PackType,
    val relativePath: String,
    val contentText: String,
    val texturePrompt: String = ""
)

data class GeneratedModDelta(
    val modName: String,
    val namespace: String,
    val description: String,
    val assistantSummary: String,
    val files: List<GeneratedFileSpec>,
    val texturePrompts: Map<String, String>
)

sealed interface StudioUiState<out T> {
    data object Loading : StudioUiState<Nothing>
    data class Success<T>(val data: T) : StudioUiState<T>
    data class Error(val message: String) : StudioUiState<Nothing>
}

data class StudioBannerMessage(
    val text: String,
    val isError: Boolean = false
)
