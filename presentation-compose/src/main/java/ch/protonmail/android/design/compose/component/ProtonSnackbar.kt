/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package ch.protonmail.android.design.compose.component

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.theme.ProtonTheme
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
fun ProtonSnackbarHost(
    hostState: ProtonSnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { data ->
        ProtonSnackbar(snackbarData = data, hostState.type)
    }
) {
    SnackbarHost(
        hostState = hostState.snackbarHostState,
        modifier = modifier,
        snackbar = snackbar
    )
}

@Stable
class ProtonSnackbarHostState(
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
    defaultType: ProtonSnackbarType = ProtonSnackbarType.WARNING
) {

    private val mutex = Mutex()

    var type by mutableStateOf(defaultType)
        private set

    suspend fun showSnackbar(
        type: ProtonSnackbarType,
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        withDismissAction: Boolean = false
    ): SnackbarResult {
        dismissAll()
        return mutex.withLock {
            this.type = type
            snackbarHostState.showSnackbar(message, actionLabel, withDismissAction, duration)
        }
    }

    fun dismissAll() {
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}

@Composable
fun ProtonSnackbar(
    snackbarData: SnackbarData,
    type: ProtonSnackbarType,
    modifier: Modifier = Modifier,
    actionOnNewLine: Boolean = false,
    shape: Shape = ProtonTheme.shapes.medium,
    contentColor: Color = ProtonTheme.colors.textInverted,
    actionColor: Color = contentColor
) {
    Snackbar(
        snackbarData = snackbarData,
        modifier = modifier,
        actionOnNewLine = actionOnNewLine,
        shape = shape,
        containerColor = when (type) {
            ProtonSnackbarType.SUCCESS -> ProtonTheme.colors.notificationSuccess
            ProtonSnackbarType.WARNING -> ProtonTheme.colors.notificationWarning
            ProtonSnackbarType.ERROR -> ProtonTheme.colors.notificationError
            ProtonSnackbarType.NORM -> ProtonTheme.colors.notificationNorm
        },
        contentColor = contentColor,
        actionColor = actionColor
    )
}

private val previewSnackbarData = object : SnackbarData {
    override val visuals = object : SnackbarVisuals {
        override val actionLabel: String? = null
        override val duration: SnackbarDuration = SnackbarDuration.Indefinite
        override val message: String = "This is a snackbar"
        override val withDismissAction: Boolean = false
    }

    override fun dismiss() = Unit
    override fun performAction() = Unit
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewSuccessSnackbar() {
    ProtonTheme {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.SUCCESS)
    }
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewErrorSnackbar() {
    ProtonTheme {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.ERROR)
    }
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewWarningSnackbar() {
    ProtonTheme {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.WARNING)
    }
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewNormSnackbar() {
    ProtonTheme {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.NORM)
    }
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewSuccessSnackbarDark() {
    ProtonTheme(isDark = true) {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.SUCCESS)
    }
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewErrorSnackbarDark() {
    ProtonTheme(isDark = true) {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.ERROR)
    }
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewWarningSnackbarDark() {
    ProtonTheme(isDark = true) {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.WARNING)
    }
}

@Preview
@Composable
@Suppress("unused")
private fun PreviewNormSnackbarDark() {
    ProtonTheme(isDark = true) {
        ProtonSnackbar(snackbarData = previewSnackbarData, type = ProtonSnackbarType.NORM)
    }
}

enum class ProtonSnackbarType {
    SUCCESS, WARNING, ERROR, NORM
}
