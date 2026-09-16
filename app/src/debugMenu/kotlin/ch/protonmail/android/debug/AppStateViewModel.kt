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

package ch.protonmail.android.debug

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailcontentsearch.data.repository.ContentSearchPromptRepositoryImpl
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailnotifications.data.local.NotificationDataStoreProvider
import ch.protonmail.android.mailonboarding.data.OnboardingDataStoreProvider
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailspotlight.data.FeatureSpotlightDataStoreProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Resets the one-shot preferences that decide whether flows like onboarding, the feature spotlight
 * and the notification permission prompt are shown.
 *
 * Content search's prompts are held per account, so they are reset one account at a time.
 */
@HiltViewModel
internal class AppStateViewModel @Inject constructor(
    private val onboardingDataStoreProvider: OnboardingDataStoreProvider,
    private val featureSpotlightDataStoreProvider: FeatureSpotlightDataStoreProvider,
    private val notificationDataStoreProvider: NotificationDataStoreProvider,
    private val contentSearchDataStoreProvider: ContentSearchDataStoreProvider,
    userSessionRepository: UserSessionRepository
) : ViewModel() {

    private val mutableResets = MutableSharedFlow<Reset>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val resets: SharedFlow<Reset> = mutableResets.asSharedFlow()

    val accounts: StateFlow<List<DebugAccount>> = userSessionRepository.observeAccounts()
        .map { accounts ->
            accounts.map { account ->
                DebugAccount(account.userId, account.primaryAddress.ifBlank { account.name })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun resetOnboarding() = clear(ResetTarget.Onboarding, onboardingDataStoreProvider.onboardingDataStore)

    fun resetFeatureSpotlight() = clear(
        ResetTarget.FeatureSpotlight,
        featureSpotlightDataStoreProvider.featureSpotlightDataStore
    )

    fun resetNotificationPrompt() = clear(
        ResetTarget.NotificationPrompt,
        notificationDataStoreProvider.notificationPermissionStore
    )

    /** Makes [userId] eligible for one of the search screen's one-off banners again. */
    fun resetContentSearchPrompt(userId: UserId, prompt: ContentSearchPrompt) {
        viewModelScope.launch {
            contentSearchDataStoreProvider.contentSearchDataStore.edit { preferences ->
                preferences.remove(ContentSearchPromptRepositoryImpl.dismissedKey(userId, prompt))
            }
            mutableResets.emit(Reset(ResetTarget.ContentSearchPrompt, userId, prompt))
        }
    }

    private fun clear(target: ResetTarget, dataStore: DataStore<Preferences>) {
        viewModelScope.launch {
            dataStore.edit { it.clear() }
            mutableResets.emit(Reset(target))
        }
    }

    data class DebugAccount(val userId: UserId, val label: String)

    /** What was reset: for which account, and which banner, when the flag is held per account. */
    data class Reset(
        val target: ResetTarget,
        val userId: UserId? = null,
        val prompt: ContentSearchPrompt? = null
    )

    enum class ResetTarget {
        Onboarding,
        FeatureSpotlight,
        NotificationPrompt,
        ContentSearchPrompt
    }
}
