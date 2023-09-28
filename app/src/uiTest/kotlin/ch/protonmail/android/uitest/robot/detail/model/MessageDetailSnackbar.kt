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

package ch.protonmail.android.uitest.robot.detail.model

import ch.protonmail.android.test.R
import ch.protonmail.android.uitest.models.snackbar.SnackbarEntry
import ch.protonmail.android.uitest.models.snackbar.SnackbarType
import ch.protonmail.android.uitest.util.getTestString

internal sealed class MessageDetailSnackbar(value: String, type: SnackbarType) : SnackbarEntry(value, type) {

    class ConversationMovedToFolder(folder: String) : MessageDetailSnackbar(
        getTestString(R.string.test_conversation_moved_to_selected_destination, folder), SnackbarType.Normal
    )

    object FailedToDecryptMessage : MessageDetailSnackbar(
        getTestString(R.string.test_decryption_error), SnackbarType.Default
    )

    object FailedToGetAttachment : MessageDetailSnackbar(
        getTestString(R.string.error_get_attachment_failed), SnackbarType.Default
    )

    object FailedToLoadMessage : MessageDetailSnackbar(
        getTestString(R.string.test_detail_error_retrieving_message_body), SnackbarType.Default
    )

    object MultipleDownloadsWarning : MessageDetailSnackbar(
        getTestString(R.string.test_error_attachment_download_in_progress), SnackbarType.Default
    )
}
