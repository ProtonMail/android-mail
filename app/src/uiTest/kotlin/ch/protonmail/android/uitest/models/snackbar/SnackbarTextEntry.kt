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

package ch.protonmail.android.uitest.models.snackbar

import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

internal sealed class SnackbarTextEntry(val value: String) {
    object FeatureComingSoon : SnackbarTextEntry(
        getTestString(testR.string.test_feature_coming_soon)
    )

    class ConversationMovedToFolder(folder: String) : SnackbarTextEntry(
        getTestString(testR.string.test_conversation_moved_to_selected_destination, folder)
    )

    object FailedToGetAttachment : SnackbarTextEntry(
        getTestString(testR.string.error_get_attachment_failed)
    )

    object MultipleDownloadsWarning : SnackbarTextEntry(
        getTestString(testR.string.test_error_attachment_download_in_progress)
    )

    object FailedToDecryptMessage : SnackbarTextEntry(
        getTestString(testR.string.test_decryption_error)
    )

    object FailedToLoadMessage : SnackbarTextEntry(
        getTestString(testR.string.test_detail_error_retrieving_message_body)
    )

    object FailedToLoadNewItems : SnackbarTextEntry(
        getTestString(testR.string.test_mailbox_error_message_generic)
    )

    object DraftSaved : SnackbarTextEntry(
        getTestString(testR.string.test_mailbox_draft_saved)
    )

    object DraftOutOfSync : SnackbarTextEntry(
        getTestString(testR.string.test_composer_error_loading_draft)
    )

    class DuplicateEmailAddress(recipient: String) : SnackbarTextEntry(
        getTestString(testR.string.test_composer_error_duplicate_email, recipient)
    )

    object InvalidEmailAddress : SnackbarTextEntry(
        getTestString(testR.string.test_composer_error_invalid_email)
    )

    object UpgradePlanToChangeSender : SnackbarTextEntry(
        getTestString(testR.string.test_composer_change_sender_paid_feature)
    )

    object SendingMessage : SnackbarTextEntry(
        getTestString(testR.string.test_mailbox_message_sending)
    )

    object MessageSent : SnackbarTextEntry(
        getTestString(testR.string.test_mailbox_message_sending_success)
    )
}
