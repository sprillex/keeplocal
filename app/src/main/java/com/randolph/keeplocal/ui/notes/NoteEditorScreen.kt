package com.randolph.keeplocal.ui.notes

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.randolph.keeplocal.data.local.entity.NoteEntity
import com.randolph.keeplocal.data.local.entity.NoteType
import com.randolph.keeplocal.data.repository.SearchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val PASTEL_NOTE_COLORS = listOf(
    Color.Transparent,          // Default Surface
    Color(0xFF2D221E),          // Dark Muted Rose/Pastel Red
    Color(0xFF2C2A1E),          // Dark Muted Amber/Yellow
    Color(0xFF1E2B2A),          // Dark Muted Teal/Cyan
    Color(0xFF1F2922),          // Dark Muted Green
    Color(0xFF212532),          // Dark Muted Blue/Slate
    Color(0xFF282030)           // Dark Muted Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity?,
    relatedContextMatches: List<SearchResult>,
    onBackAndSave: (id: Long, title: String, content: String, noteType: NoteType, isPinned: Boolean, colorHex: String?) -> Unit,
    onDelete: (NoteEntity) -> Unit,
    onArchive: (NoteEntity) -> Unit,
    onQueryRelatedContext: (title: String, content: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }
    var isPinned by remember(note) { mutableStateOf(note?.isPinned ?: false) }
    var selectedNoteType by remember(note) { mutableStateOf(note?.noteType ?: NoteType.TEXT) }
    var selectedColor by remember(note) {
        mutableStateOf(
            if (note?.colorHex != null) {
                try { Color(android.graphics.Color.parseColor(note.colorHex)) } catch (e: Exception) { Color.Transparent }
            } else Color.Transparent
        )
    }

    var showMenu by remember { mutableStateOf(false) }
    var showColorPalette by remember { mutableStateOf(false) }
    var isDrawerExpanded by remember { mutableStateOf(false) }

    val formattedTime = remember(note) {
        val date = Date(note?.updatedAt ?: System.currentTimeMillis())
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }

    // Auto-query related context on draft changes
    LaunchedEffect(title, content) {
        if (title.isNotBlank() || content.isNotBlank()) {
            onQueryRelatedContext(title, content)
        }
    }

    fun saveAndExit() {
        val finalColorHex = if (selectedColor == Color.Transparent) null else String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
        onBackAndSave(note?.id ?: 0, title, content, selectedNoteType, isPinned, finalColorHex)
    }

    // Handle system back gesture
    BackHandler {
        saveAndExit()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back and Save",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin Note" else "Pin Note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Reminder set for 9:00 AM tomorrow", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationAdd,
                            contentDescription = "Add Reminder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                if (note != null) onDelete(note)
                                onBackAndSave(0, "", "", NoteType.TEXT, false, null)
                            },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = {
                                showMenu = false
                                if (note != null) onArchive(note)
                                onBackAndSave(0, "", "", NoteType.TEXT, false, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                showMenu = false
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Note"))
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (selectedColor == Color.Transparent) MaterialTheme.colorScheme.background else selectedColor
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selectedColor == Color.Transparent) MaterialTheme.colorScheme.surface else selectedColor)
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Collapsible Related Local Notes Drawer
                if (relatedContextMatches.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDrawerExpanded = !isDrawerExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "Related Context",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "▲ Related Local Notes (${relatedContextMatches.size} results match)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isDrawerExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Toggle Drawer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(
                            visible = isDrawerExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                for (match in relatedContextMatches) {
                                    Card(
                                        onClick = {
                                            title = match.note.title
                                            content = match.note.content
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = match.note.title.ifBlank { "Untitled Note" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = match.note.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Pastel Color Palette popup selector
                if (showColorPalette) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (color in PASTEL_NOTE_COLORS) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (color == Color.Transparent) MaterialTheme.colorScheme.surface else color,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (selectedColor == color) 2.dp else 1.dp,
                                        color = if (selectedColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColor = color
                                        showColorPalette = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColor == color) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            Toast.makeText(context, "Attachment picker (Images & Media)", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.AddBox, contentDescription = "Add Attachment", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(onClick = {
                            if (selectedNoteType == NoteType.CHECKLIST) {
                                selectedNoteType = NoteType.TEXT
                            } else {
                                selectedNoteType = NoteType.CHECKLIST
                                if (!content.contains("[ ]") && !content.contains("[x]")) {
                                    val lines = content.lines().filter { it.isNotBlank() }
                                    if (lines.isNotEmpty()) {
                                        content = lines.joinToString("\n") { "[ ] $it" }
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedNoteType == NoteType.CHECKLIST) Icons.Filled.Checklist else Icons.Outlined.Checklist,
                                contentDescription = "Checklist Toggle",
                                tint = if (selectedNoteType == NoteType.CHECKLIST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showColorPalette = !showColorPalette }) {
                            Icon(Icons.Outlined.Palette, contentDescription = "Color Palette", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        text = "Edited $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = if (selectedColor == Color.Transparent) MaterialTheme.colorScheme.background else selectedColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Borderless Title Input (MaterialTheme.typography.titleLarge)
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Borderless Body Input (MaterialTheme.typography.bodyLarge)
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            text = "Note",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}
