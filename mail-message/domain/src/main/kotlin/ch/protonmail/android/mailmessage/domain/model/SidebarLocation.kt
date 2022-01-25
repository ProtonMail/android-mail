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
sealed class SidebarLocation {

    sealed class MailLocation : SidebarLocation()

    object Inbox : MailLocation()
    object Drafts : MailLocation()
    object Sent : MailLocation()
    object Starred : MailLocation()
    object Archive : MailLocation()
    object Spam : MailLocation()
    object Trash : MailLocation()
    object AllMail : MailLocation()

    data class CustomLabel(val id: LabelId) : SidebarLocation()

    data class CustomFolder(val id: LabelId) : SidebarLocation()
}
