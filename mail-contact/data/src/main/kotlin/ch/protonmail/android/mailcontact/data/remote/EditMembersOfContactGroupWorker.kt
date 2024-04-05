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

package ch.protonmail.android.mailcontact.data.remote

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcontact.data.local.ContactGroupLocalDataSource
import ch.protonmail.android.mailcontact.data.remote.resource.LabelContactEmailsBody
import ch.protonmail.android.mailcontact.data.remote.resource.UnlabelContactEmailsBody
import ch.protonmail.android.mailcontact.data.remote.response.filterUnsuccessful
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.contact.domain.entity.ContactEmailId
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiManager

@HiltWorker
class EditMembersOfContactGroupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val apiProvider: ApiProvider,
    private val contactGroupLocalDataSource: ContactGroupLocalDataSource
) : CoroutineWorker(context, workerParameters) {

    @Suppress("LongMethod")
    override suspend fun doWork(): Result {

        val userId = inputData.getString(RawUserIdKey)
        val labelId = inputData.getString(RawLabelIdKey)
        val labelContactEmailIds = inputData.getStringArray(RawLabelContactEmailIdsKey)
        val unlabelContactEmailIds = inputData.getStringArray(RawUnlabelContactEmailIdsKey)

        @Suppress("ComplexCondition")
        if (userId == null ||
            labelId == null ||
            @Suppress("UnnecessaryParentheses")
            (labelContactEmailIds.isNullOrEmpty() && unlabelContactEmailIds.isNullOrEmpty())
        ) {
            return Result.failure()
        }

        val api: ApiManager<out ContactGroupApi> = apiProvider.get(UserId(userId))

        @Suppress("TooGenericExceptionThrown")
        return kotlin.runCatching {
            if (labelContactEmailIds?.isNotEmpty() == true) {
                val result = api {
                    labelContactEmails(
                        LabelContactEmailsBody(
                            labelId,
                            labelContactEmailIds.toList()
                        )
                    )
                }.toEither()

                val unsuccessfulResponses = result.getOrNull()?.responses?.filterUnsuccessful() ?: emptyList()

                if (result.isLeft()) {
                    throw Exception("labelContactEmailIds failed")
                } else if (unsuccessfulResponses.isNotEmpty()) {
                    contactGroupLocalDataSource.removeContactEmailIdsFromContactGroup(
                        UserId(userId),
                        LabelId(labelId),
                        unsuccessfulResponses.map { ContactEmailId(it.contactEmailId) }.toSet()
                    )
                    throw Exception("labelContactEmailIds partially failed")
                }
            }

            if (unlabelContactEmailIds?.isNotEmpty() == true) {
                val result = api {
                    unlabelContactEmails(
                        UnlabelContactEmailsBody(
                            labelId,
                            unlabelContactEmailIds.toList()
                        )
                    )
                }.toEither()

                val unsuccessfulResponses = result.getOrNull()?.responses?.filterUnsuccessful() ?: emptyList()

                if (result.isLeft()) {
                    throw Exception("unlabelContactEmails failed")
                } else if (unsuccessfulResponses.isNotEmpty()) {
                    contactGroupLocalDataSource.addContactEmailIdsToContactGroup(
                        UserId(userId),
                        LabelId(labelId),
                        unsuccessfulResponses.map { ContactEmailId(it.contactEmailId) }.toSet()
                    )
                    throw Exception("unlabelContactEmails partially failed")
                }
            }

            Result.success()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.failure() }
        )
    }

    companion object {

        internal const val RawUserIdKey = "userId"
        internal const val RawLabelIdKey = "labelId"
        internal const val RawLabelContactEmailIdsKey = "labelContactEmailIds"
        internal const val RawUnlabelContactEmailIdsKey = "unlabelContactEmailIds"

        fun params(
            userId: UserId,
            labelId: LabelId,
            labelContactEmailIds: Set<ContactEmailId>,
            unlabelContactEmailIds: Set<ContactEmailId>
        ) = mapOf(
            RawUserIdKey to userId.id,
            RawLabelIdKey to labelId.id,
            RawLabelContactEmailIdsKey to labelContactEmailIds.map { it.id }.toTypedArray(),
            RawUnlabelContactEmailIdsKey to unlabelContactEmailIds.map { it.id }.toTypedArray()
        )

    }

}
