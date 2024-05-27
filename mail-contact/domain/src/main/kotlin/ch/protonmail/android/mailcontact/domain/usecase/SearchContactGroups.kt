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

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcontact.domain.model.ContactGroup
import ch.protonmail.android.mailcontact.domain.model.GetContactError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.transformLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.util.kotlin.containsNoCase
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject

class SearchContactGroups @Inject constructor(
    private val observeContactGroupLabels: ObserveContactGroupLabels,
    private val observeContactGroup: ObserveContactGroup
) {

    /**
     * @param returnEmpty if we should return matched Contact Groups that have no Members
     */
    operator fun invoke(
        userId: UserId,
        query: String,
        returnEmpty: Boolean = false
    ): Flow<Either<GetContactError, List<ContactGroup>>> =
        observeContactGroupLabels(userId).distinctUntilChanged().transformLatest {
            it.onLeft {
                Timber.e("SearchContactGroups, error observing contacts group labels: $it")
                emit(GetContactError.left())
            }.onRight { contactGroupLabels ->
                query.trim().takeIfNotBlank()?.run {
                    val labelsMatchingQuery = search(contactGroupLabels, query)

                    val contactGroups = labelsMatchingQuery.map {
                        observeContactGroup(userId, it.labelId).firstOrNull()?.getOrNull()
                    }

                    if (contactGroups.any { it == null }) {
                        Timber.e("SearchContactGroups, error observing contact group: $it")
                        emit(GetContactError.left())
                    } else {
                        emit(
                            contactGroups
                                .filterNotNull()
                                .filter {
                                    if (!returnEmpty) it.members.isNotEmpty() else true
                                }
                                .right()
                        )
                    }
                }
            }
        }.distinctUntilChanged()

    private fun search(contactGroupLabels: List<Label>, query: String): List<Label> =
        contactGroupLabels.filter { label ->
            label.name.containsNoCase(query)
        }
}
