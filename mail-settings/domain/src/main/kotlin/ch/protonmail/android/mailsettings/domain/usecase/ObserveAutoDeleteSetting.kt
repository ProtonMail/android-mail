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

package ch.protonmail.android.mailsettings.domain.usecase

import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

class ObserveAutoDeleteSetting @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository,
    private val isPaidMailUser: IsPaidMailUser,
    private val observeUpsellingVisibility: ObserveUpsellingVisibility,
    private val observePrimaryUser: ObservePrimaryUser
) {

    operator fun invoke(): Flow<AutoDeleteSetting> = observePrimaryUser()
        .distinctUntilChanged()
        .filterNotNull()
        .flatMapConcat {
            mailSettingsRepository.getMailSettingsFlow(it.userId)
                .mapSuccessValueOrNull()
                .filterNotNull()
        }
        .combine(
            observeUpsellingVisibility(UpsellingEntryPoint.Feature.AutoDelete)
        ) { mailSettings, shouldShowUpselling ->

            val hasMailSubscription = isPaidMailUser(mailSettings.userId).getOrNull()

            when (mailSettings.autoDeleteSpamAndTrashDays) {
                null -> {
                    if (hasMailSubscription == true) {
                        AutoDeleteSetting.NotSet.PaidUser
                    } else {
                        if (shouldShowUpselling) {
                            AutoDeleteSetting.NotSet.FreeUser.UpsellingOn
                        } else AutoDeleteSetting.NotSet.FreeUser.UpsellingOff
                    }
                }

                0 -> AutoDeleteSetting.Disabled
                else -> AutoDeleteSetting.Enabled
            }
        }

}
