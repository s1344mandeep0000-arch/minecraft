package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bedrock.BedrockPackEngine
import com.example.model.ModFileEntry
import com.example.model.ModProject
import com.example.model.PackType
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.RedstoneContainer
import com.example.ui.theme.RedstoneError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoxelCard
import com.example.ui.theme.VoxelElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModPreviewSheet(
    project: ModProject,
    files: List<ModFileEntry>,
    generatingTexturePath: String?,
    onGenerateTexture: (ModFileEntry) -> Unit,
    onFixErrors: () -> Unit,
    onExportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFile by remember { mutableStateOf<ModFileEntry?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CharcoalSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${project.namespace} • Bedrock ${project.targetVersion} • ${files.size} files",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onExportClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderEmerald),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close preview",
                            tint = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val tabs = listOf("Overview", "Pack Files (${files.size})", "Diagnostics")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CharcoalSurface,
                contentColor = EmeraldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EmeraldPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            selectedFile = null
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) EmeraldPrimary else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> OverviewAndTexturesTab(
                    project = project,
                    files = files,
                    generatingTexturePath = generatingTexturePath,
                    onGenerateTexture = onGenerateTexture
                )
                1 -> {
                    val activeFile = selectedFile
                    if (activeFile != null) {
                        FileCodeInspector(
                            file = activeFile,
                            onBack = { selectedFile = null },
                            onGenerateTexture = onGenerateTexture,
                            isGeneratingTexture = generatingTexturePath == activeFile.relativePath
                        )
                    } else {
                        PackFilesExplorerTab(
                            files = files,
                            onFileClick = { selectedFile = it }
                        )
                    }
                }
                2 -> DiagnosticsTab(
                    project = project,
                    files = files,
                    onFixErrors = {
                        onDismiss()
                        onFixErrors()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OverviewAndTexturesTab(
    project: ModProject,
    files: List<ModFileEntry>,
    generatingTexturePath: String?,
    onGenerateTexture: (ModFileEntry) -> Unit
) {
    val textureFiles = files.filter { it.relativePath.endsWith(".png", ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (project.description.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VoxelCard,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            MetricTile("Blocks", project.blocksCount, Modifier.weight(1f))
            MetricTile("Mobs", project.mobsCount, Modifier.weight(1f))
            MetricTile("Weapons", project.weaponsCount, Modifier.weight(1f))
            MetricTile("Armor", project.armorCount, Modifier.weight(1f))
            MetricTile("Items", project.itemsCount, Modifier.weight(1f))
            MetricTile("Structures", project.structuresCount, Modifier.weight(1f))
            MetricTile("Recipes", project.recipesCount, Modifier.weight(1f))
            MetricTile("Textures", project.texturesCount, Modifier.weight(1f))
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VoxelCard,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PACK IDENTIFIERS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("BP UUID: ${project.bpUuid}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("RP UUID: ${project.rpUuid}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }

        Text(
            text = "PIXEL TEXTURES (${textureFiles.size})",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )

        if (textureFiles.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VoxelCard,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No textures registered yet. Ask the AI to add items, blocks, weapons, or pixel textures!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            textureFiles.forEach { tex ->
                val bmp = remember(tex.textureBase64) { BedrockPackEngine.decodeBase64Bitmap(tex.textureBase64) }
                val isGenerating = generatingTexturePath == tex.relativePath
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VoxelCard,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = tex.relativePath,
                                    filterQuality = FilterQuality.None,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tex.relativePath,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (bmp != null) "32×32 PNG Ready" else tex.validationNotes.ifBlank { "Pending pixel texture" },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { onGenerateTexture(tex) },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderEmerald)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (bmp == null) "Paint" else "Redo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, count: Long, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = VoxelCard,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (count > 0L) EmeraldPrimary else TextMuted
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PackFilesExplorerTab(
    files: List<ModFileEntry>,
    onFileClick: (ModFileEntry) -> Unit
) {
    var filter by remember { mutableStateOf<PackType?>(null) }
    val filtered = remember(files, filter) {
        if (filter == null) files else files.filter { it.resolvedPackType == filter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filter == null,
                onClick = { filter = null },
                label = { Text("All (${files.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldContainer,
                    selectedLabelColor = EmeraldPrimary
                )
            )
            FilterChip(
                selected = filter == PackType.BEHAVIOR_PACK,
                onClick = { filter = PackType.BEHAVIOR_PACK },
                label = { Text("BP (${files.count { it.resolvedPackType == PackType.BEHAVIOR_PACK }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldContainer,
                    selectedLabelColor = EmeraldPrimary
                )
            )
            FilterChip(
                selected = filter == PackType.RESOURCE_PACK,
                onClick = { filter = PackType.RESOURCE_PACK },
                label = { Text("RP (${files.count { it.resolvedPackType == PackType.RESOURCE_PACK }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldContainer,
                    selectedLabelColor = EmeraldPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id.ifBlank { "${it.packType}_${it.relativePath}" } }) { file ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VoxelCard,
                    border = BorderStroke(1.dp, if (file.valid) BorderSubtle else RedstoneError.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFileClick(file) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (file.isTexture) Icons.Rounded.Image else Icons.Rounded.Code,
                            contentDescription = null,
                            tint = if (file.valid) EmeraldPrimary else RedstoneError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${file.resolvedPackType.folderPrefix}/${file.relativePath}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (file.validationNotes.isNotBlank()) {
                                Text(
                                    text = file.validationNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (file.valid) TextSecondary else RedstoneError,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = if (file.valid) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = if (file.valid) EmeraldPrimary else RedstoneError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileCodeInspector(
    file: ModFileEntry,
    onBack: () -> Unit,
    onGenerateTexture: (ModFileEntry) -> Unit,
    isGeneratingTexture: Boolean
) {
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to files",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "${file.resolvedPackType.folderPrefix}/${file.relativePath}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = EmeraldPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!file.isTexture) {
                IconButton(onClick = { clipboard.setText(AnnotatedString(file.contentText)) }) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy code",
                        tint = TextSecondary
                    )
                }
            }
        }

        if (file.isTexture) {
            val bmp = remember(file.textureBase64) { BedrockPackEngine.decodeBase64Bitmap(file.textureBase64) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = file.relativePath,
                            filterQuality = FilterQuality.None,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Image,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onGenerateTexture(file) },
                    enabled = !isGeneratingTexture,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = OnEmeraldDark
                    )
                ) {
                    Text(if (isGeneratingTexture) "Painting Pixel Texture…" else "Generate / Refresh Pixel Texture")
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianBg,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text(
                        text = file.contentText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsTab(
    project: ModProject,
    files: List<ModFileEntry>,
    onFixErrors: () -> Unit
) {
    val invalidFiles = files.filter { !it.valid }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (invalidFiles.isEmpty()) EmeraldContainer.copy(alpha = 0.4f) else RedstoneContainer,
            border = BorderStroke(1.dp, if (invalidFiles.isEmpty()) BorderEmerald else RedstoneError),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (invalidFiles.isEmpty()) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = if (invalidFiles.isEmpty()) EmeraldPrimary else RedstoneError
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = project.validationSummary.ifBlank { "All files checked." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }

        invalidFiles.forEach { file ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = VoxelCard,
                border = BorderStroke(1.dp, RedstoneError.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "${file.resolvedPackType.folderPrefix}/${file.relativePath}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = RedstoneError
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = file.validationNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Button(
            onClick = onFixErrors,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("preview_fix_errors_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = OnEmeraldDark
            )
        ) {
            Icon(imageVector = Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (invalidFiles.isEmpty()) "Run AI Deep Schema Audit" else "Fix All Errors with AI",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
