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

package ch.protonmail.android.mailmessage.data.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.AllBottomBarActions
import ch.protonmail.android.mailcommon.domain.model.AvailableActions
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.LabelAsActions
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.mailmessage.data.local.RustMessageDataSource
import ch.protonmail.android.mailmessage.data.mapper.toAllBottomBarActions
import ch.protonmail.android.mailmessage.data.mapper.toAvailableActions
import ch.protonmail.android.mailmessage.data.mapper.toLabelAsActions
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toLocalThemeOptions
import ch.protonmail.android.mailmessage.data.mapper.MoveDestinationMapper
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageThemeOptions
import ch.protonmail.android.mailmessage.domain.repository.MessageActionRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class RustMessageActionRepository @Inject constructor(
    private val rustMessageDataSource: RustMessageDataSource,
    private val moveDestinationMapper: MoveDestinationMapper
) : MessageActionRepository {

    override suspend fun getAvailableActions(
        userId: UserId,
        labelId: LabelId,
        messageId: MessageId,
        messageThemeOptions: MessageThemeOptions
    ): Either<DataError, AvailableActions> {
        val availableActions = rustMessageDataSource.getAvailableActions(
            userId,
            labelId.toLocalLabelId(),
            messageId.toLocalMessageId(),
            messageThemeOptions.toLocalThemeOptions()
        )
        return availableActions.map { it.toAvailableActions() }
    }

    override suspend fun getMoveToLocations(
        userId: UserId,
        labelId: LabelId,
        messageIds: List<MessageId>
    ): Either<DataError, List<MailLabel>> {
        val moveToActions = rustMessageDataSource.getAvailableMoveToDestinations(
            userId,
            labelId.toLocalLabelId(),
            messageIds.map { it.toLocalMessageId() }
        )

        return moveToActions.map(moveDestinationMapper::invoke)
    }

    override suspend fun getAvailableLabelAsActions(
        userId: UserId,
        labelId: LabelId,
        messageIds: List<MessageId>
    ): Either<DataError, LabelAsActions> {
        val labelAsActions = rustMessageDataSource.getAvailableLabelAsActions(
            userId,
            labelId.toLocalLabelId(),
            messageIds.map { it.toLocalMessageId() }
        )

        return labelAsActions.map { it.toLabelAsActions() }
    }

    override suspend fun getAllListBottomBarActions(
        userId: UserId,
        labelId: LabelId,
        messageIds: List<MessageId>
    ): Either<DataError, AllBottomBarActions> {
        val allActions = rustMessageDataSource.getAllAvailableListBottomBarActions(
            userId,
            labelId.toLocalLabelId(),
            messageIds.map { it.toLocalMessageId() }
        )
        return allActions.map { it.toAllBottomBarActions() }
    }

    override suspend fun getAllBottomBarActions(
        userId: UserId,
        labelId: LabelId,
        messageId: MessageId,
        messageThemeOptions: MessageThemeOptions
    ): Either<DataError, AllBottomBarActions> {
        val allActions = rustMessageDataSource.getAllAvailableBottomBarActions(
            userId,
            labelId.toLocalLabelId(),
            messageId.toLocalMessageId(),
            messageThemeOptions.toLocalThemeOptions()
        )
        return allActions.map { it.toAllBottomBarActions() }
    }
}
