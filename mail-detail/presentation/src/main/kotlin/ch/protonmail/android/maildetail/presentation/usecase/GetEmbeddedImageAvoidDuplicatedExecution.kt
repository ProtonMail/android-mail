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

package ch.protonmail.android.maildetail.presentation.usecase

import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImage
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class GetEmbeddedImageAvoidDuplicatedExecution @Inject constructor(
    private val getEmbeddedImage: GetEmbeddedImage
) {

    private val loadEmbeddedImageJobMap = mutableMapOf<String, Deferred<GetEmbeddedImageResult?>>()

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        contentId: String,
        coroutineContext: CoroutineContext
    ): GetEmbeddedImageResult? = runCatching {
        withContext(coroutineContext) {
            if (loadEmbeddedImageJobMap[contentId]?.isActive == true) {
                loadEmbeddedImageJobMap[contentId]
            } else {
                async { getEmbeddedImage(userId, messageId, contentId).getOrNull() }.apply {
                    loadEmbeddedImageJobMap[contentId] = this
                }
            }?.await()
        }
    }.getOrNull()
}
