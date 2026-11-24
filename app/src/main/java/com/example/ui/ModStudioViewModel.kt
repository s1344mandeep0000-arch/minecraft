package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.bedrock.BedrockPackEngine
import com.example.data.ModAiRepository
import com.example.data.ModProjectRepository
import com.example.model.AiGenerationStage
import com.example.model.AttachmentType
import com.example.model.ChatMessage
import com.example.model.ExportFormat
import com.example.model.ModFileEntry
import com.example.model.ModProject
import com.example.model.PackType
import com.example.model.PendingAttachment
import com.example.model.StudioBannerMessage
import com.example.model.StudioUiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val VM_TAG = "ModStudioViewModel"

@OptIn(ExperimentalCoroutinesApi::class)
class ModStudioViewModel(
    private val projectRepository: ModProjectRepository,
    private val aiRepository: ModAiRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _pendingUserMessage = MutableStateFlow<ChatMessage?>(null)
    val pendingUserMessage: StateFlow<ChatMessage?> = _pendingUserMessage.asStateFlow()

    private val _generationStage = MutableStateFlow<AiGenerationStage?>(null)
    val generationStage: StateFlow<AiGenerationStage?> = _generationStage.asStateFlow()

    private val _pendingAttachment = MutableStateFlow<PendingAttachment?>(null)
    val pendingAttachment: StateFlow<PendingAttachment?> = _pendingAttachment.asStateFlow()

    private val _bannerMessage = MutableStateFlow<StudioBannerMessage?>(null)
    val bannerMessage: StateFlow<StudioBannerMessage?> = _bannerMessage.asStateFlow()

    private val _showPreviewSheet = MutableStateFlow(false)
    val showPreviewSheet: StateFlow<Boolean> = _showPreviewSheet.asStateFlow()

    private val _showExportDialog = MutableStateFlow(false)
    val showExportDialog: StateFlow<Boolean> = _showExportDialog.asStateFlow()

    private val _generatingTexturePath = MutableStateFlow<String?>(null)
    val generatingTexturePath: StateFlow<String?> = _generatingTexturePath.asStateFlow()

    private var hasInitializedFirstProject = false

    val projectsState: StateFlow<StudioUiState<List<ModProject>>> =
        projectRepository.observeProjects(currentUserId)
            .onEach { list ->
                val currentActive = _activeProjectId.value
                if (list.isNotEmpty() && (currentActive == null || list.none { it.id == currentActive })) {
                    _activeProjectId.value = list.first().id
                } else if (list.isEmpty() && !hasInitializedFirstProject) {
                    hasInitializedFirstProject = true
                    createNewMod(initialName = "New Bedrock Mod")
                }
            }
            .map<List<ModProject>, StudioUiState<List<ModProject>>> { StudioUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(VM_TAG, "Error observing projects", rootError)
                emit(StudioUiState.Error(rootError.localizedMessage ?: "Failed to load saved projects."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = StudioUiState.Loading
            )

    val messagesState: StateFlow<StudioUiState<List<ChatMessage>>> =
        _activeProjectId
            .flatMapLatest { projectId ->
                if (projectId.isNullOrBlank()) {
                    flowOf<StudioUiState<List<ChatMessage>>>(StudioUiState.Success(emptyList()))
                } else {
                    projectRepository.observeMessages(currentUserId, projectId)
                        .map<List<ChatMessage>, StudioUiState<List<ChatMessage>>> { StudioUiState.Success(it) }
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            val rootError = firestoreError ?: error
                            Log.w(VM_TAG, "Error observing messages", rootError)
                            emit(StudioUiState.Error(rootError.localizedMessage ?: "Failed to load chat history."))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = StudioUiState.Loading
            )

    val filesState: StateFlow<StudioUiState<List<ModFileEntry>>> =
        _activeProjectId
            .flatMapLatest { projectId ->
                if (projectId.isNullOrBlank()) {
                    flowOf<StudioUiState<List<ModFileEntry>>>(StudioUiState.Success(emptyList()))
                } else {
                    projectRepository.observeFiles(currentUserId, projectId)
                        .map<List<ModFileEntry>, StudioUiState<List<ModFileEntry>>> { StudioUiState.Success(it) }
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            val rootError = firestoreError ?: error
                            Log.w(VM_TAG, "Error observing pack files", rootError)
                            emit(StudioUiState.Error(rootError.localizedMessage ?: "Failed to load pack files."))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = StudioUiState.Loading
            )

    fun activeProject(): ModProject? {
        val projects = (projectsState.value as? StudioUiState.Success)?.data ?: return null
        val activeId = _activeProjectId.value
        return projects.firstOrNull { it.id == activeId } ?: projects.firstOrNull()
    }

    fun currentFiles(): List<ModFileEntry> =
        (filesState.value as? StudioUiState.Success)?.data ?: emptyList()

    fun currentMessages(): List<ChatMessage> =
        (messagesState.value as? StudioUiState.Success)?.data ?: emptyList()

    fun selectProject(projectId: String) {
        if (_generationStage.value != null) return
        _activeProjectId.value = projectId
    }

    fun openPreviewSheet() {
        _showPreviewSheet.value = true
    }

    fun closePreviewSheet() {
        _showPreviewSheet.value = false
    }

    fun openExportDialog() {
        _showExportDialog.value = true
    }

    fun closeExportDialog() {
        _showExportDialog.value = false
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun reportError(message: String) {
        _bannerMessage.value = StudioBannerMessage(message, isError = true)
    }

    fun attachImage(name: String, bitmap: Bitmap) {
        // Scale down oversized bitmaps so multimodal payloads remain well under 20MB
        val maxDim = 1024
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        _pendingAttachment.value = PendingAttachment(
            name = name,
            type = AttachmentType.IMAGE,
            bitmap = scaled
        )
    }

    fun attachFile(name: String, content: String) {
        _pendingAttachment.value = PendingAttachment(
            name = name,
            type = AttachmentType.FILE,
            textContent = content
        )
    }

    fun clearAttachment() {
        _pendingAttachment.value = null
    }

    fun createNewMod(initialName: String = "New Bedrock Mod") {
        if (_generationStage.value != null) return
        viewModelScope.launch {
            val result = projectRepository.createProjectWithWelcome(name = initialName)
            result.onSuccess { newId ->
                _activeProjectId.value = newId
                _bannerMessage.value = StudioBannerMessage("Created a fresh mod workspace.")
            }.onFailure { err ->
                _bannerMessage.value = StudioBannerMessage(
                    err.localizedMessage ?: "Could not create new mod project.",
                    isError = true
                )
            }
        }
    }

    fun deleteProject(projectId: String) {
        if (_generationStage.value != null) return
        viewModelScope.launch {
            val result = projectRepository.deleteProject(projectId)
            result.onSuccess {
                if (_activeProjectId.value == projectId) {
                    _activeProjectId.value = null
                }
                _bannerMessage.value = StudioBannerMessage("Deleted saved project.")
            }.onFailure { err ->
                _bannerMessage.value = StudioBannerMessage(
                    err.localizedMessage ?: "Failed to delete project.",
                    isError = true
                )
            }
        }
    }

    fun sendMessage(rawPrompt: String) {
        val trimmed = rawPrompt.trim()
        val attachment = _pendingAttachment.value
        if (trimmed.isEmpty() && attachment == null) return
        if (_generationStage.value != null) return

        val lower = trimmed.lowercase(Locale.US)
        when {
            lower == "/new" || lower == "create new mod" -> {
                _pendingAttachment.value = null
                createNewMod()
                return
            }
            lower == "/preview" || lower == "preview mod" -> {
                _pendingAttachment.value = null
                openPreviewSheet()
                return
            }
            lower == "/export" || lower == "export mod" -> {
                _pendingAttachment.value = null
                openExportDialog()
                return
            }
            lower == "/fix" || lower == "fix mod errors" -> {
                _pendingAttachment.value = null
                fixModErrors()
                return
            }
        }

        val prompt = trimmed.ifEmpty {
            "Analyze the attached ${attachment?.name ?: "file"} and build or update my Minecraft Bedrock mod from it."
        }
        runAiModTurn(
            prompt = prompt,
            attachment = attachment,
            isErrorRepair = false
        )
    }

    fun fixModErrors() {
        if (_generationStage.value != null) return
        val project = activeProject() ?: return
        val files = currentFiles()
        val invalidFiles = files.filter { !it.valid }
        val prompt = if (invalidFiles.isEmpty()) {
            "Audit all Behavior Pack and Resource Pack files in ${project.name}, fix any subtle Bedrock 1.21.50 schema issues or missing texture links, and confirm everything passes validation."
        } else {
            val issues = invalidFiles.joinToString("\n") {
                "- ${it.resolvedPackType.folderPrefix}/${it.relativePath}: ${it.validationNotes}"
            }
            "Fix all detected mod errors in ${project.name}:\n$issues"
        }
        runAiModTurn(
            prompt = prompt,
            attachment = null,
            isErrorRepair = true
        )
    }

    private fun runAiModTurn(
        prompt: String,
        attachment: PendingAttachment?,
        isErrorRepair: Boolean
    ) {
        viewModelScope.launch {
            var project = activeProject()
            if (project == null) {
                val created = projectRepository.createProjectWithWelcome()
                if (created.isFailure) {
                    _bannerMessage.value = StudioBannerMessage(
                        created.exceptionOrNull()?.localizedMessage ?: "Failed to initialize project.",
                        isError = true
                    )
                    return@launch
                }
                _activeProjectId.value = created.getOrNull()
                project = activeProject() ?: return@launch
            }

            val existingFiles = currentFiles()
            val history = currentMessages()

            _pendingAttachment.value = null
            _pendingUserMessage.value = ChatMessage(
                userId = currentUserId,
                projectId = project.id,
                role = "USER",
                text = prompt,
                attachmentName = attachment?.name.orEmpty(),
                attachmentType = (attachment?.type ?: AttachmentType.NONE).name
            )
            _streamingText.value = ""
            _generationStage.value = AiGenerationStage.UNDERSTANDING

            try {
                // 1. Stream conversational AI response for low-latency ChatGPT feel
                val streamedBuilder = StringBuilder()
                aiRepository.streamConversationalReply(
                    prompt = prompt,
                    project = project,
                    history = history,
                    existingFiles = existingFiles,
                    attachedBitmap = attachment?.bitmap,
                    attachedFileText = attachment?.textContent
                ).collect { chunk ->
                    streamedBuilder.append(chunk)
                    _streamingText.value = streamedBuilder.toString()
                }

                // 2. Generate structured Bedrock BP & RP pack files
                _generationStage.value = AiGenerationStage.ARCHITECTING
                val delta = aiRepository.generateModPackDelta(
                    prompt = prompt,
                    project = project,
                    existingFiles = existingFiles,
                    attachedBitmap = attachment?.bitmap,
                    attachedFileText = attachment?.textContent,
                    isErrorRepair = isErrorRepair
                )

                val isFirstRealRename = project.name == "New Bedrock Mod" || project.name == "Untitled Mod"
                val updatedNamespace = if (isFirstRealRename) {
                    BedrockPackEngine.sanitizeNamespace(delta.namespace)
                } else {
                    project.namespace
                }
                val updatedName = if (isFirstRealRename) delta.modName else project.name
                val updatedDesc = delta.description.ifBlank { project.description }

                val baseProject = project.copy(
                    name = updatedName,
                    namespace = updatedNamespace,
                    description = updatedDesc
                )

                // Merge existing files + newly generated files
                val fileMap = existingFiles.associateBy {
                    "${it.resolvedPackType.name}:${it.relativePath}"
                }.toMutableMap()

                for (spec in delta.files) {
                    val key = "${spec.packType.name}:${spec.relativePath}"
                    val existing = fileMap[key]
                    val docId = existing?.id?.ifBlank { null }
                        ?: BedrockPackEngine.toFirestoreDocId(spec.packType, spec.relativePath)
                    fileMap[key] = ModFileEntry(
                        id = docId,
                        userId = currentUserId,
                        projectId = baseProject.id,
                        packType = spec.packType.name,
                        relativePath = spec.relativePath,
                        contentText = spec.contentText,
                        textureBase64 = existing?.textureBase64.orEmpty(),
                        valid = true,
                        validationNotes = ""
                    )
                }

                // 3. Craft up to 2 pixel-art textures via gemini-nano-banana-2.1 if requested
                val textureEntries = delta.texturePrompts.entries.take(2)
                if (textureEntries.isNotEmpty()) {
                    _generationStage.value = AiGenerationStage.GENERATING_TEXTURES
                    for ((relPath, texPrompt) in textureEntries) {
                        val key = "${PackType.RESOURCE_PACK.name}:$relPath"
                        val existing = fileMap[key]
                        val docId = existing?.id?.ifBlank { null }
                            ?: BedrockPackEngine.toFirestoreDocId(PackType.RESOURCE_PACK, relPath)
                        val texResult = aiRepository.generatePixelTexture(texPrompt)
                        val base64 = texResult.getOrNull()?.let { bmp ->
                            BedrockPackEngine.pixelateBitmapToPngBase64(bmp, 32)
                        }.orEmpty()
                        val note = if (texResult.isFailure) {
                            texResult.exceptionOrNull()?.localizedMessage ?: "Texture synthesis pending."
                        } else ""

                        fileMap[key] = ModFileEntry(
                            id = docId,
                            userId = currentUserId,
                            projectId = baseProject.id,
                            packType = PackType.RESOURCE_PACK.name,
                            relativePath = relPath,
                            contentText = "",
                            textureBase64 = base64.ifBlank { existing?.textureBase64.orEmpty() },
                            valid = true,
                            validationNotes = note
                        )
                    }
                }

                // Also register remaining textureSpecs as texture entries ready for on-demand generation
                for ((relPath, texPrompt) in delta.texturePrompts.entries.drop(2)) {
                    val key = "${PackType.RESOURCE_PACK.name}:$relPath"
                    if (!fileMap.containsKey(key)) {
                        val docId = BedrockPackEngine.toFirestoreDocId(PackType.RESOURCE_PACK, relPath)
                        fileMap[key] = ModFileEntry(
                            id = docId,
                            userId = currentUserId,
                            projectId = baseProject.id,
                            packType = PackType.RESOURCE_PACK.name,
                            relativePath = relPath,
                            contentText = texPrompt,
                            textureBase64 = "",
                            valid = true,
                            validationNotes = "Tap Generate Texture in Preview to paint this sprite."
                        )
                    }
                }

                // 4. Ensure manifests & validate all pack files
                _generationStage.value = AiGenerationStage.VALIDATING
                val withManifests = BedrockPackEngine.ensureManifestFiles(
                    project = baseProject,
                    existingFiles = fileMap.values.toList(),
                    userId = currentUserId
                )
                val validation = BedrockPackEngine.validatePackFiles(withManifests)
                val counts = BedrockPackEngine.computeComponentCounts(validation.files)

                val finalProject = baseProject.copy(
                    blocksCount = counts.blocksCount,
                    mobsCount = counts.mobsCount,
                    weaponsCount = counts.weaponsCount,
                    armorCount = counts.armorCount,
                    itemsCount = counts.itemsCount,
                    structuresCount = counts.structuresCount,
                    recipesCount = counts.recipesCount,
                    texturesCount = counts.texturesCount,
                    validationErrorCount = validation.errorCount,
                    validationSummary = validation.summaryText
                )

                // 5. Persist conversation turn + pack files atomically to Firestore
                _generationStage.value = AiGenerationStage.SAVING
                val existingIds = existingFiles.mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()
                val assistantReply = streamedBuilder.toString().ifBlank { delta.assistantSummary }

                val saveResult = projectRepository.saveUserAndAssistantTurn(
                    project = finalProject,
                    userText = prompt,
                    attachmentName = attachment?.name.orEmpty(),
                    attachmentType = attachment?.type ?: AttachmentType.NONE,
                    assistantText = assistantReply,
                    modSnapshotSummary = delta.assistantSummary,
                    updatedFiles = validation.files,
                    existingFileIds = existingIds
                )

                saveResult.onFailure { err ->
                    _bannerMessage.value = StudioBannerMessage(
                        err.localizedMessage ?: "Failed to save mod changes to cloud.",
                        isError = true
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(VM_TAG, "AI mod generation timed out", e)
                _bannerMessage.value = StudioBannerMessage(
                    "AI generation timed out after 60 seconds. Please try a shorter request or retry.",
                    isError = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "AI mod generation failed", e)
                _bannerMessage.value = StudioBannerMessage(
                    e.localizedMessage ?: "AI generation failed. Check connection or Firebase App Check setup.",
                    isError = true
                )
            } finally {
                _generationStage.value = null
                _streamingText.value = ""
                _pendingUserMessage.value = null
            }
        }
    }

    fun generateTextureForFile(file: ModFileEntry) {
        if (_generatingTexturePath.value != null) return
        val project = activeProject() ?: return
        viewModelScope.launch {
            _generatingTexturePath.value = file.relativePath
            try {
                val cleanName = file.relativePath
                    .substringAfterLast('/')
                    .substringBeforeLast('.')
                    .replace('_', ' ')
                val texPrompt = file.contentText.takeIf { it.isNotBlank() && !it.trim().startsWith("{") }
                    ?: "$cleanName for Minecraft mod ${project.name}"
                val result = aiRepository.generatePixelTexture(texPrompt)
                result.onSuccess { bitmap ->
                    val base64 = BedrockPackEngine.pixelateBitmapToPngBase64(bitmap, 32)
                    val updatedFile = file.copy(
                        textureBase64 = base64,
                        valid = true,
                        validationNotes = ""
                    )
                    val allFiles = currentFiles().map {
                        if (it.id == file.id) updatedFile else it
                    }
                    val counts = BedrockPackEngine.computeComponentCounts(allFiles)
                    val updatedProject = project.copy(texturesCount = counts.texturesCount)
                    val existingIds = currentFiles().mapNotNull { it.id.takeIf(String::isNotBlank) }.toSet()

                    projectRepository.updateSingleTextureFile(
                        project = updatedProject,
                        file = updatedFile,
                        isExistingFile = existingIds.contains(updatedFile.id)
                    ).onSuccess {
                        _bannerMessage.value = StudioBannerMessage("Generated pixel texture for ${file.relativePath}.")
                    }.onFailure { err ->
                        _bannerMessage.value = StudioBannerMessage(
                            err.localizedMessage ?: "Could not save texture.",
                            isError = true
                        )
                    }
                }.onFailure { err ->
                    _bannerMessage.value = StudioBannerMessage(
                        err.localizedMessage ?: "Texture generation failed.",
                        isError = true
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _generatingTexturePath.value = null
            }
        }
    }

    fun exportToUri(context: Context, uri: Uri, format: ExportFormat) {
        val project = activeProject() ?: return
        val files = currentFiles()
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        BedrockPackEngine.writeArchive(project, files, format, out)
                    } ?: error("Unable to open destination file.")
                }
                _bannerMessage.value = StudioBannerMessage("Exported ${project.name}.${format.extension} successfully.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(VM_TAG, "Export to URI failed", e)
                _bannerMessage.value = StudioBannerMessage(
                    e.localizedMessage ?: "Export failed.",
                    isError = true
                )
            }
        }
    }

    suspend fun createShareUri(context: Context, format: ExportFormat): Result<Uri> {
        val project = activeProject()
            ?: return Result.failure(IllegalStateException("No active mod project to export."))
        val files = currentFiles()
        return try {
            val uri = withContext(Dispatchers.IO) {
                BedrockPackEngine.exportToCacheAndGetUri(context, project, files, format)
            }
            Result.success(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(VM_TAG, "Share archive creation failed", e)
            Result.failure(e)
        }
    }

    companion object {
        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ModStudioViewModel(
                    projectRepository = ModProjectRepository(db),
                    aiRepository = ModAiRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    }
}
