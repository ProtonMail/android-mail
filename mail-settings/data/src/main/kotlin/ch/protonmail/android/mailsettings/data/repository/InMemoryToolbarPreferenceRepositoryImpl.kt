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

package ch.protonmail.android.mailsettings.data.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailsettings.domain.model.SettingsToolbarType
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsPreference
import ch.protonmail.android.mailsettings.domain.repository.InMemoryToolbarPreferenceRepository
import ch.protonmail.android.mailsettings.domain.repository.InMemoryToolbarPreferenceRepository.Error
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

@ViewModelScoped
class InMemoryToolbarPreferenceRepositoryImpl @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository,
    private val accountManager: AccountManager
) : InMemoryToolbarPreferenceRepository {

    private val proposedPreferences = MutableStateFlow<ToolbarActionsPreference?>(null)

    override fun inMemoryPreferences(): Flow<Either<Error.UserNotLoggedIn, ToolbarActionsPreference>> {
        return accountManager.getPrimaryUserId().flatMapLatest { userId ->
            userId?.let {
                mailSettingsRepository.getMailSettingsFlow(userId)
                    .mapSuccessValueOrNull()
                    .filterNotNull()
                    .map { settings ->
                        val isConversationMode = settings.viewMode?.enum?.let { it == ViewMode.ConversationGrouping }
                        ToolbarActionsPreference
                            .create(settings.mobileSettings, isConversationMode ?: false)
                    }
                    .distinctUntilChanged()
                    .flatMapLatest { preference ->
                        proposedPreferences.update { preference }
                        proposedPreferences.filterNotNull().map { it.right() }
                    }
            } ?: flowOf(Error.UserNotLoggedIn.left())
        }
    }

    override fun toggleSelection(
        actionId: String,
        tab: SettingsToolbarType,
        toggled: Boolean
    ) {
        proposedPreferences.update { pref ->
            pref?.update(tab) {
                it.toggleSelection(actionId, toggled)
            }
        }
    }

    override fun resetToDefault(tab: SettingsToolbarType) {
        proposedPreferences.update { pref ->
            pref?.update(tab) {
                it.resetToDefault()
            }
        }
    }

    override fun reorder(
        fromIndex: Int,
        toIndex: Int,
        tab: SettingsToolbarType
    ) {
        proposedPreferences.update { pref ->
            pref?.update(tab) {
                it.reorder(fromIndex = fromIndex, toIndex = toIndex)
            }
        }
    }
}
