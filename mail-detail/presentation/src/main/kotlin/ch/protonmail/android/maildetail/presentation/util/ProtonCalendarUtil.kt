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

package ch.protonmail.android.maildetail.presentation.util

import android.content.Intent
import android.net.Uri
import ch.protonmail.android.maildetail.domain.usecase.IsProtonCalendarInstalled.Companion.PROTON_CALENDAR_PACKAGE_NAME

object ProtonCalendarUtil {

    private const val ACTION_OPEN_ICS = "me.proton.android.calendar.intent.action.CTA_OPEN_ICS"
    private const val EXTRA_SENDER = "me.proton.android.calendar.intent.extra.ICS_SENDER_EMAIL"
    private const val EXTRA_RECIPIENT = "me.proton.android.calendar.intent.extra.ICS_RECIPIENT_EMAIL"
    private const val CALENDAR_MIME_TYPE = "text/calendar"

    fun getIntentToOpenIcsInProtonCalendar(
        uri: Uri,
        sender: String,
        recipient: String
    ) = Intent(ACTION_OPEN_ICS).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        setDataAndType(uri, CALENDAR_MIME_TYPE)
        setPackage(PROTON_CALENDAR_PACKAGE_NAME)
        putExtra(EXTRA_SENDER, sender)
        putExtra(EXTRA_RECIPIENT, recipient)
    }

    fun getIntentToProtonCalendarOnPlayStore() = Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        data = Uri.parse("market://details?id=$PROTON_CALENDAR_PACKAGE_NAME")
    }
}
