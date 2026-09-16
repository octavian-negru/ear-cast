@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.manualentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.ui.common.CollapsibleNotice

/**
 * Manual audiogram entry with plotted thresholds.
 * Intended for people who already have results from a professional
 * hearing test and want assist mode without running the on-device check.
 */
@Composable
fun ProfileEditorScreen(
    onBack: () -> Unit,
    viewModel: ProfileEditorModel = hiltViewModel(),
) {
    val levels by viewModel.levels.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
    ) {
        Text(stringResource(R.string.manual_title), style = MaterialTheme.typography.headlineSmall)
        CollapsibleNotice(
            title = stringResource(R.string.manual_notice_title),
            body = stringResource(R.string.manual_intro),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )

        ProfilePlotEditor(
            levels = levels,
            frequencies = viewModel.frequencies,
            previewState = previewState,
            onChange = viewModel::setLevel,
            onPreview = viewModel::previewTone,
            onStopPreview = viewModel::stopPreview,
        )

        Button(
            onClick = { viewModel.save(onSaved = onBack) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(top = 16.dp),
        ) { Text(stringResource(R.string.manual_save), style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.padding(4.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}
