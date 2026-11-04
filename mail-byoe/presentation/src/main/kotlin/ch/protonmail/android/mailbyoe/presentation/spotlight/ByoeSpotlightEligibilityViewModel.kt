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

package ch.protonmail.android.mailbyoe.presentation.spotlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailbyoe.domain.usecase.DismissByoeGuidance
import ch.protonmail.android.mailbyoe.domain.usecase.GetByoeSpotlightTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.RecordByoeSpotlightShown
import ch.protonmail.android.mailbyoe.domain.usecase.SyncOnboardingFinishedToSdk
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Decides on app open whether an existing user gets the BYOE spotlight. The SDK stops offering it once it is
 * recorded as shown, so the variant is resolved here and handed to the spotlight screen.
 */
@HiltViewModel
class ByoeSpotlightEligibilityViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val getByoeSpotlightTreatment: GetByoeSpotlightTreatment,
    private val recordByoeSpotlightShown: RecordByoeSpotlightShown,
    private val dismissByoeGuidance: DismissByoeGuidance,
    private val syncOnboardingFinishedToSdk: SyncOnboardingFinishedToSdk
) : ViewModel() {

    private val mutableState = MutableStateFlow<ByoeSpotlightEligibilityState>(ByoeSpotlightEligibilityState.Loading)
    val state: StateFlow<ByoeSpotlightEligibilityState> = mutableState.asStateFlow()

    init {
        // Waits for a signed-in user. Only the first check counts: the spotlight is an app-open prompt.
        viewModelScope.launch {
            observePrimaryUserId().filterNotNull().distinctUntilChanged().collect { userId ->
                val result = eligibilityFor(userId)
                // After the timeout, or once a first result is in, Home has moved on, so a late result must not
                // open the spotlight over it.
                if (!mutableState.compareAndSet(ByoeSpotlightEligibilityState.Loading, result)) {
                    Timber.d("byoe: the spotlight check was already decided, ignoring this result")
                }
            }
        }
        // Home holds back every prompt while this is loading, so give up on the spotlight rather than block them.
        viewModelScope.launch {
            delay(LoadingTimeout)
            if (mutableState.compareAndSet(ByoeSpotlightEligibilityState.Loading, ByoeSpotlightEligibilityState.Hide)) {
                Timber.w("byoe: the spotlight check took too long, skipping the spotlight")
            }
        }
    }

    /** The spotlight is on screen: it is hidden from now on, so Home does not open it again. */
    fun onOpened() {
        mutableState.value = ByoeSpotlightEligibilityState.Hide
        viewModelScope.launch { recordByoeSpotlightShown(primaryUserId()) }
    }

    /** "Later" or back: the user dismissed the spotlight, which starts the sidebar entry. */
    fun onDismissed() {
        viewModelScope.launch { dismissByoeGuidance(primaryUserId()) }
    }

    private suspend fun eligibilityFor(userId: UserId): ByoeSpotlightEligibilityState = try {
        syncOnboardingFinishedToSdk(userId)
        getByoeSpotlightTreatment(userId)
            ?.let { ByoeSpotlightEligibilityState.Show(it) }
            ?: ByoeSpotlightEligibilityState.Hide
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "byoe: checking the spotlight failed, skipping it")
        ByoeSpotlightEligibilityState.Hide
    }

    private suspend fun primaryUserId() = observePrimaryUserId().filterNotNull().first()

    private companion object {

        val LoadingTimeout = 5.seconds
    }
}
