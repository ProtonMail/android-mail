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
import arrow.core.getOrElse
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.repository.local.AutoLockLocalDataSource
import ch.protonmail.android.mailsettings.data.usecase.DecryptSerializableValue
import ch.protonmail.android.mailsettings.data.usecase.EncryptSerializableValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEnabledEncryptedValue
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockEncryptedRemainingAttempts
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockRemainingAttempts
import ch.protonmail.android.mailsettings.domain.repository.AutoLockPreferenceError
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AutoLockRepositoryImpl @Inject constructor(
    private val autoLockLocalDataSource: AutoLockLocalDataSource,
    private val encryptSerializableValue: EncryptSerializableValue,
    private val decryptSerializableValue: DecryptSerializableValue
) : AutoLockRepository {

    override suspend fun getCurrentAutoLockBiometricsPreference():
        Either<AutoLockPreferenceError, AutoLockBiometricsPreference> =
        autoLockLocalDataSource.getAutoLockBiometricEncryptedValue().let {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockBiometricsPreference>(encryptedValue).bind()
            }
        }

    override fun observeAutoLockBiometricsPreference(): AutoLockPreferenceEitherFlow<AutoLockBiometricsPreference> =
        autoLockLocalDataSource.observeAutoLockBiometricEncryptedValue().map {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockBiometricsPreference>(encryptedValue).bind()
            }
        }

    override fun observeAutoLockEnabledValue(): AutoLockPreferenceEitherFlow<AutoLockPreference> =
        autoLockLocalDataSource.observeAutoLockEnabledEncryptedValue().map {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockPreference>(encryptedValue).bind()
            }
        }

    override fun observeAutoLockInterval(): AutoLockPreferenceEitherFlow<AutoLockInterval> =
        autoLockLocalDataSource.observeAutoLockEncryptedInterval().map {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockInterval>(encryptedValue).bind()
            }
        }

    override fun observeAutoLockLastForegroundMillis(): AutoLockPreferenceEitherFlow<AutoLockLastForegroundMillis> =
        autoLockLocalDataSource.observeLastEncryptedForegroundMillis().map {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockLastForegroundMillis>(encryptedValue).bind()
            }
        }

    override fun observeAutoLockPin(): AutoLockPreferenceEitherFlow<AutoLockPin> =
        autoLockLocalDataSource.observeAutoLockEncryptedPin().map {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockPin>(encryptedValue).bind()
            }
        }

    override fun observeAutoLockRemainingAttempts(): AutoLockPreferenceEitherFlow<AutoLockRemainingAttempts> =
        autoLockLocalDataSource.observeAutoLockEncryptedAttemptsLeft().map {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockRemainingAttempts>(encryptedValue).bind()
            }
        }

    override fun observeAutoLockAttemptPendingStatus(): AutoLockPreferenceEitherFlow<AutoLockAttemptPendingStatus> =
        autoLockLocalDataSource.observeAutoLockEncryptedPendingAttempt().map {
            either {
                val encryptedValue = it.getOrElse { raise(AutoLockPreferenceError.DataStoreError) }.encryptedValue
                decryptSerializedValue<AutoLockAttemptPendingStatus>(encryptedValue).bind()
            }
        }

    override suspend fun updateAutoLockBiometricsPreference(
        value: AutoLockBiometricsPreference
    ): Either<AutoLockPreferenceError, Unit> = either {
        val encryptedValue = encryptValueWithSerialization(value).bind()
        autoLockLocalDataSource.updateAutoLockBiometricEncryptedValue(
            AutoLockBiometricsEncryptedValue(
                encryptedValue
            )
        )
            .mapEither()
            .bind()
    }

    override suspend fun updateAutoLockEnabledValue(value: AutoLockPreference): AutoLockPreferenceEither<Unit> =
        either {
            val encryptedValue = encryptValueWithSerialization(value).bind()
            autoLockLocalDataSource.updateAutoLockEnabledEncryptedValue(AutoLockEnabledEncryptedValue(encryptedValue))
                .mapEither()
                .bind()
        }

    override suspend fun updateAutoLockInterval(interval: AutoLockInterval): AutoLockPreferenceEither<Unit> = either {
        val encryptedValue = encryptValueWithSerialization(interval).bind()
        autoLockLocalDataSource.updateAutoLockEncryptedInterval(AutoLockEncryptedInterval(encryptedValue)).mapEither()
            .bind()
    }

    override suspend fun updateAutoLockPin(pin: AutoLockPin): AutoLockPreferenceEither<Unit> = either {
        val encryptedValue = encryptValueWithSerialization(pin).bind()
        autoLockLocalDataSource.updateAutoLockEncryptedPin(AutoLockEncryptedPin(encryptedValue)).mapEither().bind()
    }

    override suspend fun updateLastForegroundMillis(
        timestamp: AutoLockLastForegroundMillis
    ): AutoLockPreferenceEither<Unit> = either {
        val encryptedValue = encryptValueWithSerialization(timestamp).bind()
        autoLockLocalDataSource.updateLastEncryptedForegroundMillis(
            AutoLockEncryptedLastForegroundMillis(encryptedValue)
        )
            .mapEither()
            .bind()
    }

    override suspend fun updateAutoLockRemainingAttempts(attempts: AutoLockRemainingAttempts) = either {
        val encryptedValue = encryptValueWithSerialization(attempts).bind()
        autoLockLocalDataSource.updateAutoLockAttemptsLeft(
            AutoLockEncryptedRemainingAttempts(encryptedValue)
        )
            .mapEither()
            .bind()
    }

    override suspend fun updateAutoLockAttemptPendingStatus(pendingAttempt: AutoLockAttemptPendingStatus) = either {
        val encryptedValue = encryptValueWithSerialization(pendingAttempt).bind()
        autoLockLocalDataSource.updateAutoLockPendingAttempt(
            AutoLockEncryptedAttemptPendingStatus(encryptedValue)
        )
            .mapEither()
            .bind()
    }

    private suspend inline fun <reified T> decryptSerializedValue(value: String) = either {
        decryptSerializableValue<T>(value).getOrElse {
            raise(AutoLockPreferenceError.DeserializationError(it.message))
        }
    }

    private suspend inline fun <reified T> encryptValueWithSerialization(value: T) = either {
        encryptSerializableValue(value).getOrElse {
            raise(AutoLockPreferenceError.SerializationError(it.message))
        }
    }

    private fun Either<PreferencesError, Unit>.mapEither() = either {
        getOrElse { raise(AutoLockPreferenceError.DataStoreError) }
    }
}

private typealias AutoLockPreferenceEitherFlow<T> = Flow<AutoLockPreferenceEither<T>>
private typealias AutoLockPreferenceEither<T> = Either<AutoLockPreferenceError, T>
