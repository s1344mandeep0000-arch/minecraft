package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.GeneratedFileSpec
import com.example.model.GeneratedModDelta
import com.example.model.MessageRole
import com.example.model.ModFileEntry
import com.example.model.ModProject
import com.example.model.PackType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "ModAiRepository"

class ModAiRepository {

    private val chatPlannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the lead Minecraft Bedrock Edition Add-on Architect inside Minecraft AI Mod Maker Studio.
                    Respond like a helpful, concise, enthusiastic ChatGPT-style co-creator.
                    When the user describes a mod idea (e.g., "Create a Dragon World Mod" or "Add a powerful fire dragon boss"),
                    immediately explain what you are creating or adding (mobs, blocks, weapons, armor, items, structures, recipes, abilities, loot, and textures)
                    and note any design highlights clearly in structured bullet points.
                    Never tell the user to manually write JSON or pick components in a form—you handle everything automatically.
                    """.trimIndent()
                )
            }
        )
    }

    private val packSchema = Schema.obj(
        properties = mapOf(
            "modName" to Schema.string(description = "Short catchy name for the Minecraft Bedrock Add-on"),
            "namespace" to Schema.string(description = "Lowercase underscore Bedrock namespace, e.g. dragon_world"),
            "description" to Schema.string(description = "1-sentence description of the add-on"),
            "assistantSummary" to Schema.string(description = "Concise 1-2 line snapshot summary of what was added/updated"),
            "files" to Schema.array(
                description = "List of Behavior Pack and Resource Pack JSON/mcfunction files to create or update",
                items = Schema.obj(
                    properties = mapOf(
                        "packType" to Schema.enumeration(
                            values = listOf("BEHAVIOR_PACK", "RESOURCE_PACK"),
                            description = "Target pack"
                        ),
                        "relativePath" to Schema.string(
                            description = "Path inside pack, e.g. entities/fire_dragon.json, items/dragon_sword.json, blocks/dragon_egg_block.json, recipes/dragon_sword_recipe.json, loot_tables/entities/fire_dragon.json, textures/item_texture.json, textures/terrain_texture.json, texts/en_US.lang"
                        ),
                        "contentText" to Schema.string(
                            description = "Valid formatted JSON string (or .lang / .mcfunction text) for Minecraft Bedrock 1.21.50"
                        )
                    )
                )
            ),
            "textureSpecs" to Schema.array(
                description = "Up to 4 key pixel-art texture files to generate in RP/textures/...",
                items = Schema.obj(
                    properties = mapOf(
                        "relativePath" to Schema.string(
                            description = "Path inside Resource Pack ending in .png, e.g. textures/items/dragon_sword.png"
                        ),
                        "prompt" to Schema.string(
                            description = "Detailed 16-bit Minecraft pixel art icon/texture prompt on dark or clean background"
                        )
                    )
                )
            )
        )
    )

    private val packBuilderModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = packSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are a deterministic Minecraft Bedrock Edition (1.21.50) Behavior Pack & Resource Pack compiler.
                    Generate valid, authentic Bedrock JSON files matching the user's request and existing project context.
                    Rules:
                    - Do NOT emit manifest.json unless specifically fixing a broken manifest (the studio manages BP/RP manifests and UUID links).
                    - Every block JSON must use format_version "1.21.50" and root key "minecraft:block" with description.identifier "<namespace>:<name>".
                    - Every entity/mob JSON must use format_version "1.21.50" and root key "minecraft:entity" with description.identifier "<namespace>:<name>", components (health, movement, attack/boss abilities, physics), and component_groups if applicable.
                    - Every item/weapon/armor JSON must use format_version "1.21.50" and root key "minecraft:item" with description.identifier "<namespace>:<name>" and appropriate components (minecraft:icon, minecraft:damage, minecraft:durability, minecraft:wearable, minecraft:display_name).
                    - Every recipe JSON must use format_version "1.20.10" and root key "minecraft:recipe_shaped" or "minecraft:recipe_shapeless" or "minecraft:recipe_furnace".
                    - Include corresponding Resource Pack definitions: RP/textures/item_texture.json, RP/textures/terrain_texture.json, RP/entity/<mob>.entity.json, RP/texts/en_US.lang, and RP/loot_tables/ or BP/loot_tables/ when appropriate.
                    - Ensure every texture_name in item_texture.json / terrain_texture.json matches the PNG paths listed in textureSpecs (without .png extension in texture_data).
                    - Keep JSON clean, syntactically valid, and free of trailing commas or comments.
                    """.trimIndent()
                )
            }
        )
    }

    private val textureModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun streamConversationalReply(
        prompt: String,
        project: ModProject,
        history: List<ChatMessage>,
        existingFiles: List<ModFileEntry>,
        attachedBitmap: Bitmap?,
        attachedFileText: String?
    ): Flow<String> = flow {
        val recentHistory = history.takeLast(8).joinToString("\n") { msg ->
            "${msg.role}: ${msg.text.take(400)}"
        }
        val existingList = existingFiles.take(25).joinToString(", ") {
            "${it.resolvedPackType.folderPrefix}/${it.relativePath}"
        }
        val fullPrompt = buildString {
            appendLine("Active Mod Project: ${project.name} (namespace: ${project.namespace})")
            if (existingList.isNotBlank()) {
                appendLine("Existing Pack Files: $existingList")
            }
            if (recentHistory.isNotBlank()) {
                appendLine("Recent Conversation:\n$recentHistory")
            }
            if (!attachedFileText.isNullOrBlank()) {
                appendLine("Attached File Context:\n${attachedFileText.take(3000)}")
            }
            appendLine("User Request: $prompt")
        }

        val inputContent: Content = content {
            if (attachedBitmap != null) {
                image(attachedBitmap)
            }
            text(fullPrompt)
        }

        withTimeout(60_000L) {
            chatPlannerModel.generateContentStream(inputContent).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }

    suspend fun generateModPackDelta(
        prompt: String,
        project: ModProject,
        existingFiles: List<ModFileEntry>,
        attachedBitmap: Bitmap?,
        attachedFileText: String?,
        isErrorRepair: Boolean = false
    ): GeneratedModDelta {
        val existingSummary = existingFiles
            .filter { !it.relativePath.endsWith(".png", ignoreCase = true) }
            .take(20)
            .joinToString("\n") { file ->
                val diagnosticNote = if (!file.valid) " [INVALID: ${file.validationNotes}]" else ""
                "--- ${file.resolvedPackType.name} :: ${file.relativePath}$diagnosticNote ---\n${file.contentText.take(1500)}"
            }

        val compilerPrompt = buildString {
            appendLine("Project Name: ${project.name}")
            appendLine("Namespace: ${project.namespace}")
            appendLine("Description: ${project.description}")
            if (isErrorRepair) {
                appendLine("MODE: REPAIR ERRORS. Fix all invalid JSON files, missing format_version fields, invalid identifiers, or broken pack links.")
            } else {
                appendLine("MODE: CREATE OR MODIFY ADD-ON. Preserve existing namespace '${project.namespace}' if already meaningful, or choose a fitting namespace if project is still 'New Bedrock Mod'.")
            }
            if (existingSummary.isNotBlank()) {
                appendLine("Current Project Files:\n$existingSummary")
            }
            if (!attachedFileText.isNullOrBlank()) {
                appendLine("User Attached Mod File:\n${attachedFileText.take(4000)}")
            }
            appendLine("User Instruction: $prompt")
        }

        val inputContent = content {
            if (attachedBitmap != null) {
                image(attachedBitmap)
            }
            text(compilerPrompt)
        }

        val response = withTimeout(60_000L) {
            packBuilderModel.generateContent(inputContent)
        }
        val rawJson = response.text ?: throw IllegalStateException("AI returned an empty pack payload.")
        val root = JSONObject(rawJson)

        val modName = root.optString("modName").ifBlank { project.name }
        val namespace = root.optString("namespace").ifBlank { project.namespace }
        val description = root.optString("description").ifBlank { project.description }
        val assistantSummary = root.optString("assistantSummary").ifBlank { "Updated Behavior & Resource Pack files." }

        val filesArray = root.optJSONArray("files")
        val files = mutableListOf<GeneratedFileSpec>()
        if (filesArray != null) {
            for (i in 0 until filesArray.length()) {
                val obj = filesArray.optJSONObject(i) ?: continue
                val packType = PackType.fromRaw(obj.optString("packType"))
                val relPath = obj.optString("relativePath").trim().trimStart('/')
                val contentText = obj.optString("contentText")
                if (relPath.isNotBlank() && contentText.isNotBlank()) {
                    files.add(
                        GeneratedFileSpec(
                            packType = packType,
                            relativePath = relPath,
                            contentText = contentText
                        )
                    )
                }
            }
        }

        val textureSpecsArray = root.optJSONArray("textureSpecs")
        val texturePrompts = linkedMapOf<String, String>()
        if (textureSpecsArray != null) {
            for (i in 0 until textureSpecsArray.length()) {
                val obj = textureSpecsArray.optJSONObject(i) ?: continue
                val relPath = obj.optString("relativePath").trim().trimStart('/')
                val texPrompt = obj.optString("prompt").trim()
                if (relPath.isNotBlank() && texPrompt.isNotBlank()) {
                    texturePrompts[relPath] = texPrompt
                }
            }
        }

        return GeneratedModDelta(
            modName = modName,
            namespace = namespace,
            description = description,
            assistantSummary = assistantSummary,
            files = files,
            texturePrompts = texturePrompts
        )
    }

    suspend fun generatePixelTexture(prompt: String): Result<Bitmap> {
        return try {
            val fullPrompt = "16-bit crisp pixel art game sprite texture for Minecraft Bedrock item or block: $prompt. Centered square composition, sharp pixel edges, no text, no blur."
            val response = withTimeout(60_000L) {
                textureModel.generateContent(fullPrompt)
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()?.image
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("Texture model did not return an image."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Texture generation timed out", e)
            Result.failure(IllegalStateException("Texture generation timed out after 60s."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Texture generation failed", e)
            Result.failure(e)
        }
    }
}
