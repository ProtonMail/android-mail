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
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

class ObservePrivacySettings @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository,
    private val observePreventScreenshotsSetting: ObservePreventScreenshotsSetting
) {

    operator fun invoke(userId: UserId): Flow<Either<DataError, PrivacySettings>> {
        return combine(
            mailSettingsRepository.getMailSettingsFlow(userId, refresh = false).mapToEither(),
            observePreventScreenshotsSetting()
        ) { coreMailSettings, preventScreenshotSetting ->
            either {
                val mailSettings = coreMailSettings.bind()
                val preventScreenshots = preventScreenshotSetting.getOrElse {
                    return@combine DataError.Local.NoDataCached.left()
                }

                PrivacySettings(
                    autoShowRemoteContent = mailSettings.showImages.isAutoShowRemoteContentEnabled,
                    autoShowEmbeddedImages = mailSettings.showImages.isAutoShowEmbeddedImages,
                    preventTakingScreenshots = preventScreenshots.isEnabled,
                    requestLinkConfirmation = mailSettings.confirmLink ?: false
                )
            }
        }
    }
}

private val IntEnum<ShowImage>?.isAutoShowRemoteContentEnabled: Boolean
    get() = this?.enum == ShowImage.Remote || this?.enum == ShowImage.Both

private val IntEnum<ShowImage>?.isAutoShowEmbeddedImages: Boolean
    get() = this?.enum == ShowImage.Embedded || this?.enum == ShowImage.Both
