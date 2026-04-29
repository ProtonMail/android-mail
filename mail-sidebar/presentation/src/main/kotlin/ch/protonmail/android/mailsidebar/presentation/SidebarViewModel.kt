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

package ch.protonmail.android.mailsidebar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveLoadedMailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.SelectMailLabelId
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabelExpandedState
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class SidebarViewModel @Inject constructor(
    val appInformation: AppInformation,
    private val updateLabelExpandedState: UpdateLabelExpandedState,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val observeUnreadCounters: ObserveUnreadCounters,
    private val observeLoadedMailLabelId: ObserveLoadedMailLabelId,
    private val selectMailLabelId: SelectMailLabelId
) : ViewModel() {

    private val initialState = State.Disabled

    private val primaryUser = observePrimaryUserId().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val state: StateFlow<State> = primaryUser.filterNotNull().flatMapLatest { userId ->
        combine(
            observeLoadedMailLabelId(),
            observeMailLabels(userId),
            observeUnreadCounters(userId)
        ) { loadedMailLabelId, mailLabels, counters ->
            State.Enabled(
                selectedMailLabelId = loadedMailLabelId,
                // Pending Account team to migrate "paymentManager" to rust
                // (current implementation isn't aware of the rust session and throws
                // exception crashing the app if no user is logged into "core"
                canChangeSubscription = false,
                mailLabels = mailLabels.toUiModels(counters.toMap(), loadedMailLabelId)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = initialState
    )

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                is Action.LabelAction -> onSidebarLabelAction(action.action)
            }.exhaustive
        }
    }

    private fun onSidebarLabelAction(action: SidebarLabelAction) {
        when (action) {
            is SidebarLabelAction.Add -> Unit
            is SidebarLabelAction.Collapse -> onUpdateLabelExpandedState(action.labelId, false)
            is SidebarLabelAction.Expand -> onUpdateLabelExpandedState(action.labelId, true)
            is SidebarLabelAction.Select -> selectMailLabelId(action.labelId)
        }
    }

    private fun onUpdateLabelExpandedState(labelId: MailLabelId, isExpanded: Boolean) = viewModelScope.launch {
        primaryUser.value?.let { userId ->
            updateLabelExpandedState(userId, labelId, isExpanded)
        }
    }

    private fun List<UnreadCounter>.toMap(): Map<LabelId, Int?> = run {
        associateBy({ it.labelId }, { it.count.takeIf { count -> count > 0 } })
    }

    sealed class State {
        data class Enabled(
            val selectedMailLabelId: MailLabelId,
            val canChangeSubscription: Boolean,
            val mailLabels: MailLabelsUiModel
        ) : State()

        object Disabled : State()
    }

    sealed interface Action {
        data class LabelAction(val action: SidebarLabelAction) : Action
    }
}
