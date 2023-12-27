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

import me.proton.core.label.domain.entity.LabelId

sealed class MailLabelId(
    open val labelId: LabelId
) {

    sealed class System(
        val systemLabelId: SystemLabelId
    ) : MailLabelId(systemLabelId.labelId) {

        object Inbox : System(SystemLabelId.Inbox)
        object AllDrafts : System(SystemLabelId.AllDrafts)
        object AllSent : System(SystemLabelId.AllSent)
        object Trash : System(SystemLabelId.Trash)
        object Spam : System(SystemLabelId.Spam)
        object AllMail : System(SystemLabelId.AllMail)
        object Archive : System(SystemLabelId.Archive)
        object Sent : System(SystemLabelId.Sent)
        object Drafts : System(SystemLabelId.Drafts)
        object Outbox : System(SystemLabelId.Outbox)
        object Starred : System(SystemLabelId.Starred)
        object AllScheduled : System(SystemLabelId.AllScheduled)
        object AlmostAllMail : System(SystemLabelId.AlmostAllMail)
        object Snoozed : System(SystemLabelId.Snoozed)

        fun toMailLabel() = MailLabel.System(id = this)
    }

    sealed class Custom(
        override val labelId: LabelId
    ) : MailLabelId(labelId) {

        data class Label(
            override val labelId: LabelId
        ) : Custom(labelId)

        data class Folder(
            override val labelId: LabelId
        ) : Custom(labelId)
    }
}

sealed class MailLabel(
    open val id: MailLabelId
) {

    data class System(
        override val id: MailLabelId.System
    ) : MailLabel(id)

    data class Custom(
        override val id: MailLabelId.Custom,
        val parent: Custom?,
        val text: String,
        val color: Int,
        val isExpanded: Boolean,
        val level: Int,
        val order: Int,
        val children: List<MailLabelId.Custom>
    ) : MailLabel(id)
}

data class MailLabels(
    val systemLabels: List<MailLabel.System>,
    val folders: List<MailLabel.Custom>,
    val labels: List<MailLabel.Custom>
) {

    val allById = (systemLabels + folders + labels).associateBy { item -> item.id }

    companion object {

        val Initial = MailLabels(
            systemLabels = emptyList(),
            folders = emptyList(),
            labels = emptyList()
        )
    }
}
