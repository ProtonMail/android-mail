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
import ch.protonmail.android.maillabel.domain.model.toDynamicSystemMailLabel
import ch.protonmail.android.maillabel.domain.model.toMailLabelCustom
import ch.protonmail.android.maillabel.domain.repository.LabelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ObserveMailLabels @Inject constructor(
    @DefaultDispatcher
    private val dispatcher: CoroutineDispatcher,
    private val labelRepository: LabelRepository
) {

    operator fun invoke(userId: UserId) = combine(
        observeDynamicSystemLabels(userId).map { it.toDynamicSystemMailLabel() },
        observeCustomLabels(userId).map { it.toMailLabelCustom() },
        observeCustomFolders(userId).map { it.toMailLabelCustom() }
    ) { system, labels, folders ->
        MailLabels(
            system = system,
            folders = folders,
            labels = labels
        )
    }.flowOn(dispatcher)

    private fun observeDynamicSystemLabels(userId: UserId) = labelRepository.observeSystemLabels(userId)
        .flowOn(dispatcher)

    private fun observeCustomFolders(userId: UserId) = labelRepository.observeCustomFolders(userId)
        .mapLatest { list -> list.sortedBy { it.order } }
        .flowOn(dispatcher)

    private fun observeCustomLabels(userId: UserId) = labelRepository.observeCustomLabels(userId)
        .mapLatest { list -> list.sortedBy { it.order } }
        .flowOn(dispatcher)
}
