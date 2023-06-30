/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.ui.CommonTestTags
import ch.protonmail.android.mailcomposer.presentation.model.ComposerDraftState
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType

@Composable
internal fun NotSubmittableComposerForm(
    emailValidator: (String) -> Boolean,
    recipientsOpen: Boolean,
    initialFocus: FocusedFieldType,
    state: ComposerDraftState.NotSubmittable,
    actions: ComposerFormActions
) {
    val snackbarHostState = remember { ProtonSnackbarHostState() }

    ComposerForm(
        emailValidator = emailValidator,
        recipientsOpen = recipientsOpen,
        initialFocus = initialFocus,
        fields = state.fields,
        actions = actions
    )

    ProtonSnackbarHost(modifier = Modifier.testTag(CommonTestTags.SnackbarHost), hostState = snackbarHostState)

    ConsumableTextEffect(effect = state.error) { message ->
        snackbarHostState.showSnackbar(
            type = ProtonSnackbarType.ERROR,
            message = message
        )
    }
}
