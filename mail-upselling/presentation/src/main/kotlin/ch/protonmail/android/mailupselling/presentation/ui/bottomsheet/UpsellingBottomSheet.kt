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

package ch.protonmail.android.mailupselling.presentation.ui.bottomsheet

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingScreenContentState
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreenContent
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreenContentError
import ch.protonmail.android.mailupselling.presentation.viewmodel.UpsellingViewModel
import me.proton.core.compose.component.ProtonCenteredProgress

@Composable
fun UpsellingBottomSheet(
    modifier: Modifier = Modifier,
    bottomSheetActions: UpsellingScreen.Actions,
    upsellingEntryPoint: UpsellingEntryPoint.Feature
) {

    val viewModel = hiltViewModel<UpsellingViewModel, UpsellingViewModel.Factory>(
        key = upsellingEntryPoint.toString()
    ) { factory ->
        factory.create(upsellingEntryPoint)
    }

    val actions = bottomSheetActions.copy(
        onDisplayed = { viewModel.updateLastSeenTimestamp() },
        onUpgradeAttempt = { viewModel.trackUpgradeAttempt(it) },
        onUpgradeCancelled = { viewModel.trackUpgradeCancelled(it) },
        onUpgradeErrored = { viewModel.trackUpgradeErrored(it) },
        onSuccess = { viewModel.trackPurchaseCompleted(it) }
    )

    when (val state = viewModel.state.collectAsState().value) {
        UpsellingScreenContentState.Loading -> ProtonCenteredProgress(
            modifier = Modifier
                .fillMaxWidth()
                .height(MailDimens.ExtraLargeSpacing)
        )
        is UpsellingScreenContentState.Data -> UpsellingScreenContent(modifier, state, actions)
        is UpsellingScreenContentState.Error -> UpsellingScreenContentError(state = state, actions)
    }
}

object UpsellingBottomSheet {
    const val DELAY_SHOWING = 100L
}
