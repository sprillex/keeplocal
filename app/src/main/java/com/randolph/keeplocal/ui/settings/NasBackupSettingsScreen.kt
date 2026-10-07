package com.randolph.keeplocal.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.randolph.keeplocal.data.nas.NasConfig
import com.randolph.keeplocal.data.nas.NasCredentialManager
import com.randolph.keeplocal.data.nas.SmbBackupManager
import com.randolph.keeplocal.data.nas.SyncResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NasBackupSettingsScreen(
    nasCredentialManager: NasCredentialManager,
    smbBackupManager: SmbBackupManager,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(nasCredentialManager.getNasConfig()) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local NAS Backup (SMB 2/3)") },
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
                            imageVector = Icons.Filled.Storage,
                            contentDescription = "NAS Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LAN NAS Share Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = config.hostIp,
                        onValueChange = { config = config.copy(hostIp = it) },
                        label = { Text("NAS Host / IP Address (e.g. 192.168.1.100)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = config.shareName,
                        onValueChange = { config = config.copy(shareName = it) },
                        label = { Text("SMB Share Name (e.g. Backups)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = config.subfolderPath,
                        onValueChange = { config = config.copy(subfolderPath = it) },
                        label = { Text("Subfolder Path (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = config.username,
                        onValueChange = { config = config.copy(username = it) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = config.password,
                        onValueChange = { config = config.copy(password = it) },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isProcessing = true
                            syncStatusMessage = "Testing SMB NAS connection..."
                            scope.launch {
                                val testResult = smbBackupManager.testConnection(config)
                                isProcessing = false
                                when (testResult) {
                                    is SyncResult.Success -> {
                                        nasCredentialManager.saveNasConfig(config)
                                        isError = false
                                        syncStatusMessage = "Credentials saved & SMB NAS connection verified successfully!"
                                    }
                                    is SyncResult.Error -> {
                                        isError = true
                                        syncStatusMessage = "Connection Test Failed: ${testResult.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing && config.isValid
                    ) {
                        Text("Save & Test Credentials")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (syncStatusMessage != null) {
                Text(
                    text = syncStatusMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        isProcessing = true
                        syncStatusMessage = "Uploading backup to NAS..."
                        scope.launch {
                            val result = smbBackupManager.performBackup(context.cacheDir)
                            isProcessing = false
                            when (result) {
                                is SyncResult.Success -> {
                                    isError = false
                                    syncStatusMessage = "Backup completed successfully!"
                                }
                                is SyncResult.Error -> {
                                    isError = true
                                    syncStatusMessage = "Backup Failed: ${result.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing && config.isValid
                ) {
                    Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "Backup")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backup Now")
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = {
                        isProcessing = true
                        syncStatusMessage = "Restoring notes from NAS..."
                        scope.launch {
                            val result = smbBackupManager.restoreBackup(context.cacheDir)
                            isProcessing = false
                            when (result) {
                                is SyncResult.Success -> {
                                    isError = false
                                    syncStatusMessage = "Restore completed! Vector index re-embedding triggered."
                                }
                                is SyncResult.Error -> {
                                    isError = true
                                    syncStatusMessage = "Restore Failed: ${result.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing && config.isValid
                ) {
                    Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = "Restore")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
            }
        }
    }
}
