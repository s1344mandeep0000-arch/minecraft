package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AiGenerationStage
import com.example.model.AttachmentType
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.ModProject
import com.example.model.PendingAttachment
import com.example.ui.theme.AssistantBubbleBg
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldWarning
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.RedstoneContainer
import com.example.ui.theme.RedstoneError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UserBubbleBg
import com.example.ui.theme.VoxelCard
import com.example.ui.theme.VoxelElevated
import com.example.ui.theme.VoxelInputBg

private val STARTER_IDEAS = listOf(
    "Create a Dragon World Mod",
    "Add a powerful Fire Dragon Boss",
    "Cyberpunk Laser Weapons & Armor",
    "Emerald Golem Companion & Ore"
)

@Composable
fun WelcomeStudioBanner(
    onStarterClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = VoxelCard,
            border = BorderStroke(1.dp, BorderEmerald)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = "Minecraft AI Mod Maker Studio banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, VoxelCard)
                                )
                            )
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = ObsidianBg.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, BorderEmerald)
                    ) {
                        Text(
                            text = "BEDROCK 1.21.50 ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "What would you like to build today?",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Chat naturally to create custom mobs, blocks, weapons, armor, recipes, structures, and pixel textures.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        STARTER_IDEAS.forEachIndexed { idx, prompt ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldContainer.copy(alpha = 0.65f),
                                border = BorderStroke(1.dp, BorderEmerald),
                                modifier = Modifier
                                    .testTag("starter_prompt_$idx")
                                    .clickable { onStarterClick(prompt) }
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    project: ModProject?,
    onPreviewMod: () -> Unit,
    onExportMod: () -> Unit,
    onFixErrors: () -> Unit
) {
    val role = MessageRole.fromRaw(message.role)
    val isUser = role == MessageRole.USER

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { it / 6 }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                Image(
                    painter = painterResource(id = R.drawable.img_ai_avatar),
                    contentDescription = "AI Assistant",
                    modifier = Modifier
                        .padding(top = 2.dp, end = 10.dp)
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Column(
                modifier = Modifier.widthIn(max = if (isUser) 310.dp else 560.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = if (isUser) 16.dp else 4.dp,
                        topEnd = if (isUser) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    color = if (isUser) UserBubbleBg else AssistantBubbleBg,
                    border = BorderStroke(1.dp, if (isUser) BorderEmerald else BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                        if (message.attachmentName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = VoxelElevated,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (AttachmentType.fromRaw(message.attachmentType) == AttachmentType.IMAGE) {
                                            Icons.Rounded.Image
                                        } else {
                                            Icons.Rounded.AttachFile
                                        },
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = message.attachmentName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }

                // Inline Mod Snapshot & Action Card for turns that built/updated pack files
                if (!isUser && message.generatedFileCount > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = VoxelCard,
                        border = BorderStroke(
                            1.dp,
                            if (message.validationErrorCount > 0L) GoldWarning.copy(alpha = 0.55f) else BorderEmerald
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = project?.name ?: "Bedrock Add-on Updated",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (message.validationErrorCount == 0L) EmeraldContainer else RedstoneContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (message.validationErrorCount == 0L) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = if (message.validationErrorCount == 0L) EmeraldPrimary else RedstoneError,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (message.validationErrorCount == 0L) {
                                                "${message.generatedFileCount} Files Valid"
                                            } else {
                                                "${message.validationErrorCount} Error(s)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (message.validationErrorCount == 0L) EmeraldPrimary else RedstoneError
                                        )
                                    }
                                }
                            }

                            if (message.modSnapshotSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.modSnapshotSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            if (project != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (project.mobsCount > 0) ComponentBadge("${project.mobsCount} Mobs")
                                    if (project.blocksCount > 0) ComponentBadge("${project.blocksCount} Blocks")
                                    if (project.weaponsCount > 0) ComponentBadge("${project.weaponsCount} Weapons")
                                    if (project.armorCount > 0) ComponentBadge("${project.armorCount} Armor")
                                    if (project.itemsCount > 0) ComponentBadge("${project.itemsCount} Items")
                                    if (project.recipesCount > 0) ComponentBadge("${project.recipesCount} Recipes")
                                    if (project.texturesCount > 0) ComponentBadge("${project.texturesCount} Textures")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onPreviewMod,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderEmerald),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("inline_preview_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Preview",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary
                                    )
                                }

                                OutlinedButton(
                                    onClick = onExportMod,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderEmerald),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("inline_export_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Export",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary
                                    )
                                }

                                if (message.validationErrorCount > 0L) {
                                    OutlinedButton(
                                        onClick = onFixErrors,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, RedstoneError.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .height(40.dp)
                                            .testTag("inline_fix_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Build,
                                            contentDescription = "Fix mod errors",
                                            tint = RedstoneError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = VoxelElevated,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StreamingAssistantBubble(
    streamingText: String,
    stage: AiGenerationStage
) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_ai_avatar),
            contentDescription = "AI Assistant generating",
            modifier = Modifier
                .padding(top = 2.dp, end = 10.dp)
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Surface(
            modifier = Modifier.widthIn(max = 560.dp),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = AssistantBubbleBg,
            border = BorderStroke(1.dp, BorderEmerald.copy(alpha = pulseAlpha))
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = EmeraldPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stage.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldPrimary
                    )
                }

                if (streamingText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = streamingText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun BottomChatComposer(
    inputText: String,
    onInputChange: (String) -> Unit,
    pendingAttachment: PendingAttachment?,
    onClearAttachment: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    val canSend = (inputText.isNotBlank() || pendingAttachment != null) && !isGenerating

    val sendBgColor by animateColorAsState(
        targetValue = if (canSend) EmeraldPrimary else VoxelElevated,
        animationSpec = tween(200),
        label = "sendBg"
    )
    val sendScale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sendScale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (pendingAttachment != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VoxelElevated,
                    border = BorderStroke(1.dp, BorderEmerald),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bmp = pendingAttachment.bitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Attached image preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AttachFile,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pendingAttachment.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onClearAttachment,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove attachment",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = VoxelInputBg,
                border = BorderStroke(1.dp, if (inputText.isNotBlank()) BorderEmerald else BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box {
                        IconButton(
                            onClick = { showAttachMenu = true },
                            enabled = !isGenerating,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("attach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Attach file or upload image",
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false },
                            containerColor = VoxelElevated
                        ) {
                            DropdownMenuItem(
                                text = { Text("Upload Image", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = EmeraldPrimary
                                    )
                                },
                                onClick = {
                                    showAttachMenu = false
                                    onPickImage()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attach Mod File (.json, .mcpack, .zip)", color = TextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.AttachFile,
                                        contentDescription = null,
                                        tint = EmeraldPrimary
                                    )
                                },
                                onClick = {
                                    showAttachMenu = false
                                    onPickFile()
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Describe your Minecraft Mod idea...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                            cursorBrush = SolidColor(EmeraldPrimary),
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_input_field")
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .scale(sendScale)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(sendBgColor)
                            .testTag("send_button")
                            .clickable(enabled = canSend) { onSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send prompt",
                                tint = if (canSend) OnEmeraldDark else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
