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

package ch.protonmail.android.mailcomposer.presentation.model.operations

import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadataWithState
import ch.protonmail.android.mailattachments.domain.model.AttachmentState
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.presentation.mapper.AttachmentListErrorMapper
import ch.protonmail.android.mailcomposer.presentation.model.ComposerState
import ch.protonmail.android.mailcomposer.presentation.model.DraftDisplayBodyUiModel
import ch.protonmail.android.mailcomposer.presentation.model.DraftUiModel
import ch.protonmail.android.mailcomposer.presentation.model.ScheduleSendOptionsUiModel
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.AccessoriesStateModification.ScheduleSendOptionsUpdated
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.AttachmentsStateModification.ListUpdated
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.OnDraftReady
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.SendersListReady
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.UpdateLoading
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.MainStateModification.UpdateSender
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.BottomSheetEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification.SendExpirationSupportUnknownConfirmationRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification.SendExpirationUnsupportedConfirmationRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ConfirmationsEffectsStateModification.SendExpirationUnsupportedForSomeConfirmationRequested
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications.DraftBodyChanged
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.ContentEffectsStateModifications.DraftSenderChanged
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError.AttachmentRemoveFailed
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.effects.RecoverableError.AttachmentsListChangedWithError

internal sealed interface CompositeEvent : ComposerStateEvent {

    @Suppress("LongMethod")
    override fun toStateModifications(): ComposerStateModifications = when (this) {
        is DraftContentReady -> ComposerStateModifications(
            mainModification = OnDraftReady(draftUiModel, bodyShouldTakeFocus),
            effectsModification = ContentEffectsStateModifications.DraftContentReady(
                fields = draftUiModel,
                isDataRefresh = isDataRefreshed,
                forceBodyFocus = bodyShouldTakeFocus
            )
        )

        is DraftContentUpdated -> ComposerStateModifications(
            mainModification = OnDraftReady(draftUiModel, false),
            effectsModification = DraftBodyChanged(
                refreshedBody = draftUiModel.draftDisplayBodyUiModel
            )
        )

        is SenderAddressesListReady -> ComposerStateModifications(
            mainModification = SendersListReady(sendersList),
            effectsModification = BottomSheetEffectsStateModification.ShowBottomSheet
        )

        is OnSendWithEmptySubject -> ComposerStateModifications(
            mainModification = UpdateLoading(ComposerState.LoadingType.None),
            effectsModification = ConfirmationsEffectsStateModification.SendNoSubjectConfirmationRequested
        )

        is UserChangedSender -> ComposerStateModifications(
            mainModification = UpdateSender(newSender),
            effectsModification = DraftSenderChanged(refreshedBody)
        )

        is ScheduleSendOptionsReady -> ComposerStateModifications(
            accessoriesModification = ScheduleSendOptionsUpdated(options),
            effectsModification = BottomSheetEffectsStateModification.ShowBottomSheet
        )

        is AttachmentListChanged -> ComposerStateModifications(
            attachmentsModification = ListUpdated(list),
            effectsModification = list
                .filter { it.attachmentState is AttachmentState.Error }
                .takeIf { it.isNotEmpty() }
                ?.let { errored ->
                    AttachmentListErrorMapper.toRemoveAttachmentErrorOrNull(errored)?.let { removeError ->
                        AttachmentRemoveFailed(removeError)
                    } ?: AttachmentListErrorMapper.toAttachmentAddErrorWithList(errored)?.let { errorWithList ->
                        AttachmentsListChangedWithError(
                            attachmentAddErrorWithList = errorWithList
                        )
                    }
                }
        )

        is OnSendWithExpirationSupportUnknown -> ComposerStateModifications(
            mainModification = UpdateLoading(ComposerState.LoadingType.None),
            effectsModification = SendExpirationSupportUnknownConfirmationRequested
        )

        is OnSendWithExpirationUnsupportedForSome -> ComposerStateModifications(
            mainModification = UpdateLoading(ComposerState.LoadingType.None),
            effectsModification = SendExpirationUnsupportedForSomeConfirmationRequested(
                recipients
            )
        )

        OnSendWithExpirationUnsupported -> ComposerStateModifications(
            mainModification = UpdateLoading(ComposerState.LoadingType.None),
            effectsModification = SendExpirationUnsupportedConfirmationRequested
        )
    }

    data class DraftContentReady(
        val draftUiModel: DraftUiModel,
        val isDataRefreshed: Boolean,
        val bodyShouldTakeFocus: Boolean
    ) : CompositeEvent

    data class DraftContentUpdated(
        val draftUiModel: DraftUiModel,
        val shouldForceReload: Boolean
    ) : CompositeEvent

    data class SenderAddressesListReady(val sendersList: List<SenderUiModel>) : CompositeEvent
    data class UserChangedSender(
        val newSender: SenderEmail,
        val refreshedBody: DraftDisplayBodyUiModel
    ) : CompositeEvent

    data class ScheduleSendOptionsReady(val options: ScheduleSendOptionsUiModel) : CompositeEvent

    data object OnSendWithEmptySubject : CompositeEvent

    data class AttachmentListChanged(val list: List<AttachmentMetadataWithState>) : CompositeEvent

    data class OnSendWithExpirationUnsupportedForSome(val recipients: List<String>) : CompositeEvent

    data object OnSendWithExpirationSupportUnknown : CompositeEvent

    data object OnSendWithExpirationUnsupported : CompositeEvent
}
