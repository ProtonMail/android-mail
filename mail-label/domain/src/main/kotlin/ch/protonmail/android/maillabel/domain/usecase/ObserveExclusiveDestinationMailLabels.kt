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

import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelCustom
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject

class ObserveExclusiveDestinationMailLabels @Inject constructor(
    private val observeLabels: ObserveLabels
) {

    operator fun invoke(userId: UserId) = combine(
        observeSystemLabelIds().map { it.toMailLabelSystem() },
        observeMessageFolders(userId).map { it.toMailLabelCustom() }
    ) { defaults, folders ->
        MailLabels(
            systemLabels = defaults,
            labels = emptyList(),
            folders = folders
        )
    }

    private fun observeSystemLabelIds() = flowOf(SystemLabelId.exclusiveDestinationList)

    private fun observeMessageFolders(userId: UserId) = observeLabels(userId, LabelType.MessageFolder)
        .map { it.getOrNull() }
        .mapLatest { list ->
            list.orEmpty()
                .filter { !it.labelId.isReservedSystemLabelId() }
                .sortedBy { it.order }
        }
}
