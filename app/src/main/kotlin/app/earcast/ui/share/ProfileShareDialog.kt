@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.share

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.earcast.R
import app.earcast.audiogram.HearingCurve
import app.earcast.ui.common.StudioButton
import app.earcast.ui.common.StudioButtonStyle
import app.earcast.ui.common.StudioPanel
import app.earcast.ui.common.StudioSplitRow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Preview-then-share dialog: the user sees the exact image before anything
 * leaves the app (it is their hearing data). The visible [ProfileShareCard] is
 * recorded into a graphics layer and exported as a PNG on demand.
 */
@Composable
fun ProfileShareDialog(
    audiogram: HearingCurve,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val dateText =
        remember {
            LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        }
    var sharing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        StudioPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    stringResource(R.string.share_preview_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    ProfileShareCard(
                        audiogram = audiogram,
                        dateText = dateText,
                        modifier =
                            Modifier
                                .padding(top = 12.dp)
                                .drawWithContent {
                                    layer.record { this@drawWithContent.drawContent() }
                                    drawLayer(layer)
                                },
                    )
                }
                Text(
                    stringResource(R.string.share_preview_note),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                StudioSplitRow(modifier = Modifier.padding(top = 16.dp)) {
                    StudioButton(
                        label = stringResource(R.string.share_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        style = StudioButtonStyle.SECONDARY,
                    )
                    StudioButton(
                        label = stringResource(R.string.share_send),
                        enabled = !sharing,
                        onClick = {
                            sharing = true
                            scope.launch {
                                ProfileImageExporter.share(
                                    context,
                                    layer.toImageBitmap().asAndroidBitmap(),
                                )
                                sharing = false
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
