package br.com.tec.tecmotors.presentation.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.com.tec.tecmotors.R

@Composable
fun AccountSyncDialog(
    state: AccountSyncUiState,
    onDismiss: () -> Unit,
    onLoginGoogle: () -> Unit,
    onEvent: (AccountSyncUiEvent) -> Unit,
    onBuildBackupJson: suspend () -> Result<String>,
    onImportBackupJson: suspend (String) -> Result<Unit>,
    runBusyAction: (suspend () -> Unit) -> Unit
) {
    val context = LocalContext.current

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            onEvent(AccountSyncUiEvent.SetStatusMessage(context.getString(R.string.feedback_export_cancelled)))
            return@rememberLauncherForActivityResult
        }

        runBusyAction {
            onBuildBackupJson()
                .onSuccess { raw ->
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(raw.toByteArray(Charsets.UTF_8))
                        } ?: error("Falha ao abrir destino")
                    }.onSuccess {
                        onEvent(AccountSyncUiEvent.SetStatusMessage(context.getString(R.string.feedback_backup_export_success)))
                    }.onFailure {
                        onEvent(
                            AccountSyncUiEvent.SetStatusMessage(
                                context.getString(R.string.feedback_backup_export_failed, it.message.orEmpty())
                            )
                        )
                    }
                }
                .onFailure {
                    onEvent(
                        AccountSyncUiEvent.SetStatusMessage(
                            context.getString(R.string.feedback_backup_export_failed, it.message.orEmpty())
                        )
                    )
                }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            onEvent(AccountSyncUiEvent.SetStatusMessage(context.getString(R.string.feedback_export_cancelled)))
            return@rememberLauncherForActivityResult
        }

        runBusyAction {
            val json = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Arquivo de backup vazio")
            }

            json.onSuccess { raw ->
                onImportBackupJson(raw)
                    .onSuccess {
                        onEvent(AccountSyncUiEvent.SetStatusMessage(context.getString(R.string.feedback_backup_import_success)))
                    }
                    .onFailure {
                        onEvent(
                            AccountSyncUiEvent.SetStatusMessage(
                                context.getString(R.string.feedback_backup_import_failed, it.message.orEmpty())
                            )
                        )
                    }
            }.onFailure {
                onEvent(
                    AccountSyncUiEvent.SetStatusMessage(
                        context.getString(R.string.feedback_backup_import_failed, it.message.orEmpty())
                    )
                )
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.title_account_sync), style = MaterialTheme.typography.titleLarge)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.userEmail == null) {
                                Text(stringResource(R.string.text_not_logged_in))
                                Button(
                                    onClick = onLoginGoogle,
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_login_google))
                                }
                            } else {
                                Text(stringResource(R.string.text_logged_as))
                                Text(state.userEmail, fontWeight = FontWeight.Bold)

                                OutlinedButton(
                                    onClick = { onEvent(AccountSyncUiEvent.SignOut) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_logout))
                                }

                                Button(
                                    onClick = { onEvent(AccountSyncUiEvent.SyncNow) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_sync_now))
                                }

                                OutlinedButton(
                                    onClick = { onEvent(AccountSyncUiEvent.Upload) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_upload_cloud))
                                }

                                OutlinedButton(
                                    onClick = { onEvent(AccountSyncUiEvent.Download) },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.action_download_cloud))
                                }
                            }

                            HorizontalSectionTitle(title = stringResource(R.string.title_local_backup))

                            OutlinedButton(
                                onClick = { exportBackupLauncher.launch("tec-motors-backup.json") },
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.action_export_backup_json))
                            }

                            OutlinedButton(
                                onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/*")) },
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.action_import_backup_json))
                            }

                            if (state.busy) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.height(18.dp))
                                    Text(stringResource(R.string.text_processing))
                                }
                            }

                            state.statusMessage?.let {
                                Text(it, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Text(
                        stringResource(R.string.text_sync_conflict_hint),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}
