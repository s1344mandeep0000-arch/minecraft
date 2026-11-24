package com.example.ui.screens

import android.content.Intent
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bedrock.BedrockPackEngine
import com.example.firebase.signOut
import com.example.model.ExportFormat
import com.example.model.StudioUiState
import com.example.ui.ModStudioViewModel
import com.example.ui.components.BottomChatComposer
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.ExportModDialog
import com.example.ui.components.ModPreviewSheet
import com.example.ui.components.ProjectDrawerContent
import com.example.ui.components.StreamingAssistantBubble
import com.example.ui.components.WelcomeStudioBanner
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldWarning
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RedstoneContainer
import com.example.ui.theme.RedstoneError
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoxelCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatStudioScreen(
    viewModel: ModStudioViewModel,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val projectsState by viewModel.projectsState.collectAsStateWithLifecycle()
    val activeProjectId by viewModel.activeProjectId.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val filesState by viewModel.filesState.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val pendingUserMessage by viewModel.pendingUserMessage.collectAsStateWithLifecycle()
    val generationStage by viewModel.generationStage.collectAsStateWithLifecycle()
    val pendingAttachment by viewModel.pendingAttachment.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val showPreviewSheet by viewModel.showPreviewSheet.collectAsStateWithLifecycle()
    val showExportDialog by viewModel.showExportDialog.collectAsStateWithLifecycle()
    val generatingTexturePath by viewModel.generatingTexturePath.collectAsStateWithLifecycle()

    var inputText by rememberSaveable { mutableStateOf("") }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }

    val projects = (projectsState as? StudioUiState.Success)?.data ?: emptyList()
    val activeProject = projects.firstOrNull { it.id == activeProjectId } ?: projects.firstOrNull()
    val messages = (messagesState as? StudioUiState.Success)?.data ?: emptyList()
    val files = (filesState as? StudioUiState.Success)?.data ?: emptyList()

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, pendingUserMessage, streamingText) {
        val totalItems = 1 + messages.size + (if (pendingUserMessage != null) 1 else 0) + (if (generationStage != null) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    // Zero-permission Android Photo Picker for image uploads
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = false
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }
                val fileName = resolveDisplayName(context, uri) ?: "concept_image.png"
                viewModel.attachImage(fileName, bitmap)
            }
        }
    }

    // Storage Access Framework document picker for .json, .mcpack, .mcaddon, .zip, .txt
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val fileName = resolveDisplayName(context, uri) ?: "mod_file.json"
                val content = withContext(Dispatchers.IO) {
                    BedrockPackEngine.extractAttachmentSummary(context, uri, fileName)
                }
                viewModel.attachFile(fileName, content)
            }
        }
    }

    // SAF CreateDocument launcher for exporting .mcaddon / .mcpack / .zip
    val saveExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val fmt = pendingExportFormat
        pendingExportFormat = null
        if (uri != null && fmt != null) {
            viewModel.exportToUri(context, uri, fmt)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProjectDrawerContent(
                projects = projects,
                activeProjectId = activeProject?.id,
                userEmail = userEmail,
                onCreateNewMod = {
                    scope.launch { drawerState.close() }
                    viewModel.createNewMod()
                },
                onSelectProject = { id ->
                    scope.launch { drawerState.close() }
                    viewModel.selectProject(id)
                },
                onDeleteProject = { id -> viewModel.deleteProject(id) },
                onPreviewMod = {
                    scope.launch { drawerState.close() }
                    viewModel.openPreviewSheet()
                },
                onFixErrors = {
                    scope.launch { drawerState.close() }
                    viewModel.fixModErrors()
                },
                onExportMod = {
                    scope.launch { drawerState.close() }
                    viewModel.openExportDialog()
                },
                onSignOut = {
                    scope.launch {
                        drawerState.close()
                        signOut(credentialManager, scope)
                    }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0C1411), ObsidianBg)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Top App Bar
                    Surface(
                        color = CharcoalSurface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("drawer_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Open saved projects drawer",
                                    tint = TextPrimary
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "Minecraft AI Mod Maker Studio",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (activeProject != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewModel.openPreviewSheet() }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (activeProject.validationErrorCount > 0L) GoldWarning else EmeraldPrimary
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${activeProject.name} • ${files.size} files",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.createNewMod() },
                                enabled = generationStage == null,
                                modifier = Modifier.testTag("topbar_new_mod_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddComment,
                                    contentDescription = "Create new mod",
                                    tint = EmeraldPrimary
                                )
                            }
                        }
                    }

                    // Transient status / error banner
                    AnimatedVisibility(visible = bannerMessage != null) {
                        val banner = bannerMessage
                        if (banner != null) {
                            Surface(
                                color = if (banner.isError) RedstoneContainer else EmeraldContainer,
                                border = BorderStroke(1.dp, if (banner.isError) RedstoneError else BorderEmerald),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = banner.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (banner.isError) RedstoneError else TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearBanner() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss message",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Scrollable Chat Area above bottom composer
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        when (val msgState = messagesState) {
                            is StudioUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = EmeraldPrimary)
                                }
                            }
                            is StudioUiState.Error -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = RedstoneContainer,
                                    border = BorderStroke(1.dp, RedstoneError),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = msgState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = RedstoneError,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                            is StudioUiState.Success -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .widthIn(max = 760.dp)
                                        .fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item(key = "welcome_banner") {
                                        WelcomeStudioBanner(
                                            onStarterClick = { starter ->
                                                viewModel.sendMessage(starter)
                                            }
                                        )
                                    }

                                    items(
                                        items = msgState.data,
                                        key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}" } }
                                    ) { msg ->
                                        ChatMessageItem(
                                            message = msg,
                                            project = activeProject,
                                            onPreviewMod = { viewModel.openPreviewSheet() },
                                            onExportMod = { viewModel.openExportDialog() },
                                            onFixErrors = { viewModel.fixModErrors() }
                                        )
                                    }

                                    val pendingMsg = pendingUserMessage
                                    if (pendingMsg != null) {
                                        item(key = "pending_user_msg") {
                                            ChatMessageItem(
                                                message = pendingMsg,
                                                project = activeProject,
                                                onPreviewMod = {},
                                                onExportMod = {},
                                                onFixErrors = {}
                                            )
                                        }
                                    }

                                    val activeStage = generationStage
                                    if (activeStage != null) {
                                        item(key = "streaming_ai_bubble") {
                                            StreamingAssistantBubble(
                                                streamingText = streamingText,
                                                stage = activeStage
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Permanent Bottom AI Chat Box pinned above keyboard & navigation bars
                    BottomChatComposer(
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        pendingAttachment = pendingAttachment,
                        onClearAttachment = { viewModel.clearAttachment() },
                        onPickImage = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onPickFile = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        isGenerating = generationStage != null,
                        onSend = {
                            val toSend = inputText
                            inputText = ""
                            viewModel.sendMessage(toSend)
                        }
                    )
                }
            }
        }
    }

    val currentProj = activeProject
    if (showPreviewSheet && currentProj != null) {
        ModPreviewSheet(
            project = currentProj,
            files = files,
            generatingTexturePath = generatingTexturePath,
            onGenerateTexture = { file -> viewModel.generateTextureForFile(file) },
            onFixErrors = { viewModel.fixModErrors() },
            onExportClick = {
                viewModel.closePreviewSheet()
                viewModel.openExportDialog()
            },
            onDismiss = { viewModel.closePreviewSheet() }
        )
    }

    if (showExportDialog && currentProj != null) {
        ExportModDialog(
            project = currentProj,
            onSaveToDevice = { format ->
                viewModel.closeExportDialog()
                pendingExportFormat = format
                val fileName = "${BedrockPackEngine.sanitizeFileName(currentProj.name)}.${format.extension}"
                saveExportLauncher.launch(fileName)
            },
            onShareArchive = { format ->
                viewModel.closeExportDialog()
                scope.launch {
                    val uriResult = viewModel.createShareUri(context, format)
                    uriResult.onSuccess { uri ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = format.mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Export ${currentProj.name}.${format.extension}")
                        )
                    }.onFailure { err ->
                        viewModel.reportError(err.localizedMessage ?: "Could not prepare mod export archive.")
                    }
                }
            },
            onDismiss = { viewModel.closeExportDialog() }
        )
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }
}
