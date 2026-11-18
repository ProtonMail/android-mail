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
import arrow.core.Either
import arrow.core.flatMap
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailbyoe.domain.usecase.SyncOnboardingFinishedToSdk
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.local.ContentSearchDataStoreProvider
import ch.protonmail.android.mailcontentsearch.data.repository.ContentSearchPromptRepositoryImpl
import ch.protonmail.android.mailcontentsearch.domain.model.ContentSearchPrompt
import ch.protonmail.android.mailnotifications.data.local.NotificationDataStoreProvider
import ch.protonmail.android.mailonboarding.data.OnboardingDataStoreProvider
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailspotlight.data.FeatureSpotlightDataStoreProvider
import ch.protonmail.android.mailspotlight.domain.repository.FeatureGateRepository
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
import timber.log.Timber
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
    private val featureGateRepository: FeatureGateRepository,
    private val byoeGuidanceRepository: ByoeGuidanceRepository,
    private val syncOnboardingFinishedToSdk: SyncOnboardingFinishedToSdk,
    private val userSessionRepository: UserSessionRepository
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

    /**
     * Makes the onboarding show again on the next app start, for every signed-in account.
     *
     * The SDK keeps its own record of the onboarding steps and reads it to decide on the BYOE page and on which
     * announcements an account gets, so that record is reset along with the app's own.
     */
    fun resetOnboarding() {
        viewModelScope.launch {
            onboardingDataStoreProvider.onboardingDataStore.edit { it.clear() }
            resetInSdk(ResetTarget.Onboarding) { userId ->
                byoeGuidanceRepository.debugResetOnboardingSteps(userId)
                    .onRight { featureGateRepository.refreshEntryPoints(userId) }
            }
        }
    }

    /**
     * Makes the content search What's New and the BYOE spotlight show again when due, for every signed-in account.
     *
     * The SDK decides which announcement shows, so its records are reset along with the app's own. As on iOS, the
     * SDK also forgets everything else it recorded as shown, the onboarding steps included: an account that went
     * through the onboarding then counts as an existing user, the only kind that gets announcements. The finished
     * onboarding is recorded again at once, as the app does at every start.
     */
    fun resetFeatureSpotlight() {
        viewModelScope.launch {
            featureSpotlightDataStoreProvider.featureSpotlightDataStore.edit { it.clear() }
            resetInSdk(ResetTarget.FeatureSpotlight) { userId ->
                featureGateRepository.debugResetAnnouncements(userId)
                    .flatMap { byoeGuidanceRepository.debugClearScreenVisibility(userId) }
                    .flatMap { syncOnboardingFinishedToSdk(userId) }
                    .onRight { featureGateRepository.refreshEntryPoints(userId) }
            }
        }
    }

    /**
     * Runs [reset] for every signed-in account, and confirms the [target] only when all of them succeeded: a reset
     * the SDK did not take would otherwise look done while the screen still does not come back.
     *
     * The accounts are read from the SDK at the tap, not from [accounts], which is empty until its first value.
     */
    private suspend fun resetInSdk(target: ResetTarget, reset: suspend (UserId) -> Either<DataError, Unit>) {
        val failures = userSessionRepository.getReadyUserIds().mapNotNull { userId ->
            reset(userId).leftOrNull()?.also { Timber.w("debug-menu: SDK reset of $target failed for $userId: $it") }
        }
        if (failures.isEmpty()) mutableResets.emit(Reset(target))
    }

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
