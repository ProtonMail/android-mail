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

package ch.protonmail.android.mailcontact.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalContactId
import ch.protonmail.android.mailcommon.data.mapper.LocalGroupedContacts
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontact.data.ContactRustCoroutineScope
import ch.protonmail.android.mailcontact.data.mapper.ContactSuggestionsMapper
import ch.protonmail.android.mailcontact.data.mapper.DeviceContactsWithSignatureMapper
import ch.protonmail.android.mailcontact.data.mapper.GroupedContactsMapper
import ch.protonmail.android.mailcontact.data.mapper.toContactDetailCard
import ch.protonmail.android.mailcontact.data.usecase.CreateRustContactWatcher
import ch.protonmail.android.mailcontact.data.usecase.RustDeleteContact
import ch.protonmail.android.mailcontact.data.usecase.RustGetContactDetails
import ch.protonmail.android.mailcontact.data.usecase.RustGetContactSuggestions
import ch.protonmail.android.mailcontact.domain.model.ContactDetailCard
import ch.protonmail.android.mailcontact.domain.model.ContactMetadata
import ch.protonmail.android.mailcontact.domain.model.ContactSuggestionQuery
import ch.protonmail.android.mailcontact.domain.model.DeviceContactsWithSignature
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import ch.protonmail.android.mailcontact.domain.model.GroupedContacts
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.ContactsLiveQueryCallback
import uniffi.mail_uniffi.VoidActionResult
import uniffi.mail_uniffi.WatchedContactList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RustContactDataSourceImpl @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val groupedContactsMapper: GroupedContactsMapper,
    private val contactSuggestionMapper: ContactSuggestionsMapper,
    private val deviceContactsMapper: DeviceContactsWithSignatureMapper,
    private val createRustContactWatcher: CreateRustContactWatcher,
    private val rustDeleteContact: RustDeleteContact,
    private val rustGetContactSuggestions: RustGetContactSuggestions,
    private val rustGetContactDetails: RustGetContactDetails,
    @ContactRustCoroutineScope private val coroutineScope: CoroutineScope
) : RustContactDataSource {

    private val mutex = Mutex()

    private data class WatcherState(
        val contacts: MutableStateFlow<Either<GetContactError, List<GroupedContacts>>>,
        val contactListWatcher: WatchedContactList
    )

    private val watchers = mutableMapOf<UserId, WatcherState>()

    override suspend fun getContactSuggestions(
        userId: UserId,
        deviceContacts: DeviceContactsWithSignature,
        query: ContactSuggestionQuery
    ): Either<DataError, List<ContactMetadata>> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-contact: trying to get contact suggestions with a null session")
            return DataError.Local.NoUserSession.left()
        }

        val localDeviceContacts = deviceContactsMapper.toLocalDeviceContact(deviceContacts)
        return rustGetContactSuggestions(userId, session, localDeviceContacts, query.value)
            .map { contactSuggestionMapper.toContactSuggestions(it) }
    }

    override suspend fun preloadContactSuggestions(
        userId: UserId,
        deviceContacts: DeviceContactsWithSignature
    ): Either<DataError, Unit> {
        val session = userSessionRepository.getUserSession(userId)
            ?: return DataError.Local.NoUserSession.left().also {
                Timber.e("rust-contact: preload with a null session")
            }

        val local = deviceContactsMapper.toLocalDeviceContact(deviceContacts)
        return rustGetContactSuggestions.preload(userId, session, local)
    }

    override suspend fun observeAllContacts(userId: UserId): Flow<Either<GetContactError, List<ContactMetadata>>> {
        return observeAllGroupedContacts(userId).transformLatest {
            it.onRight { groupedContactsList ->
                val contactMetadataList = mutableListOf<ContactMetadata>()

                groupedContactsList.map { groupedContacts ->
                    contactMetadataList.addAll(groupedContacts.contacts)
                }

                emit(contactMetadataList.right())
            }
            it.onLeft {
                emit(GetContactError.left())
            }
        }
    }

    override suspend fun observeAllGroupedContacts(
        userId: UserId
    ): Flow<Either<GetContactError, List<GroupedContacts>>> {
        return when (val res = getOrCreateWatcher(userId)) {
            is Either.Left -> flowOf(GetContactError.left())
            is Either.Right -> res.value.contacts
        }
    }

    override suspend fun deleteContact(userId: UserId, contactId: LocalContactId): Either<DataError, Unit> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-contact: trying to load message with a null session")
            return DataError.Local.NoUserSession.left()
        }

        return when (val result = rustDeleteContact(session, contactId)) {
            is VoidActionResult.Error -> {
                Timber.e("rust-contact: Failed to delete contact")
                return result.v1.toDataError().left()
            }

            VoidActionResult.Ok -> Unit.right()
        }
    }

    override suspend fun getContactDetails(
        userId: UserId,
        contactId: LocalContactId
    ): Either<DataError, ContactDetailCard> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-contact: trying to load contact details with a null session")
            return DataError.Local.NoUserSession.left()
        }

        return rustGetContactDetails(session, contactId)
            .onLeft { Timber.e("rust-contact: getting contact details failed") }
            .map { it.toContactDetailCard() }
    }

    private suspend fun getOrCreateWatcher(userId: UserId): Either<DataError, WatcherState> {
        watchers[userId]?.let { return it.right() }
        return createWatcher(userId)
    }

    private suspend fun createWatcher(userId: UserId): Either<DataError, WatcherState> = mutex.withLock {
        watchers[userId]?.right() ?: run {
            val session = userSessionRepository.getUserSession(userId)
                ?: return DataError.Local.NoUserSession.left()

            val groupedContactsFlow = MutableStateFlow<Either<GetContactError, List<GroupedContacts>>>(
                GetContactError.left()
            )

            val callback = object : ContactsLiveQueryCallback {
                override fun onUpdate(contacts: List<LocalGroupedContacts>) {
                    coroutineScope.launch {
                        groupedContactsFlow.value = contacts.map(groupedContactsMapper::toGroupedContacts).right()
                        Timber.d("rust-contact-data-source: contact list updated (${contacts.size} groups)")
                    }
                }
            }

            createRustContactWatcher(session, callback)
                .mapLeft { error ->
                    Timber.e("rust-contact-data-source: failed creating contact watcher $error")
                    error
                }
                .map { watcher ->
                    groupedContactsFlow.value = watcher.contactList
                        .map(groupedContactsMapper::toGroupedContacts)
                        .right()

                    Timber.d("rust-contact-data-source: watcher created")

                    WatcherState(groupedContactsFlow, watcher).also { watchers[userId] = it }
                }
        }
    }
}
