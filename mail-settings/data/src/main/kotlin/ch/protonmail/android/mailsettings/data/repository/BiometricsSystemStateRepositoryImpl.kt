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

import android.content.Context
import androidx.biometric.BiometricManager
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.BiometricsSystemState
import ch.protonmail.android.mailsettings.domain.repository.BiometricsSystemStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

class BiometricsSystemStateRepositoryImpl @Inject constructor(
    @ApplicationContext
    private val context: Context
) : BiometricsSystemStateRepository {

    private val _stateFlow = MutableStateFlow<BiometricsSystemState?>(null)
    private val stateFlow: StateFlow<BiometricsSystemState?> = _stateFlow

    init {
        _stateFlow.value = getCurrentState()
    }

    override fun getCurrentState(): BiometricsSystemState {
        return when (BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricsSystemState.BiometricEnrolled
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricsSystemState.BiometricNotEnrolled
            else -> BiometricsSystemState.BiometricNotAvailable
        }
    }

    override fun observe(): Flow<BiometricsSystemState> = stateFlow.filterNotNull()
}
