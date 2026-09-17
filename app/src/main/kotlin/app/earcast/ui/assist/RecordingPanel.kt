@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.assist

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.ui.common.StudioButton
import app.earcast.ui.common.StudioButtonStyle
import app.earcast.ui.common.StudioPanel
import app.earcast.ui.common.StudioSplitRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
internal fun RecordingPanel(
    viewModel: ListenStateModel,
    active: Boolean,
) {
    val state by viewModel.diagnostics.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    StudioPanel(Modifier.fillMaxWidth()) {
        Text("Sound comparison recording", style = MaterialTheme.typography.titleSmall)
        Text(
            "Record up to 30 seconds of microphone and processed sound on your next start. " +
                "Recordings stay on this device until you choose Export.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = state.armed,
                onCheckedChange = viewModel::armDiagnostics,
                enabled = !active && !state.saving && !exporting,
            )
            Text("Record next session", style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::setDiagnosticNotes,
            label = { Text("Notes: distance, room, headset firmware") },
            enabled = !active && !state.saving,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(state.message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        StudioSplitRow(modifier = Modifier.padding(top = 8.dp)) {
            StudioButton(
                label = "Export latest",
                enabled = !active && !state.saving && !exporting && state.lastDirectory != null,
                modifier = Modifier.weight(1f),
                style = StudioButtonStyle.SECONDARY,
                onClick = export@{
                    val directory = state.lastDirectory ?: return@export
                    exporting = true
                    error = null
                    scope.launch {
                        error = shareDiagnostic(context, directory)
                        exporting = false
                    }
                },
            )
            StudioButton(
                label = "Delete recordings",
                enabled = !active && !state.saving && !exporting,
                modifier = Modifier.weight(1f),
                style = StudioButtonStyle.DANGER,
                onClick = viewModel::deleteDiagnostics,
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private suspend fun shareDiagnostic(
    context: Context,
    directory: File,
): String? {
    val result =
        runCatching {
            val uri =
                withContext(Dispatchers.IO) {
                    val shared = File(context.cacheDir, "shared").apply { mkdirs() }
                    val archive = File(shared, "sound-${directory.name}.zip")
                    if (!archive.isFile) {
                        val temporary = File.createTempFile("sound-", ".tmp", shared)
                        try {
                            ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                                for (name in listOf("stages.wav", "blocks.csv", "metadata.json")) {
                                    val file = File(directory, name)
                                    check(file.isFile) { "Recording is incomplete: $name" }
                                    zip.putNextEntry(ZipEntry(name))
                                    file.inputStream().use { it.copyTo(zip) }
                                    zip.closeEntry()
                                }
                            }
                            check(temporary.renameTo(archive)) { "Could not finalize the recording export" }
                        } finally {
                            temporary.delete()
                        }
                    }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archive)
                }
            val send =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("Sound comparison recording", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(send, "Export sound comparison recording"))
        }
    val failure = result.exceptionOrNull()
    if (failure is CancellationException) {
        throw failure
    }
    return failure?.message ?: if (failure == null) null else "Export failed"
}
