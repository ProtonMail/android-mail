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

package ch.protonmail.android.mailcontact.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontact.data.model.LocalDeviceContactsWithSignature
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.ContactSuggestion
import uniffi.mail_uniffi.ContactSuggestions
import uniffi.mail_uniffi.ContactSuggestionsResult
import javax.inject.Inject

class RustGetContactSuggestions @Inject constructor(
    private val contactSuggestions: RustContactSuggestions
) {

    private val cacheMutex = Mutex()

    private val cachedByUser: MutableMap<UserId, CacheEntry> = mutableMapOf()

    private data class CacheEntry(
        val suggestions: ContactSuggestions,
        val signature: Long
    )

    suspend operator fun invoke(
        userId: UserId,
        mailUserSession: MailUserSessionWrapper,
        deviceContacts: LocalDeviceContactsWithSignature,
        query: String
    ): Either<DataError, List<ContactSuggestion>> {

        // Serve from cache only if we have cached contacts and device contacts signature matches
        val cached = cacheMutex.withLock { cachedByUser[userId] }
        if (cached != null && cached.signature == deviceContacts.signature) {
            return cached.suggestions.filtered(query).right()
        }

        // signature changed or not cached yet, fetch fresh suggestions
        return when (
            val result = contactSuggestions(
                deviceContacts.contacts,
                mailUserSession.getRustUserSession()
            )
        ) {
            is ContactSuggestionsResult.Error -> result.v1.toDataError().left()
            is ContactSuggestionsResult.Ok -> {
                val freshContactSuggestions = result.v1

                cacheMutex.withLock {
                    Timber.d("contact-suggestions: Caching fresh contact suggestions")
                    cachedByUser[userId] = CacheEntry(freshContactSuggestions, deviceContacts.signature)
                }
                freshContactSuggestions.filtered(query).right()
            }
        }
    }

    suspend fun preload(
        userId: UserId,
        mailUserSession: MailUserSessionWrapper,
        deviceContacts: LocalDeviceContactsWithSignature
    ): Either<DataError, Unit> {

        return when (val res = contactSuggestions(deviceContacts.contacts, mailUserSession.getRustUserSession())) {
            is ContactSuggestionsResult.Error -> res.v1.toDataError().left()
            is ContactSuggestionsResult.Ok -> {
                cacheMutex.withLock {
                    cachedByUser[userId] = CacheEntry(res.v1, deviceContacts.signature)
                }
                Timber.d("contact-suggestions: Preloaded contact suggestions")
                Unit.right()
            }
        }
    }
}
