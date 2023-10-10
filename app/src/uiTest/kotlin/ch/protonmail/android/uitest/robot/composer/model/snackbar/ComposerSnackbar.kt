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

package ch.protonmail.android.uitest.robot.composer.model.snackbar

import ch.protonmail.android.test.R
import ch.protonmail.android.uitest.models.snackbar.SnackbarEntry
import ch.protonmail.android.uitest.models.snackbar.SnackbarType
import ch.protonmail.android.uitest.util.getTestString

internal sealed class ComposerSnackbar(value: String, type: SnackbarType) : SnackbarEntry(value, type) {

    object AttachmentUploadError : ComposerSnackbar(
        getTestString(R.string.test_mailbox_attachment_uploading_error), SnackbarType.Error
    )

    object DraftSaved : ComposerSnackbar(
        getTestString(R.string.test_mailbox_draft_saved), SnackbarType.Success
    )

    object DraftOutOfSync : ComposerSnackbar(
        getTestString(R.string.test_composer_error_loading_draft), SnackbarType.Default
    )

    class DuplicateEmailAddress(recipient: String) : ComposerSnackbar(
        getTestString(R.string.test_composer_error_duplicate_email, recipient), SnackbarType.Default
    )

    object InvalidEmailAddress : ComposerSnackbar(
        getTestString(R.string.test_composer_error_invalid_email), SnackbarType.Default
    )

    object MessageSent : ComposerSnackbar(
        getTestString(R.string.test_mailbox_message_sending_success), SnackbarType.Success
    )

    object MessageSentError : ComposerSnackbar(
        getTestString(R.string.test_mailbox_message_sending_error), SnackbarType.Error
    )

    object MessageQueued : ComposerSnackbar(
        getTestString(R.string.test_mailbox_message_sending_offline), SnackbarType.Normal
    )

    object SendingMessage : ComposerSnackbar(
        getTestString(R.string.test_mailbox_message_sending), SnackbarType.Normal
    )

    object UpgradePlanToChangeSender : ComposerSnackbar(
        getTestString(R.string.test_composer_change_sender_paid_feature), SnackbarType.Default
    )
}
