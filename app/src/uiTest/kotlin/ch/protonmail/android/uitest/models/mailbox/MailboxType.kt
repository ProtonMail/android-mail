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

package ch.protonmail.android.uitest.models.mailbox

import ch.protonmail.android.test.R
import ch.protonmail.android.uitest.util.getTestString

internal sealed class MailboxType(val name: String) {

    object Inbox : MailboxType(getTestString(R.string.test_label_title_inbox))
    object Drafts : MailboxType(getTestString(R.string.test_label_title_drafts))
    object Sent : MailboxType(getTestString(R.string.test_label_title_sent))
    object Starred : MailboxType(getTestString(R.string.test_label_title_starred))
    object Archive : MailboxType(getTestString(R.string.test_label_title_archive))
    object Spam : MailboxType(getTestString(R.string.test_label_title_spam))
    object Trash : MailboxType(getTestString(R.string.test_label_title_trash))
    object AllMail : MailboxType(getTestString(R.string.test_label_title_all_mail))

    class CustomLabel(name: String) : MailboxType(name)
    class CustomFolder(name: String) : MailboxType(name)
}
