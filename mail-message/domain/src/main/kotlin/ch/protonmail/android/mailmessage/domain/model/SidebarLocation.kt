/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.domain.model

import me.proton.core.label.domain.entity.LabelId

/**
 * Represents the currently selected item of the sidebar.
 * Locations that navigate away from the sidebar when selected (eg. settings, subscriptions, logout)
 * are not represented here as they do not need changing the sidebar's selected item
 */
sealed class SidebarLocation(open val labelId: LabelId) {

    sealed class MailLocation(override val labelId: LabelId) : SidebarLocation(labelId)

    object Inbox : MailLocation(LabelId("0"))
    object Drafts : MailLocation(LabelId("8"))
    object Sent : MailLocation(LabelId("7"))
    object Starred : MailLocation(LabelId("10"))
    object Archive : MailLocation(LabelId("6"))
    object Spam : MailLocation(LabelId("4"))
    object Trash : MailLocation(LabelId("3"))
    object AllMail : MailLocation(LabelId("5"))

    data class CustomLabel(val id: LabelId) : SidebarLocation(LabelId(id.id))

    data class CustomFolder(val id: LabelId) : SidebarLocation(LabelId(id.id))
}
