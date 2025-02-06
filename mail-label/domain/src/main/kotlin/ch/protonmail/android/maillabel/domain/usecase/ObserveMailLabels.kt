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

import ch.protonmail.android.mailcommon.domain.coroutines.DefaultDispatcher
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.Companion.defaultDisplayedList
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.Companion.showAllDefaultDisplayedList
import ch.protonmail.android.maillabel.domain.model.SystemLabelId.Companion.showAllDisplayedList
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelCustom
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.LabelType.MessageFolder
import me.proton.core.label.domain.entity.LabelType.MessageLabel
import me.proton.core.mailsettings.domain.entity.AlmostAllMail
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import javax.inject.Inject

class ObserveMailLabels @Inject constructor(
    @DefaultDispatcher
    private val dispatcher: CoroutineDispatcher,
    private val observeLabels: ObserveLabels,
    private val mailSettingsRepository: MailSettingsRepository
) {

    operator fun invoke(userId: UserId, respectSettings: Boolean = false) = combine(
        observeSystemLabelIds(userId, respectSettings)
            .map { it.toMailLabelSystem() },
        observeCustomLabels(userId, MessageLabel).map { it.toMailLabelCustom() },
        observeCustomLabels(userId, MessageFolder).map { it.toMailLabelCustom() }
    ) { defaults, labels, folders ->
        MailLabels(
            systemLabels = defaults,
            labels = labels,
            folders = folders
        )
    }.flowOn(dispatcher)

    private fun observeSystemLabelIds(userId: UserId, respectSettings: Boolean = false) = if (respectSettings) {
        mailSettingsRepository.getMailSettingsFlow(userId).mapSuccessValueOrNull()
            .mapLatest { generateSystemLabelsBasedOnSettings(it) }
            .flowOn(dispatcher)
    } else {
        flowOf(SystemLabelId.displayedList)
    }

    private fun generateSystemLabelsBasedOnSettings(mailSettings: MailSettings?): MutableList<SystemLabelId> {
        val displayedList = mutableListOf(SystemLabelId.Inbox)

        displayedList += if (mailSettings?.showMoved?.enum == ShowMoved.Both) {
            showAllDisplayedList
        } else {
            showAllDefaultDisplayedList
        }

        displayedList += defaultDisplayedList

        displayedList += if (mailSettings?.almostAllMail?.enum == AlmostAllMail.Enabled) {
            SystemLabelId.AlmostAllMail
        } else {
            SystemLabelId.AllMail
        }

        return displayedList
    }

    private fun observeCustomLabels(userId: UserId, type: LabelType) = observeLabels(userId, type)
        .map { it.getOrNull() }
        .mapLatest { list ->
            list.orEmpty()
                .filter { !it.labelId.isReservedSystemLabelId() }
                .sortedBy { it.order }
                .map { it.copy(isExpanded = true) } // Temporary fix until folder hierarchy is supported
        }
        .flowOn(dispatcher)
}
