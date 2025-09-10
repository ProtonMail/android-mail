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

package ch.protonmail.android.mailconversation.data.repository

import arrow.core.Either
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.AllBottomBarActions
import ch.protonmail.android.mailcommon.domain.model.AvailableActions
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.local.RustConversationDataSource
import ch.protonmail.android.mailconversation.data.mapper.toAvailableActions
import ch.protonmail.android.mailconversation.domain.repository.ConversationActionRepository
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.LabelAsActions
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.mailmessage.data.mapper.toAllBottomBarActions
import ch.protonmail.android.mailmessage.data.mapper.toLabelAsActions
import ch.protonmail.android.mailmessage.data.mapper.toLocalConversationId
import ch.protonmail.android.mailmessage.data.mapper.toMailLabels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class RustConversationActionRepository @Inject constructor(
    private val rustConversationDataSource: RustConversationDataSource
) : ConversationActionRepository {

    override suspend fun getAvailableActions(
        userId: UserId,
        labelId: LabelId,
        conversationId: ConversationId
    ): Either<DataError, AvailableActions> {
        val availableActions = rustConversationDataSource.getAvailableBottomSheetActions(
            userId,
            labelId.toLocalLabelId(),
            conversationId.toLocalConversationId()
        )

        return availableActions.map { it.toAvailableActions() }
    }

    override suspend fun getSystemMoveToLocations(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, List<MailLabel.System>> {
        val moveToActions = rustConversationDataSource.getAvailableSystemMoveToActions(
            userId,
            labelId.toLocalLabelId(),
            conversationIds.map { it.toLocalConversationId() }
        )

        return moveToActions.map { it.toMailLabels() }
    }

    override suspend fun getAvailableLabelAsActions(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, LabelAsActions> {
        val labelAsActions = rustConversationDataSource.getAvailableLabelAsActions(
            userId,
            labelId.toLocalLabelId(),
            conversationIds.map { it.toLocalConversationId() }
        )

        return labelAsActions.map { it.toLabelAsActions() }
    }

    override suspend fun getAllListBottomBarActions(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, AllBottomBarActions> {
        val allActions = rustConversationDataSource.getAllAvailableListBottomBarActions(
            userId,
            labelId.toLocalLabelId(),
            conversationIds.map { it.toLocalConversationId() }
        )

        return allActions.map { it.toAllBottomBarActions() }
    }

    override suspend fun observeAllBottomBarActions(
        userId: UserId,
        labelId: LabelId,
        conversationId: ConversationId
    ): Flow<Either<DataError, AllBottomBarActions>> {
        return rustConversationDataSource.observeConversation(
            userId = userId,
            conversationId = conversationId.toLocalConversationId(),
            labelId = labelId.toLocalLabelId()
        ).mapLatest { conversationResult ->
            conversationResult.fold(
                ifLeft = { error ->
                    Timber.w("Failed to observe bottomBar actions due to conversation error $error")
                    DataError.Local.NoDataCached.left()
                },
                ifRight = { conversation ->
                    val allActions = rustConversationDataSource.getAllAvailableBottomBarActions(
                        userId,
                        labelId.toLocalLabelId(),
                        conversationId.toLocalConversationId()
                    )

                    allActions.map { it.toAllBottomBarActions() }
                }
            )
        }.distinctUntilChanged()
    }
}
