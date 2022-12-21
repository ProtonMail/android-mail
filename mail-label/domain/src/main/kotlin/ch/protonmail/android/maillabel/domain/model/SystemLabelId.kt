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

package ch.protonmail.android.maillabel.domain.model

import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.AllDrafts
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.AllMail
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.AllSent
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Archive
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Drafts
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Inbox
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Outbox
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Sent
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Spam
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Starred
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Trash
import me.proton.core.label.domain.entity.LabelId

enum class SystemLabelId(val labelId: LabelId) {
    /** Displayed. */
    Inbox(LabelId("0")),

    /**
     * Not displayed.
     * All the draft mails have this label.
     * This is necessary because a Draft could have been moved into another folder than `Drafts`.
     */
    AllDrafts(LabelId("1")),

    /**
     * Not displayed.
     * All the sent mails have this label.
     * This is necessary because a Sent message could have been moved into another folder than `Sent.
     */
    AllSent(LabelId("2")),

    /** Displayed. */
    Trash(LabelId("3")),

    /** Displayed. */
    Spam(LabelId("4")),

    /** Displayed. */
    AllMail(LabelId("5")),

    /** Displayed. */
    Archive(LabelId("6")),

    /** Displayed. */
    Sent(LabelId("7")),

    /** Displayed. */
    Drafts(LabelId("8")),

    /** Displayed. */
    Outbox(LabelId("9")),

    /** Displayed. */
    Starred(LabelId("10"));

    companion object {

        private val map = values().associateBy { stringOf(it) }

        val displayedList = listOf(Inbox, Drafts, Sent, Starred, Archive, Spam, Trash, AllMail)

        val exclusiveDestinationList = listOf(Inbox, Archive, Spam, Trash)

        val exclusiveList = exclusiveDestinationList + Drafts + Sent

        fun stringOf(value: SystemLabelId): String = value.labelId.id
        fun enumOf(value: String?): SystemLabelId = map[value] ?: Inbox
    }
}

fun SystemLabelId.toMailLabelSystem(): MailLabel.System = when (this) {
    SystemLabelId.Inbox -> MailLabel.System(Inbox)
    SystemLabelId.AllDrafts -> MailLabel.System(AllDrafts)
    SystemLabelId.AllSent -> MailLabel.System(AllSent)
    SystemLabelId.Trash -> MailLabel.System(Trash)
    SystemLabelId.Spam -> MailLabel.System(Spam)
    SystemLabelId.AllMail -> MailLabel.System(AllMail)
    SystemLabelId.Archive -> MailLabel.System(Archive)
    SystemLabelId.Sent -> MailLabel.System(Sent)
    SystemLabelId.Drafts -> MailLabel.System(Drafts)
    SystemLabelId.Outbox -> MailLabel.System(Outbox)
    SystemLabelId.Starred -> MailLabel.System(Starred)
}
