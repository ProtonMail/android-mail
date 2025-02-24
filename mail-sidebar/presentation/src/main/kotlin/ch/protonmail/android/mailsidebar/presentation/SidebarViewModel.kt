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
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.UpdateLabelExpandedState
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsidebar.presentation.usecase.ObserveSidebarUpsellingVisibility
import ch.protonmail.android.mailsidebar.presentation.usecase.TrackSidebarUpsellingClick
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.payment.domain.PaymentManager
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class SidebarViewModel @Inject constructor(
    val appInformation: AppInformation,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val updateLabelExpandedState: UpdateLabelExpandedState,
    private val paymentManager: PaymentManager,
    observePrimaryUser: ObservePrimaryUser,
    observeFolderColors: ObserveFolderColorSettings,
    observeMailLabels: ObserveMailLabels,
    observeUnreadCounters: ObserveUnreadCounters,
    private val observeSidebarUpsellingVisibility: ObserveSidebarUpsellingVisibility,
    private val trackUpsellingClick: TrackSidebarUpsellingClick
) : ViewModel() {

    val initialState = State.Disabled

    private val primaryUser = observePrimaryUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val state: Flow<State> = primaryUser.flatMapLatest { user ->
        if (user == null) {
            return@flatMapLatest flowOf(State.Disabled)
        }

        combine(
            selectedMailLabelId.flow,
            observeFolderColors(user.userId),
            observeMailLabels(user.userId, true),
            observeUnreadCounters(user.userId),
            observeSidebarUpsellingVisibility()
        ) { selectedMailLabelId, folderColors, mailLabels, counters, showUpsell ->
            State.Enabled(
                selectedMailLabelId = selectedMailLabelId,
                canChangeSubscription = paymentManager.isSubscriptionAvailable(user.userId),
                mailLabels = mailLabels.toUiModels(folderColors, counters.toMap(), selectedMailLabelId),
                showUpsell = showUpsell
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
                Action.UpsellClicked -> trackUpsellingClick()
            }.exhaustive
        }
    }

    private fun onSidebarLabelAction(action: SidebarLabelAction) {
        when (action) {
            is SidebarLabelAction.ViewList -> Unit
            is SidebarLabelAction.Add -> Unit
            is SidebarLabelAction.Collapse -> onUpdateLabelExpandedState(action.labelId, false)
            is SidebarLabelAction.Expand -> onUpdateLabelExpandedState(action.labelId, true)
            is SidebarLabelAction.Select -> selectedMailLabelId.set(action.labelId)
        }
    }

    private fun onUpdateLabelExpandedState(labelId: MailLabelId, isExpanded: Boolean) = viewModelScope.launch {
        primaryUser.value?.let {
            updateLabelExpandedState(it.userId, labelId, isExpanded)
        }
    }

    private fun List<UnreadCounter>.toMap(): Map<LabelId, Int?> = run {
        associateBy({ it.labelId }, { it.count.takeIf { count -> count > 0 } })
    }

    sealed class State {
        data class Enabled(
            val selectedMailLabelId: MailLabelId,
            val canChangeSubscription: Boolean,
            val mailLabels: MailLabelsUiModel,
            val showUpsell: Boolean
        ) : State()

        object Disabled : State()
    }

    sealed interface Action {
        data class LabelAction(val action: SidebarLabelAction) : Action
        data object UpsellClicked : Action
    }
}
