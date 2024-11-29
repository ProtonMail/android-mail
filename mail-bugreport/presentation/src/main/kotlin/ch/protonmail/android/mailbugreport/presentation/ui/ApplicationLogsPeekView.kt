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

package ch.protonmail.android.mailbugreport.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsPeekViewOperation
import ch.protonmail.android.mailbugreport.presentation.model.ApplicationLogsPeekViewState
import ch.protonmail.android.mailbugreport.presentation.viewmodel.ApplicationLogsPeekViewViewModel
import me.proton.core.compose.component.ProtonCenteredProgress

@Composable
fun ApplicationLogsPeekView(onBack: () -> Unit) {

    val viewModel = hiltViewModel<ApplicationLogsPeekViewViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state) {
        is ApplicationLogsPeekViewState.Loaded -> ApplicationLogsPeekViewContent(
            state as ApplicationLogsPeekViewState.Loaded,
            onBack
        )

        ApplicationLogsPeekViewState.Loading -> ProtonCenteredProgress()
        ApplicationLogsPeekViewState.Error -> ApplicationLogsPeekViewError(onBack)
    }

    LaunchedEffect(Unit) {
        viewModel.submit(ApplicationLogsPeekViewOperation.ViewAction.DisplayFileContent)
    }
}

object ApplicationLogsPeekView {

    const val ApplicationLogsViewMode = "application_logs_view_mode"
}
