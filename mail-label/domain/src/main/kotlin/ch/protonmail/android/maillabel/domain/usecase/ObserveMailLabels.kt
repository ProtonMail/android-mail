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

package ch.protonmail.android.maillabel.domain.usecase

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
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
import me.proton.core.label.domain.repository.LabelRepository
import javax.inject.Inject

class ObserveMailLabels @Inject constructor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val labelRepository: LabelRepository,
) {

    operator fun invoke(userId: UserId) = combine(
        observeSystemLabelIds().map { it.toMailLabelSystem() },
        observeLabels(userId, MessageLabel).map { it.toMailLabelCustom() },
        observeLabels(userId, MessageFolder).map { it.toMailLabelCustom() },
    ) { defaults, labels, folders ->
        MailLabels(
            systems = defaults,
            labels = labels,
            folders = folders
        )
    }.flowOn(dispatcher)

    private fun observeSystemLabelIds() = flowOf(SystemLabelId.displayedList)

    private fun observeLabels(
        userId: UserId,
        type: LabelType,
    ) = labelRepository.observeLabels(userId, type)
        .mapSuccessValueOrNull()
        .mapLatest { list -> list.orEmpty().sortedBy { it.order } }
        .flowOn(dispatcher)
}
