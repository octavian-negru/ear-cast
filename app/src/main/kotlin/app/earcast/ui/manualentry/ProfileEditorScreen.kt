@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.manualentry

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.earcast.R
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionPage
import app.earcast.ui.common.CollapsibleNotice
import app.earcast.ui.common.ScreenHeader

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
    DisposableEffect(viewModel) {
        onDispose { viewModel.stopPreview() }
    }

    ActionPage(actions = {
        ActionButton(
            label = stringResource(R.string.manual_save),
            onClick = { viewModel.save(onSaved = onBack) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }) {
        ScreenHeader(
            title = stringResource(R.string.manual_title),
            subtitle = stringResource(R.string.identity_editor_detail),
            onBack = onBack,
        )
        CollapsibleNotice(
            title = stringResource(R.string.manual_notice_title),
            body = stringResource(R.string.manual_intro),
            modifier = Modifier.fillMaxWidth(),
        )

        ProfilePlotEditor(
            levels = levels,
            frequencies = viewModel.frequencies,
            previewState = previewState,
            onChange = viewModel::setLevel,
            onPreview = viewModel::previewTone,
            onStopPreview = viewModel::stopPreview,
        )
    }
}
