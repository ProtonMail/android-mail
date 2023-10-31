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

package ch.protonmail.android.mailsettings.domain.usecase.privacy

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

class UpdateAutoShowEmbeddedImagesSetting @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val mailSettingsRepository: MailSettingsRepository
) {

    suspend operator fun invoke(newValue: Boolean): Either<DataError, Unit> {
        val userId = observePrimaryUserId().firstOrNull() ?: return DataError.Local.NoDataCached.left()
        val currentValue = mailSettingsRepository.getMailSettings(userId, refresh = false).showImages
        val enum = currentValue?.enum ?: return DataError.Local.NoDataCached.left()

        if (enum.includesEmbedded() != newValue) {
            mailSettingsRepository.updateShowImages(userId, enum.toggleEmbedded())
        }

        return Unit.right()
    }
}
