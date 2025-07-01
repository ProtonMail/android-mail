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

package ch.protonmail.android.mailupselling.presentation.ui.npsfeedback

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackViewEvent
import ch.protonmail.android.mailupselling.presentation.viewmodel.NPSFeedbackViewModel
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

@Composable
fun NPSFeedbackScreen(actions: NPSFeedbackScreen.Actions, modifier: Modifier = Modifier) {
    val viewmodel = hiltViewModel<NPSFeedbackViewModel>()
    val state = viewmodel.state.collectAsStateWithLifecycle().value

    LaunchedEffect(Unit) {
        viewmodel.submit(NPSFeedbackViewEvent.ContentShown)
    }

    val successMessage = stringResource(R.string.nps_success)
    ConsumableLaunchedEffect(state.showSuccess) {
        actions.onSubmitted(successMessage)
    }

    val scope = rememberCoroutineScope()
    val isSystemBackButtonClickEnabled = remember { mutableStateOf(true) }
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    BackHandler(isSystemBackButtonClickEnabled.value) {
        isSystemBackButtonClickEnabled.value = false
        viewmodel.submit(NPSFeedbackViewEvent.Dismissed)
        scope.launch {
            awaitFrame()
            onBackPressedDispatcher?.onBackPressed()
        }
    }

    NPSFeedbackContent(state, onCloseClick = {
        viewmodel.submit(NPSFeedbackViewEvent.Dismissed)
        actions.onDismiss()
    }, onEvent = {
        viewmodel.submit(it)
    }, modifier = modifier)
}

object NPSFeedbackScreen {

    data class Actions(
        val onSubmitted: (String) -> Unit,
        val onDismiss: () -> Unit
    )
}
