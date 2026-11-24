package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExportFormat
import com.example.model.ModProject
import com.example.ui.theme.BorderEmerald
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoxelCard
import com.example.ui.theme.VoxelElevated

@Composable
fun ExportModDialog(
    project: ModProject,
    onSaveToDevice: (ExportFormat) -> Unit,
    onShareArchive: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.MCADDON) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoxelElevated,
        icon = {
            Icon(
                imageVector = Icons.Rounded.FolderZip,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(30.dp)
            )
        },
        title = {
            Text(
                text = "Export \"${project.name}\"",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Choose your Minecraft Bedrock package format:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                ExportFormat.entries.forEach { format ->
                    val selected = selectedFormat == format
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) EmeraldContainer.copy(alpha = 0.5f) else VoxelCard,
                        border = BorderStroke(1.dp, if (selected) BorderEmerald else BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { selectedFormat = format },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = EmeraldPrimary,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = format.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = format.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onShareArchive(selectedFormat) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderEmerald),
                    modifier = Modifier.testTag("export_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", color = TextPrimary)
                }

                Button(
                    onClick = { onSaveToDevice(selectedFormat) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = OnEmeraldDark
                    ),
                    modifier = Modifier.testTag("export_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SaveAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save File", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
