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

package ch.protonmail.android.maillabel.domain.usecase

import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

class GetDraftLabelId @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository,
    private val observePrimaryUserId: ObservePrimaryUserId
) {

    suspend operator fun invoke(): MailLabelId {
        val userId = observePrimaryUserId.invoke().filterNotNull().first()
        val mailSettings = mailSettingsRepository.getMailSettings(userId)
        val showMoved = mailSettings.showMoved
        return if (showMoved?.enum == ShowMoved.Both) {
            MailLabelId.System.AllDrafts
        } else {
            MailLabelId.System.Drafts
        }
    }
}
