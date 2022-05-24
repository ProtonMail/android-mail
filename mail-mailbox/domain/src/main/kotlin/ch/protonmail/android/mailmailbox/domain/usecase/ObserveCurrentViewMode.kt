/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

class ObserveCurrentViewMode @Inject constructor(
    private val observeMailSettings: ObserveMailSettings,
) {

    private val messagesOnlyLabelsIds = listOf(
        SystemLabelId.Drafts,
        SystemLabelId.AllDrafts,
        SystemLabelId.Sent,
        SystemLabelId.AllSent
    ).map { it.labelId }

    operator fun invoke(selectedMailLabelId: MailLabelId): Flow<ViewMode> =
        when (selectedMailLabelId.labelId) {
            in messagesOnlyLabelsIds -> flowOf(ViewMode.NoConversationGrouping)
            else -> invoke()
        }

    operator fun invoke(): Flow<ViewMode> = observeMailSettings()
        .mapLatest { it?.viewMode?.enum ?: DefaultViewMode }
        .distinctUntilChanged()

    companion object {

        val DefaultViewMode = ViewMode.NoConversationGrouping
    }
}
