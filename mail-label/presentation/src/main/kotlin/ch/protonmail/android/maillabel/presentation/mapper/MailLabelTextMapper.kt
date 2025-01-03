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

package ch.protonmail.android.maillabel.presentation.mapper

import android.content.Context
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MailLabelTextMapper @Inject constructor(@ApplicationContext val context: Context) {

    fun mapToString(mailLabelText: MailLabelText): String {
        return when (mailLabelText) {
            is MailLabelText.TextString -> mailLabelText.string
            is MailLabelText.TextRes -> context.getString(mailLabelText.resource)
        }
    }
}
