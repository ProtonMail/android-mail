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

package ch.protonmail.android.mailnotifications.data.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import uniffi.mail_uniffi.DeviceEnvironment
import uniffi.mail_uniffi.MailSessionRegisterDeviceTaskResult
import uniffi.mail_uniffi.RegisteredDevice
import uniffi.mail_uniffi.VoidActionResult
import javax.inject.Inject

internal class DeviceRegistrationRepositoryImpl @Inject constructor(
    private val mailSessionRepository: MailSessionRepository,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : DeviceRegistrationRepository {

    private var resultHandle: MailSessionRegisterDeviceTaskResult? = null

    override suspend fun registerDeviceToken(token: String): Either<DataError, Unit> = withContext(ioDispatcher) {
        val device = RegisteredDevice(
            deviceToken = token,
            environment = DeviceEnvironment.GOOGLE,
            pingNotificationStatus = null,
            pushNotificationStatus = null
        )

        destroyExistingResultHandle()

        mailSessionRepository.runInRustBackground { mailSession ->
            when (val result = mailSession.registerDeviceTask()) {
                is MailSessionRegisterDeviceTaskResult.Error -> {
                    Timber.tag("Register device token").d("error ${result.v1}")
                    DataError.Remote.Unknown.left()
                }

                is MailSessionRegisterDeviceTaskResult.Ok -> {
                    resultHandle = result

                    when (val taskResult = result.v1.updateDevice(device)) {
                        is VoidActionResult.Error -> {
                            Timber.tag("Register device token").d("Error registering device $taskResult")
                            DataError.Remote.Unknown.left()
                        }

                        is VoidActionResult.Ok -> {
                            Timber.tag("Register device token").d("Successfully registered device.")
                            Unit.right()
                        }
                    }
                }
            }
        }
    }

    private fun destroyExistingResultHandle() {
        resultHandle?.destroy()
        resultHandle = null
    }
}
