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

package ch.protonmail.android.uitest.robot.mailbox.model.snackbar

import ch.protonmail.android.test.R
import ch.protonmail.android.uitest.models.snackbar.SnackbarEntry
import ch.protonmail.android.uitest.models.snackbar.SnackbarType
import ch.protonmail.android.uitest.util.getTestString

internal sealed class MailboxSnackbar(value: String, type: SnackbarType) : SnackbarEntry(value, type) {

    object FailedToLoadNewItems : MailboxSnackbar(
        getTestString(R.string.test_mailbox_error_message_generic), SnackbarType.Error
    )
}
