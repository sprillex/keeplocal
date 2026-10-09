package com.randolph.keeplocal.data.nas

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupArchiverTest {

    private lateinit var context: Context
    private lateinit var backupArchiver: BackupArchiver

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        backupArchiver = BackupArchiver()
    }

    @Test
    fun testJsonSerializationAndDeserialization() {
        val notes = listOf(
            NoteEntity(
                id = 1,
                title = "Backup Test Note",
                content = "Testing local archive serialization.",
                noteType = NoteType.TEXT,
                isPinned = true,
                imageUris = listOf("content://media/external/images/media/301")
            ),
            NoteEntity(
                id = 2,
                title = "Shopping",
                content = "Buy milk",
                noteType = NoteType.CHECKLIST
            )
        )

        val json = backupArchiver.serializeNotesToJson(notes)
        assertTrue(json.contains("Backup Test Note"))

        val restored = backupArchiver.deserializeNotesFromJson(json)
        assertEquals(2, restored.size)
        assertEquals(1L, restored[0].id)
        assertEquals("Backup Test Note", restored[0].title)
        assertTrue(restored[0].isPinned)
        assertEquals(1, restored[0].imageUris.size)
        assertEquals("content://media/external/images/media/301", restored[0].imageUris[0])
        assertEquals(NoteType.CHECKLIST, restored[1].noteType)
    }

    @Test
    fun testArchiveCompressionAndUnpack() {
        val archiveFile = File(context.cacheDir, "test_backup.tar.gz")
        val notes = listOf(
            NoteEntity(
                id = 10,
                title = "GZIP Test",
                content = "Compressed backup contents",
                noteType = NoteType.TEXT
            )
        )

        val manifest = backupArchiver.createBackupArchive(notes, archiveFile)
        assertTrue(archiveFile.exists())
        assertTrue(archiveFile.length() > 0)
        assertEquals(1, manifest.noteCount)

        val (restoredManifest, restoredNotes) = backupArchiver.unpackBackupArchive(archiveFile)
        assertEquals(1, restoredManifest.noteCount)
        assertEquals(manifest.sha256, restoredManifest.sha256)
        assertEquals(1, restoredNotes.size)
        assertEquals("GZIP Test", restoredNotes[0].title)

        archiveFile.delete()
    }
}
