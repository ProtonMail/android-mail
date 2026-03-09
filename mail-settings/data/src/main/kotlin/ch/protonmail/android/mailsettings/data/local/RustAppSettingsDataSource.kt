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

package ch.protonmail.android.mailsettings.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalAppSettings
import ch.protonmail.android.mailcommon.data.mapper.LocalAppSettingsDiff
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import uniffi.mail_uniffi.MailSessionChangeAppSettingsResult
import uniffi.mail_uniffi.MailSessionGetAppSettingsResult
import javax.inject.Inject

class RustAppSettingsDataSource @Inject constructor() : AppSettingsDataSource {

    override suspend fun getAppSettings(mailSession: MailSessionWrapper): Either<DataError, LocalAppSettings> =
        when (val result = mailSession.getRustMailSession().getAppSettings()) {
            is MailSessionGetAppSettingsResult.Error -> result.v1.toDataError().left()
            is MailSessionGetAppSettingsResult.Ok -> result.v1.right()
        }

    override suspend fun updateAppSettings(
        mailSession: MailSessionWrapper,
        appSettingsDiff: LocalAppSettingsDiff
    ): Either<DataError, Unit> =
        when (val result = mailSession.getRustMailSession().changeAppSettings(appSettingsDiff)) {
            is MailSessionChangeAppSettingsResult.Error -> result.v1.toDataError().left()
            is MailSessionChangeAppSettingsResult.Ok -> Unit.right()
        }
}
