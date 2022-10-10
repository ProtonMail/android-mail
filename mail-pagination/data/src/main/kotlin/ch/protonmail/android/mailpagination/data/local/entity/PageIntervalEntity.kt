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

package ch.protonmail.android.mailpagination.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.data.entity.UserEntity

@Entity(
    primaryKeys = ["userId", "type", "orderBy", "labelId", "keyword", "read", "minValue", "maxValue"],
    indices = [
        Index("userId"),
        Index("type"),
        Index("minValue"),
        Index("maxValue"),
        Index("minOrder"),
        Index("maxOrder")
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PageIntervalEntity(
    val userId: UserId,
    val type: PageItemType,
    val orderBy: OrderBy,
    val labelId: LabelId,
    val keyword: String,
    val read: ReadStatus,
    val minValue: Long,
    val maxValue: Long,
    val minOrder: Long,
    val maxOrder: Long,
    val minId: String?,
    val maxId: String?
)
