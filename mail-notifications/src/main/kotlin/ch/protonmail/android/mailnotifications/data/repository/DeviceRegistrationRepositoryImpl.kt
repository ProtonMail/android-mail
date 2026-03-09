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

import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import timber.log.Timber
import uniffi.mail_uniffi.DeviceEnvironment
import uniffi.mail_uniffi.MailSessionRegisterDeviceTaskResult
import uniffi.mail_uniffi.RegisteredDevice
import uniffi.mail_uniffi.VoidActionResult
import javax.inject.Inject

internal class DeviceRegistrationRepositoryImpl @Inject constructor(
    private val mailSessionRepository: MailSessionRepository
) : DeviceRegistrationRepository {

    private var resultHandle: MailSessionRegisterDeviceTaskResult? = null

    override fun registerDeviceToken(token: String) {
        val device = RegisteredDevice(
            deviceToken = token,
            environment = DeviceEnvironment.GOOGLE,
            pingNotificationStatus = null,
            pushNotificationStatus = null
        )

        destroyExistingResultHandle()

        resultHandle = mailSessionRepository.getMailSession().registerDeviceTask()

        when (val currentResultHandle = resultHandle) {
            is MailSessionRegisterDeviceTaskResult.Error -> {
                Timber.tag("Register device token").d("error ${currentResultHandle.v1}")
            }

            is MailSessionRegisterDeviceTaskResult.Ok -> {
                when (val taskResult = currentResultHandle.v1.updateDevice(device)) {
                    is VoidActionResult.Error -> {
                        Timber.tag("Register device token").d("error registering device $taskResult")
                    }

                    is VoidActionResult.Ok -> {
                        Timber.tag("Register device token").d("Ok")
                    }
                }
            }

            else -> Unit
        }
    }

    private fun destroyExistingResultHandle() {
        resultHandle?.destroy()
        resultHandle = null
    }
}
