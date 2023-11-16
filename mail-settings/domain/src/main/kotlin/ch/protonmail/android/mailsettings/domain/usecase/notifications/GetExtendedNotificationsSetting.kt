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

package ch.protonmail.android.mailsettings.domain.usecase.notifications

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsettings.domain.model.ExtendedNotificationPreference
import ch.protonmail.android.mailsettings.domain.repository.NotificationsSettingsRepository
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetExtendedNotificationsSetting @Inject constructor(
    private val notificationsSettingsRepository: NotificationsSettingsRepository
) {

    suspend operator fun invoke(): Either<DataError, ExtendedNotificationPreference> = either {
        notificationsSettingsRepository.observeExtendedNotificationsSetting().filterNotNull().first().getOrElse {
            raise(DataError.Local.NoDataCached)
        }
    }
}
