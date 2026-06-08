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

package ch.protonmail.android.maillabel.presentation.bottomsheet.moveto

import java.util.UUID
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonHorizontallyCenteredProgress
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import me.proton.core.domain.entity.UserId

@Composable
fun MoveToBottomSheetScreen(
    providedData: MoveToBottomSheet.InitialData,
    actions: MoveToBottomSheet.Actions,
    modifier: Modifier = Modifier
) {

    val vmSessionKey = remember { UUID.randomUUID().toString() }
    val viewModel = hiltViewModel<MoveToViewModel, MoveToViewModel.Factory>(
        key = vmSessionKey
    ) { factory ->
        factory.create(providedData)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    val contentActions = MoveToBottomSheetContent.Actions(
        onFolderSelected = { id, text, _ ->
            viewModel.submit(MoveToOperation.MoveToAction.MoveToDestinationSelected(id, text))
        },
        onCategorySelected = { id, text, _ ->
            viewModel.submit(MoveToOperation.MoveToAction.MoveToDestinationSelected(id, text))
        },
        onCreateNewFolderClick = actions.onCreateNewFolderClick,
        onDismiss = actions.onDismiss,
        onError = actions.onError,
        onMoveToComplete = actions.onMoveToComplete
    )

    val currentState = state
    when (currentState) {
        is MoveToState.Data -> MoveToBottomSheetContent(
            dataState = currentState,
            actions = contentActions,
            modifier = modifier
        )

        is MoveToState.Loading -> Column {
            ProtonHorizontallyCenteredProgress(
                modifier = Modifier.padding(vertical = ProtonDimens.Spacing.ExtraLarge)
            )

            BottomNavigationBarSpacer()
        }

        is MoveToState.Error -> {
            actions.onError(stringResource(R.string.bottom_sheet_move_to_error_fetch))
            actions.onDismiss()
        }
    }
}

object MoveToBottomSheet {

    data class Actions(
        val onCreateNewFolderClick: () -> Unit,
        val onError: (String) -> Unit,
        val onDismiss: () -> Unit,
        val onMoveToComplete: (
            mailLabelText: MailLabelText,
            entryPoint: MoveToBottomSheetEntryPoint
        ) -> Unit
    )

    @Stable
    data class InitialData(
        val userId: UserId,
        val currentLocationLabelId: LabelId,
        val items: List<MoveToItemId>,
        val entryPoint: MoveToBottomSheetEntryPoint
    )
}
