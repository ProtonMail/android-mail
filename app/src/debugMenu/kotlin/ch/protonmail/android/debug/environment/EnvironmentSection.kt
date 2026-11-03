/*
 * Copyright (c) 2026 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.debug.environment

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.R
import ch.protonmail.android.debug.DebugMenuChevron
import ch.protonmail.android.debug.DebugMenuRow
import ch.protonmail.android.debug.debugMenuSection
import ch.protonmail.android.design.compose.component.ProtonAlertDialog
import ch.protonmail.android.design.compose.component.ProtonAlertDialogButton
import ch.protonmail.android.design.compose.component.ProtonDialogTitle
import ch.protonmail.android.design.compose.component.protonOutlineTextFieldColors
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme

internal data class EnvironmentRowUiModel(
    val host: String,
    val isOverridden: Boolean,
    val isPendingRestart: Boolean
)

internal fun LazyListScope.environmentSection(row: EnvironmentRowUiModel, onClick: () -> Unit) {
    debugMenuSection(title = R.string.app_debug_screen_environment_section_title) {
        EnvironmentRow(row = row, onClick = onClick)
        WebAppRow(host = row.host)
    }
}

@Composable
private fun WebAppRow(host: String) {
    val uriHandler = LocalUriHandler.current
    val url = EnvironmentHost.accountLoginUrl(host)
    DebugMenuRow(
        icon = R.drawable.ic_proton_arrow_out_square,
        title = stringResource(R.string.app_debug_screen_environment_web_app),
        description = url.substringAfter("://"),
        onClick = { uriHandler.openUri(url) }
    )
}

@Composable
private fun EnvironmentRow(row: EnvironmentRowUiModel, onClick: () -> Unit) {
    DebugMenuRow(
        icon = R.drawable.ic_proton_globe,
        title = row.host,
        description = stringResource(
            when {
                row.isPendingRestart -> R.string.app_debug_screen_environment_pending_description
                row.isOverridden -> R.string.app_debug_screen_environment_custom_description
                else -> R.string.app_debug_screen_environment_default_description
            }
        ),
        // Only a change that still needs the restart stands out.
        tint = if (row.isPendingRestart) ProtonTheme.colors.notificationWarning else ProtonTheme.colors.iconWeak,
        trailing = { DebugMenuChevron() },
        onClick = onClick
    )
}

/**
 * Switches the proton.black environment. The tester types the full host or pastes a link; the dropdown only offers
 * the recently used hosts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EnvironmentDialog(
    currentHost: String,
    defaultHost: String,
    isOverridden: Boolean,
    recentHosts: List<String>,
    signsOut: Boolean,
    switchState: EnvironmentSwitchState,
    onSave: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    initialInput: String = currentHost,
    initiallyExpanded: Boolean = false
) {
    var input by rememberSaveable { mutableStateOf(initialInput) }
    var edited by rememberSaveable { mutableStateOf(initialInput != currentHost) }
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val isError = EnvironmentHost.parse(input, defaultHost) == EnvironmentHost.Input.Invalid
    val isSwitching = switchState == EnvironmentSwitchState.Switching
    val canApply = !isError && !isSwitching
    // Before the prefilled value is edited every recent host is offered, then those starting with the input.
    val query = if (edited) input.trim().lowercase() else ""
    val shownRecentHosts = recentHosts.filter { it.startsWith(query) }
    val hasRecentHosts = recentHosts.isNotEmpty()

    ProtonAlertDialog(
        onDismissRequest = onDismiss,
        title = { ProtonDialogTitle(titleResId = R.string.app_debug_screen_environment_dialog_title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)) {
                ExposedDropdownMenuBox(
                    expanded = expanded && shownRecentHosts.isNotEmpty(),
                    onExpandedChange = { expanded = it }
                ) {
                    EnvironmentField(
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        input = input,
                        isError = isError,
                        hasRecentHosts = hasRecentHosts,
                        expanded = expanded,
                        onInputChange = {
                            input = it
                            edited = true
                            expanded = true
                        },
                        onDone = { if (canApply) onSave(input) }
                    )
                    if (shownRecentHosts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            containerColor = ProtonTheme.colors.backgroundNorm
                        ) {
                            MenuHeader(text = stringResource(R.string.app_debug_screen_environment_recent))
                            shownRecentHosts.forEach { host ->
                                HostMenuItem(
                                    host = host,
                                    onPick = {
                                        input = host
                                        edited = true
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (signsOut) {
                    Text(
                        text = stringResource(R.string.app_debug_screen_environment_signs_out),
                        style = ProtonTheme.typography.bodyMedium,
                        color = ProtonTheme.colors.textWeak
                    )
                }
                if (switchState == EnvironmentSwitchState.Failed) {
                    Text(
                        text = stringResource(R.string.app_debug_screen_environment_sign_out_failed),
                        style = ProtonTheme.typography.bodyMedium,
                        color = ProtonTheme.colors.notificationError
                    )
                }
            }
        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (isOverridden) {
                    ProtonAlertDialogButton(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        titleResId = R.string.app_debug_screen_environment_reset,
                        contained = false,
                        enabled = !isSwitching,
                        onClick = onReset
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                ProtonAlertDialogButton(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    titleResId = R.string.app_debug_screen_environment_cancel,
                    contained = false,
                    onClick = onDismiss
                )
                ProtonAlertDialogButton(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    titleResId = R.string.app_debug_screen_environment_apply,
                    contained = false,
                    enabled = canApply,
                    textColor = if (!canApply) {
                        ProtonTheme.colors.textDisabled
                    } else {
                        ProtonTheme.colors.interactionBrandDefaultNorm
                    },
                    onClick = { onSave(input) }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvironmentField(
    input: String,
    isError: Boolean,
    hasRecentHosts: Boolean,
    expanded: Boolean,
    onInputChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = input,
        onValueChange = onInputChange,
        label = { Text(stringResource(R.string.app_debug_screen_environment_field_label)) },
        singleLine = true,
        isError = isError,
        supportingText = {
            Text(
                text = stringResource(
                    if (isError) R.string.app_debug_screen_environment_error else R.string.app_debug_screen_environment_helper
                ),
                style = ProtonTheme.typography.bodySmall,
                color = if (isError) ProtonTheme.colors.notificationError else ProtonTheme.colors.textWeak
            )
        },
        trailingIcon = {
            when {
                isError -> Icon(
                    painter = painterResource(R.drawable.ic_proton_exclamation_circle_filled),
                    contentDescription = null,
                    tint = ProtonTheme.colors.notificationError
                )

                hasRecentHosts -> ExposedDropdownMenuDefaults.TrailingIcon(expanded)

                else -> Unit
            }
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = ProtonTheme.shapes.medium,
        colors = protonOutlineTextFieldColors()
    )
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Large, vertical = ProtonDimens.Spacing.Standard),
        text = text,
        style = ProtonTheme.typography.labelMedium,
        color = ProtonTheme.colors.textHint
    )
}

@Composable
private fun HostMenuItem(host: String, onPick: (String) -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = host,
                style = ProtonTheme.typography.bodyLarge,
                color = ProtonTheme.colors.textNorm
            )
        },
        onClick = { onPick(host) }
    )
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class EnvironmentPreviews

@EnvironmentPreviews
@Composable
private fun EnvironmentRowDefaultPreview() {
    ProtonTheme {
        EnvironmentRow(
            row = EnvironmentRowUiModel(host = "proton.black", isOverridden = false, isPendingRestart = false),
            onClick = {}
        )
    }
}

@EnvironmentPreviews
@Composable
private fun EnvironmentRowOverridePreview() {
    ProtonTheme {
        EnvironmentRow(
            row = EnvironmentRowUiModel(host = "maury.proton.black", isOverridden = true, isPendingRestart = true),
            onClick = {}
        )
    }
}

@EnvironmentPreviews
@Composable
private fun EnvironmentDialogDefaultPreview() {
    ProtonTheme {
        EnvironmentDialog(
            currentHost = "proton.black",
            defaultHost = "proton.black",
            isOverridden = false,
            recentHosts = emptyList(),
            signsOut = true,
            switchState = EnvironmentSwitchState.Idle,
            onSave = {},
            onReset = {},
            onDismiss = {}
        )
    }
}

@EnvironmentPreviews
@Composable
private fun EnvironmentDialogOverridePreview() {
    ProtonTheme {
        EnvironmentDialog(
            currentHost = "maury.proton.black",
            defaultHost = "proton.black",
            isOverridden = true,
            recentHosts = listOf("maury.proton.black"),
            signsOut = true,
            switchState = EnvironmentSwitchState.Idle,
            onSave = {},
            onReset = {},
            onDismiss = {}
        )
    }
}

@EnvironmentPreviews
@Composable
private fun EnvironmentDialogErrorPreview() {
    ProtonTheme {
        EnvironmentDialog(
            currentHost = "proton.black",
            defaultHost = "proton.black",
            isOverridden = false,
            recentHosts = emptyList(),
            signsOut = false,
            switchState = EnvironmentSwitchState.Idle,
            onSave = {},
            onReset = {},
            onDismiss = {},
            initialInput = "staging.protonmail.dev"
        )
    }
}

@EnvironmentPreviews
@Composable
private fun EnvironmentDialogListOpenPreview() {
    ProtonTheme {
        EnvironmentDialog(
            currentHost = "proton.black",
            defaultHost = "proton.black",
            isOverridden = false,
            recentHosts = listOf("maury.proton.black", "mendel.proton.black", "ali.proton.black"),
            signsOut = true,
            switchState = EnvironmentSwitchState.Idle,
            onSave = {},
            onReset = {},
            onDismiss = {},
            initialInput = "m",
            initiallyExpanded = true
        )
    }
}
