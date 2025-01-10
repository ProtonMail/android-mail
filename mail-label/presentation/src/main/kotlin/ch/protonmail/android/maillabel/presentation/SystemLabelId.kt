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

package ch.protonmail.android.maillabel.presentation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ch.protonmail.android.maillabel.domain.model.SystemLabelId

@StringRes
@Suppress("ComplexMethod")
fun SystemLabelId.textRes() = when (this) {
    SystemLabelId.Inbox -> R.string.label_title_inbox
    SystemLabelId.AllDrafts -> R.string.label_title_drafts
    SystemLabelId.AllSent -> R.string.label_title_sent
    SystemLabelId.Trash -> R.string.label_title_trash
    SystemLabelId.Spam -> R.string.label_title_spam
    SystemLabelId.AllMail -> R.string.label_title_all_mail
    SystemLabelId.Archive -> R.string.label_title_archive
    SystemLabelId.Sent -> R.string.label_title_sent
    SystemLabelId.Drafts -> R.string.label_title_drafts
    SystemLabelId.Outbox -> R.string.label_title_outbox
    SystemLabelId.Starred -> R.string.label_title_starred
    SystemLabelId.AlmostAllMail -> R.string.label_title_all_mail
    SystemLabelId.AllScheduled -> R.string.label_title_all_scheduled
    SystemLabelId.Snoozed -> R.string.label_title_snoozed
}

@DrawableRes
@Suppress("ComplexMethod")
fun SystemLabelId.iconRes() = when (this) {
    SystemLabelId.Inbox -> R.drawable.ic_proton_inbox
    SystemLabelId.AllDrafts -> R.drawable.ic_proton_file_lines
    SystemLabelId.AllSent -> R.drawable.ic_proton_paper_plane
    SystemLabelId.Trash -> R.drawable.ic_proton_trash
    SystemLabelId.Spam -> R.drawable.ic_proton_fire
    SystemLabelId.AllMail -> R.drawable.ic_proton_envelopes
    SystemLabelId.Archive -> R.drawable.ic_proton_archive_box
    SystemLabelId.Sent -> R.drawable.ic_proton_paper_plane
    SystemLabelId.Drafts -> R.drawable.ic_proton_file_lines
    SystemLabelId.Outbox -> R.drawable.ic_proton_inbox
    SystemLabelId.Starred -> R.drawable.ic_proton_star
    SystemLabelId.AlmostAllMail -> R.drawable.ic_proton_envelopes
    SystemLabelId.AllScheduled -> R.drawable.ic_proton_inbox
    SystemLabelId.Snoozed -> R.drawable.ic_proton_inbox
}
