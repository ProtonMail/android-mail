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

package ch.protonmail.android.mailcontact.domain.usecase

import android.content.Context
import android.provider.ContactsContract
import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcontact.domain.model.DeviceContact
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class SearchDeviceContacts @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(query: String): Either<GetContactError, List<DeviceContact>> {

        val contentResolver = context.contentResolver

        val selectionArgs = arrayOf("%$query%", "%$query%", "%$query%")

        @Suppress("SwallowedException")
        val contactEmails = try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                ANDROID_PROJECTION,
                ANDROID_SELECTION,
                selectionArgs,
                ANDROID_ORDER_BY
            )
        } catch (e: SecurityException) {
            Timber.d("SearchDeviceContacts: contact permission is not granted")
            null
        }

        val deviceContacts = mutableListOf<DeviceContact>()

        val displayNameColumnIndex = contactEmails?.getColumnIndex(
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY
        )?.takeIf {
            it >= 0
        } ?: 0

        val emailColumnIndex = contactEmails?.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)?.takeIf {
            it >= 0
        } ?: 0

        contactEmails?.use { cursor ->
            for (position in 0 until cursor.count) {
                cursor.moveToPosition(position)
                deviceContacts.add(
                    DeviceContact(
                        name = contactEmails.getString(displayNameColumnIndex),
                        email = contactEmails.getString(emailColumnIndex)
                    )
                )
            }
        }

        return deviceContacts.right()
    }

    companion object {

        private const val ANDROID_ORDER_BY = ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY + " ASC"

        @Suppress("MaxLineLength")
        private const val ANDROID_SELECTION = "${ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY} LIKE ? OR ${ContactsContract.CommonDataKinds.Email.ADDRESS} LIKE ? OR ${ContactsContract.CommonDataKinds.Email.DATA} LIKE ?"

        private val ANDROID_PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.DATA
        )
    }
}
