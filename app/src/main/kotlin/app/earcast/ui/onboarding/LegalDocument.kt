@file:Suppress("ktlint:standard:function-naming")

package app.earcast.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.earcast.R
import app.earcast.ui.common.ActionButton
import app.earcast.ui.common.ActionPage
import app.earcast.ui.common.ScreenHeader

/** A readable document with a close action that stays reachable at any text size. */
@Composable
internal fun LegalDocument(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val height =
        LocalConfiguration.current.screenHeightDp.dp -
            insets.calculateTopPadding() - insets.calculateBottomPadding()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxWidth().height(height), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                ActionPage(Modifier.widthIn(max = 720.dp), actions = {
                    ActionButton(stringResource(R.string.terms_close), onDismiss, Modifier.fillMaxWidth())
                }) {
                    ScreenHeader(title, onBack = onDismiss)
                    content()
                }
            }
        }
    }
}
