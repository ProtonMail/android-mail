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

package ch.protonmail.android.mailsettings.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockRemainingAttempts
import kotlinx.coroutines.flow.Flow

interface AutoLockRepository {

    suspend fun getCurrentAutoLockBiometricsPreference(): Either<AutoLockPreferenceError, AutoLockBiometricsPreference>
    fun observeAutoLockBiometricsPreference(): Flow<Either<AutoLockPreferenceError, AutoLockBiometricsPreference>>
    fun observeAutoLockEnabledValue(): Flow<Either<AutoLockPreferenceError, AutoLockPreference>>
    fun observeAutoLockInterval(): Flow<Either<AutoLockPreferenceError, AutoLockInterval>>
    fun observeAutoLockLastForegroundMillis(): Flow<Either<AutoLockPreferenceError, AutoLockLastForegroundMillis>>
    fun observeAutoLockPin(): Flow<Either<AutoLockPreferenceError, AutoLockPin>>
    fun observeAutoLockRemainingAttempts(): Flow<Either<AutoLockPreferenceError, AutoLockRemainingAttempts>>
    fun observeAutoLockAttemptPendingStatus(): Flow<Either<AutoLockPreferenceError, AutoLockAttemptPendingStatus>>

    suspend fun updateAutoLockBiometricsPreference(
        value: AutoLockBiometricsPreference
    ): Either<AutoLockPreferenceError, Unit>

    suspend fun updateAutoLockEnabledValue(value: AutoLockPreference): Either<AutoLockPreferenceError, Unit>
    suspend fun updateAutoLockInterval(interval: AutoLockInterval): Either<AutoLockPreferenceError, Unit>
    suspend fun updateLastForegroundMillis(
        timestamp: AutoLockLastForegroundMillis
    ): Either<AutoLockPreferenceError, Unit>

    suspend fun updateAutoLockPin(pin: AutoLockPin): Either<AutoLockPreferenceError, Unit>

    suspend fun updateAutoLockRemainingAttempts(
        attempts: AutoLockRemainingAttempts
    ): Either<AutoLockPreferenceError, Unit>

    suspend fun updateAutoLockAttemptPendingStatus(
        pendingAttempt: AutoLockAttemptPendingStatus
    ): Either<AutoLockPreferenceError, Unit>
}

sealed interface AutoLockPreferenceError {
    object DataStoreError : AutoLockPreferenceError
    data class DeserializationError(val message: String) : AutoLockPreferenceError
    data class SerializationError(val message: String) : AutoLockPreferenceError
}
