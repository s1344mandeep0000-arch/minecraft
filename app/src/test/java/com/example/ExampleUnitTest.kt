package com.example

import com.example.bedrock.BedrockPackEngine
import com.example.model.ExportFormat
import com.example.model.ModFileEntry
import com.example.model.ModProject
import com.example.model.PackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ExampleUnitTest {

    @Test
    fun bedrockPackEngine_sanitizesCountsAndPackagesMcaddonAndMcpack() {
        assertEquals("dragon_world", BedrockPackEngine.sanitizeNamespace("Dragon World!"))
        assertEquals("minecraft_addon", BedrockPackEngine.sanitizeNamespace("minecraft"))
        assertEquals("Dragon_World_Mod", BedrockPackEngine.sanitizeFileName("Dragon World Mod"))

        val project = ModProject(
            id = "proj_1",
            userId = "user_1",
            name = "Dragon World Mod",
            namespace = "dragon_world",
            description = "Dragons and dragon gear",
            bpUuid = "11111111-2222-4333-8444-555555555555",
            rpUuid = "66666666-7777-4888-8999-aaaaaaaaaaaa"
        )

        val files = listOf(
            ModFileEntry(
                id = "bp_manifest",
                userId = "user_1",
                projectId = "proj_1",
                packType = PackType.BEHAVIOR_PACK.name,
                relativePath = "manifest.json",
                contentText = """{"format_version":2}"""
            ),
            ModFileEntry(
                id = "rp_manifest",
                userId = "user_1",
                projectId = "proj_1",
                packType = PackType.RESOURCE_PACK.name,
                relativePath = "manifest.json",
                contentText = """{"format_version":2}"""
            ),
            ModFileEntry(
                id = "bp_mob",
                userId = "user_1",
                projectId = "proj_1",
                packType = PackType.BEHAVIOR_PACK.name,
                relativePath = "entities/fire_dragon.json",
                contentText = """{"format_version":"1.21.50","minecraft:entity":{"description":{"identifier":"dragon_world:fire_dragon"}}}"""
            ),
            ModFileEntry(
                id = "bp_sword",
                userId = "user_1",
                projectId = "proj_1",
                packType = PackType.BEHAVIOR_PACK.name,
                relativePath = "items/dragon_sword.json",
                contentText = """{"format_version":"1.21.50","minecraft:item":{"description":{"identifier":"dragon_world:dragon_sword"},"components":{"minecraft:damage":14}}}"""
            ),
            ModFileEntry(
                id = "bp_armor",
                userId = "user_1",
                projectId = "proj_1",
                packType = PackType.BEHAVIOR_PACK.name,
                relativePath = "items/dragon_chestplate.json",
                contentText = """{"format_version":"1.21.50","minecraft:item":{"description":{"identifier":"dragon_world:dragon_chestplate"},"components":{"minecraft:wearable":{}}}}"""
            ),
            ModFileEntry(
                id = "bp_block",
                userId = "user_1",
                projectId = "proj_1",
                packType = PackType.BEHAVIOR_PACK.name,
                relativePath = "blocks/dragon_egg_block.json",
                contentText = """{"format_version":"1.21.50","minecraft:block":{"description":{"identifier":"dragon_world:dragon_egg_block"}}}"""
            ),
            ModFileEntry(
                id = "bp_recipe",
                userId = "user_1",
                projectId = "proj_1",
                packType = PackType.BEHAVIOR_PACK.name,
                relativePath = "recipes/dragon_sword.json",
                contentText = """{"format_version":"1.20.10"}"""
            )
        )

        val counts = BedrockPackEngine.computeComponentCounts(files)
        assertEquals(1L, counts.mobsCount)
        assertEquals(1L, counts.weaponsCount)
        assertEquals(1L, counts.armorCount)
        assertEquals(1L, counts.blocksCount)
        assertEquals(1L, counts.recipesCount)

        // Package .mcaddon and verify ZIP entries
        val mcaddonOut = ByteArrayOutputStream()
        BedrockPackEngine.writeArchive(project, files, ExportFormat.MCADDON, mcaddonOut)
        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(mcaddonOut.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = zis.nextEntry
            }
        }
        assertTrue(entries.any { it.endsWith("_BP/manifest.json") })
        assertTrue(entries.any { it.endsWith("_RP/manifest.json") })
        assertTrue(entries.any { it.endsWith("_BP/entities/fire_dragon.json") })
        assertTrue(entries.any { it.endsWith("_BP/items/dragon_sword.json") })

        // Package .zip and verify README + BP/RP layout
        val zipOut = ByteArrayOutputStream()
        BedrockPackEngine.writeArchive(project, files, ExportFormat.ZIP, zipOut)
        val zipEntries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipOut.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                zipEntries.add(entry.name)
                entry = zis.nextEntry
            }
        }
        assertTrue(zipEntries.any { it.endsWith("/README.txt") })
        assertTrue(zipEntries.any { it.contains("/BP/") })
        assertTrue(zipEntries.any { it.contains("/RP/") })
    }
}
