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

package ch.protonmail.android.mailsettings.data.repository.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEnabledEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedRemainingAttempts
import kotlinx.coroutines.flow.Flow

interface AutoLockLocalDataSource {

    suspend fun getAutoLockBiometricEncryptedValue(): Either<PreferencesError, AutoLockBiometricsEncryptedValue>
    fun observeAutoLockBiometricEncryptedValue(): Flow<Either<PreferencesError, AutoLockBiometricsEncryptedValue>>

    fun observeAutoLockEnabledEncryptedValue(): Flow<Either<PreferencesError, AutoLockEnabledEncryptedValue>>
    fun observeAutoLockEncryptedInterval(): Flow<Either<PreferencesError, AutoLockEncryptedInterval>>
    fun observeLastEncryptedForegroundMillis(): Flow<Either<PreferencesError, AutoLockEncryptedLastForegroundMillis>>
    fun observeAutoLockEncryptedAttemptsLeft(): Flow<Either<PreferencesError, AutoLockEncryptedRemainingAttempts>>
    fun observeAutoLockEncryptedPin(): Flow<Either<PreferencesError, AutoLockEncryptedPin>>
    fun observeAutoLockEncryptedPendingAttempt(): Flow<Either<PreferencesError, AutoLockEncryptedAttemptPendingStatus>>

    suspend fun updateAutoLockEnabledEncryptedValue(
        value: AutoLockEnabledEncryptedValue
    ): Either<PreferencesError, Unit>

    suspend fun updateAutoLockBiometricEncryptedValue(
        value: AutoLockBiometricsEncryptedValue
    ): Either<PreferencesError, Unit>

    suspend fun updateAutoLockEncryptedInterval(interval: AutoLockEncryptedInterval): Either<PreferencesError, Unit>
    suspend fun updateLastEncryptedForegroundMillis(
        timestamp: AutoLockEncryptedLastForegroundMillis
    ): Either<PreferencesError, Unit>

    suspend fun updateAutoLockEncryptedPin(pin: AutoLockEncryptedPin): Either<PreferencesError, Unit>
    suspend fun updateAutoLockAttemptsLeft(attempts: AutoLockEncryptedRemainingAttempts): Either<PreferencesError, Unit>

    suspend fun updateAutoLockPendingAttempt(
        pendingAttempt: AutoLockEncryptedAttemptPendingStatus
    ): Either<PreferencesError, Unit>
}
