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

package ch.protonmail.android.mailcontact.data

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcontact.data.local.ContactGroupLocalDataSource
import ch.protonmail.android.mailcontact.data.remote.ContactGroupRemoteDataSource
import ch.protonmail.android.mailcontact.domain.repository.ContactGroupRepository
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class ContactGroupRepositoryImpl @Inject constructor(
    private val contactGroupLocalDataSource: ContactGroupLocalDataSource,
    private val contactGroupRemoteDataSource: ContactGroupRemoteDataSource
) : ContactGroupRepository {

    override suspend fun addContactEmailIdsToContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactEmailIds: Set<ContactEmailId>
    ): Either<ContactGroupRepository.ContactGroupErrors, Unit> {

        contactGroupLocalDataSource.addContactEmailIdsToContactGroup(
            userId,
            labelId,
            contactEmailIds
        )

        Either.catch {
            contactGroupRemoteDataSource.addContactEmailIdsToContactGroup(
                userId,
                labelId,
                contactEmailIds
            )
        }.onLeft { return ContactGroupRepository.ContactGroupErrors.RemoteDataSourceError.left() }

        return Unit.right()
    }

    override suspend fun removeContactEmailIdsFromContactGroup(
        userId: UserId,
        labelId: LabelId,
        contactEmailIds: Set<ContactEmailId>
    ): Either<ContactGroupRepository.ContactGroupErrors, Unit> {

        contactGroupLocalDataSource.removeContactEmailIdsFromContactGroup(
            userId,
            labelId,
            contactEmailIds
        )

        Either.catch {
            contactGroupRemoteDataSource.removeContactEmailIdsFromContactGroup(
                userId,
                labelId,
                contactEmailIds
            )
        }.onLeft { return ContactGroupRepository.ContactGroupErrors.RemoteDataSourceError.left() }

        return Unit.right()
    }

}
