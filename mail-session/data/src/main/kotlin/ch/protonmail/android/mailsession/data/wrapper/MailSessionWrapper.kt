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

package ch.protonmail.android.mailsession.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalAutoLockPin
import ch.protonmail.android.mailcommon.data.mapper.LocalUserId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.mapper.toAutoLockPinError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.BackgroundExecutionCallback
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionDeletePinCodeResult
import uniffi.mail_uniffi.MailSessionGetAccountResult
import uniffi.mail_uniffi.MailSessionGetAccountSessionsResult
import uniffi.mail_uniffi.MailSessionGetPrimaryAccountResult
import uniffi.mail_uniffi.MailSessionGetSessionsResult
import uniffi.mail_uniffi.MailSessionInitializedUserSessionFromStoredSessionResult
import uniffi.mail_uniffi.MailSessionNewLoginFlowResult
import uniffi.mail_uniffi.MailSessionRemainingPinAttemptsResult
import uniffi.mail_uniffi.MailSessionSetBiometricsAppProtectionResult
import uniffi.mail_uniffi.MailSessionSetPinCodeResult
import uniffi.mail_uniffi.MailSessionUnsetBiometricsAppProtectionResult
import uniffi.mail_uniffi.MailSessionUserSessionFromStoredSessionResult
import uniffi.mail_uniffi.MailSessionVerifyPinCodeResult
import uniffi.mail_uniffi.MeasurementEventType
import uniffi.mail_uniffi.MeasurementValue
import uniffi.mail_uniffi.StoredAccount
import uniffi.mail_uniffi.StoredSession

class MailSessionWrapper(private val mailSession: MailSession) {

    fun getRustMailSession() = mailSession

    suspend fun getAccount(userId: LocalUserId): Either<DataError, StoredAccount> =
        when (val result = mailSession.getAccount(userId)) {
            is MailSessionGetAccountResult.Error -> result.v1.toDataError().left()
            is MailSessionGetAccountResult.Ok -> {
                when (val data = result.v1) {
                    null -> DataError.Local.NotFound.left()
                    else -> data.right()
                }
            }
        }

    suspend fun getPrimaryAccount(): Either<DataError, StoredAccount> =
        when (val result = mailSession.getPrimaryAccount()) {
            is MailSessionGetPrimaryAccountResult.Error -> result.v1.toDataError().left()
            is MailSessionGetPrimaryAccountResult.Ok -> {
                when (val data = result.v1) {
                    null -> DataError.Local.NotFound.left()
                    else -> data.right()
                }
            }
        }

    suspend fun getAccountSessions(account: StoredAccount): Either<DataError, List<StoredSession>> =
        when (val result = mailSession.getAccountSessions(account)) {
            is MailSessionGetAccountSessionsResult.Error -> result.v1.toDataError().left()
            is MailSessionGetAccountSessionsResult.Ok -> result.v1.right()
        }

    suspend fun getSessions(): Either<DataError, List<StoredSession>> = when (val result = mailSession.getSessions()) {
        is MailSessionGetSessionsResult.Error -> result.v1.toDataError().left()
        is MailSessionGetSessionsResult.Ok -> result.v1.right()
    }

    suspend fun userContextFromSession(session: StoredSession): Either<DataError, MailUserSessionWrapper> =
        when (val result = mailSession.userSessionFromStoredSession(session)) {
            is MailSessionUserSessionFromStoredSessionResult.Error -> result.v1.toDataError().left()
            is MailSessionUserSessionFromStoredSessionResult.Ok -> MailUserSessionWrapper(result.v1).right()
        }

    suspend fun initializedUserContextFromSession(session: StoredSession): Either<DataError, MailUserSessionWrapper?> =
        when (val result = mailSession.initializedUserSessionFromStoredSession(session)) {
            is MailSessionInitializedUserSessionFromStoredSessionResult.Error ->
                result.v1.toDataError().left()

            is MailSessionInitializedUserSessionFromStoredSessionResult.Ok ->
                result.v1?.let { MailUserSessionWrapper(it) }.right()
        }

    suspend fun deleteAccount(userId: LocalUserId) = mailSession.deleteAccount(userId)

    suspend fun logoutAccount(userId: LocalUserId) = mailSession.logoutAccount(userId)

    suspend fun setPrimaryAccount(userId: LocalUserId) = mailSession.setPrimaryAccount(userId)

    suspend fun setAutoLockPinCode(localAutoLockPin: LocalAutoLockPin) =
        when (val result = mailSession.setPinCode(localAutoLockPin)) {
            is MailSessionSetPinCodeResult.Ok -> Unit.right()
            is MailSessionSetPinCodeResult.Error -> result.v1.toAutoLockPinError().left()
        }

    suspend fun verifyPinCode(localAutoLockPin: LocalAutoLockPin) =
        when (val result = mailSession.verifyPinCode(localAutoLockPin)) {
            is MailSessionVerifyPinCodeResult.Ok -> Unit.right()
            is MailSessionVerifyPinCodeResult.Error -> result.v1.toAutoLockPinError().left()
        }

    suspend fun deleteAutoLockPinCode(localAutoLockPin: LocalAutoLockPin) =
        when (val result = mailSession.deletePinCode(localAutoLockPin)) {
            is MailSessionDeletePinCodeResult.Ok -> Unit.right()
            is MailSessionDeletePinCodeResult.Error -> result.v1.toAutoLockPinError().left()
        }

    suspend fun setBiometricAppProtection() = when (val result = mailSession.setBiometricsAppProtection()) {
        is MailSessionSetBiometricsAppProtectionResult.Ok -> Unit.right()
        is MailSessionSetBiometricsAppProtectionResult.Error -> result.v1.toDataError().left()
    }

    suspend fun unsetBiometricAppProtection() = when (val result = mailSession.unsetBiometricsAppProtection()) {
        is MailSessionUnsetBiometricsAppProtectionResult.Ok -> Unit.right()
        is MailSessionUnsetBiometricsAppProtectionResult.Error -> result.v1.toDataError().left()
    }

    fun signalBiometricsCheckPassed() = mailSession.biometricsCheckPassed()

    suspend fun getRemainingAttempts() = when (val result = mailSession.remainingPinAttempts()) {
        is MailSessionRemainingPinAttemptsResult.Error -> result.v1.toDataError().left()
        is MailSessionRemainingPinAttemptsResult.Ok -> result.v1.right()
    }

    fun startAutoLockCountdown() = mailSession.startAutoLockCountdown()

    fun registerDeviceTask() = mailSession.registerDeviceTask()

    fun startBackgroundTask(callback: BackgroundExecutionCallback) = mailSession.startBackgroundExecution(callback)

    /**
     * Used to pause work when the app is sent to the background.
     */
    fun onExitForeground() = mailSession.onExitForeground()

    /**
     * Used to resume work when the app is brought back to the foreground.
     */
    fun onEnterForeground() = mailSession.onEnterForeground()

    suspend fun sendMeasurementEvent(
        eventType: MeasurementEventType,
        asid: String,
        appPackageName: String,
        fields: Map<String, MeasurementValue?>
    ) = mailSession.recordMeasurementPrelogin(eventType, asid, appPackageName, fields)

    suspend fun newLoginFlow(): Either<DataError, LoginFlowWrapper> {
        return when (val result = mailSession.newLoginFlow()) {
            is MailSessionNewLoginFlowResult.Ok -> LoginFlowWrapper(result.v1).right()
            is MailSessionNewLoginFlowResult.Error -> result.v1.toDataError().left()
        }
    }
}
