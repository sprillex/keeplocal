package com.randolph.keeplocal.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.randolph.keeplocal.data.model.DownloadState
import com.randolph.keeplocal.data.model.ModelManager
import com.randolph.keeplocal.data.nas.NasConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen(
    modelManager: ModelManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val downloadState by modelManager.downloadState.collectAsState()
    val scope = rememberCoroutineScope()

    val modelFile = modelManager.getModelFile()
    val tokenizerFile = modelManager.getTokenizeFile()
    val isReady = modelManager.isModelAndTokenizerReady()

    var nasConfig by remember { mutableStateOf(modelManager.nasCredentialManager.getNasConfig()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    modelManager.importModelFromInputStream(inputStream)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model & AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = "Model Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EmbeddingGemma 300M (LiteRT)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Status: ${if (isReady) "Ready / Installed" else "Not Downloaded / Incomplete"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Model: ${modelFile.name} (${if (modelFile.exists()) modelFile.length() / (1024 * 1024) else 0} MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tokenizer: ${tokenizerFile.name} (${if (tokenizerFile.exists()) tokenizerFile.length() / (1024 * 1024) else 0} MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Destination: ${modelFile.parent}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "NAS Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SMB3 NAS Model Source Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nasConfig.hostIp,
                        onValueChange = { nasConfig = nasConfig.copy(hostIp = it) },
                        label = { Text("NAS IP / Hostname") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nasConfig.shareName,
                        onValueChange = { nasConfig = nasConfig.copy(shareName = it) },
                        label = { Text("Share Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nasConfig.subfolderPath,
                        onValueChange = { nasConfig = nasConfig.copy(subfolderPath = it) },
                        label = { Text("Subfolder Path (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nasConfig.username,
                        onValueChange = { nasConfig = nasConfig.copy(username = it) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nasConfig.password,
                        onValueChange = { nasConfig = nasConfig.copy(password = it) },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            modelManager.nasCredentialManager.saveNasConfig(nasConfig)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Credentials")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = downloadState) {
                is DownloadState.Downloading -> {
                    Text(
                        text = "Downloading model & tokenizer via SMB3...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.totalBytes > 0) {
                        val progress = state.bytesDownloaded.toFloat() / state.totalBytes.toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                is DownloadState.Verifying -> {
                    Text(
                        text = "Verifying file size and SHA-256 checksums...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is DownloadState.Success -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Model and tokenizer ready and verified!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                is DownloadState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                DownloadState.Idle -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        modelManager.nasCredentialManager.saveNasConfig(nasConfig)
                        scope.launch {
                            modelManager.downloadAndVerifyFromSmbNas(nasConfig)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = downloadState !is DownloadState.Downloading && downloadState !is DownloadState.Verifying
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = "Download")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isReady) "Re-download Weights" else "Download Model Weights")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    enabled = downloadState !is DownloadState.Downloading && downloadState !is DownloadState.Verifying
                ) {
                    Icon(imageVector = Icons.Filled.FolderOpen, contentDescription = "Import File")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import File")
                }
            }
        }
    }
}
