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

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import javax.inject.Inject

class ObserveLabels @Inject constructor(
    private val labelRepository: LabelRepository
) {

    operator fun invoke(userId: UserId, labelType: LabelType): Flow<Either<DataError, List<Label>>> =
        labelRepository.observeLabels(userId, labelType)
            .mapToEither()
            .map { either ->
                either.map {
                    if (labelType == LabelType.MessageFolder) {
                        it.filterDeletedSubfolders()
                    } else {
                        it
                    }
                }
            }

    private fun List<Label>.filterDeletedSubfolders(): List<Label> {
        val mapping = associateBy { it.labelId }
        return filter {
            val topParentId = it.findTopParentId(mapping)
            topParentId == null || mapping[topParentId] != null
        }
    }
}

fun Label.findTopParentId(mapping: Map<LabelId, Label>): LabelId? {
    var currentParentId = parentId
    while (currentParentId != null) {
        val nextParent = mapping[currentParentId] ?: return currentParentId
        currentParentId = nextParent.parentId
    }
    return null
}
