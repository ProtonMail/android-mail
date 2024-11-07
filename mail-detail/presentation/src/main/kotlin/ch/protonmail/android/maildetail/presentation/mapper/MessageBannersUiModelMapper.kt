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

package ch.protonmail.android.maildetail.presentation.mapper

import java.time.Duration
import java.time.Instant
import android.content.Context
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBannersUiModel
import ch.protonmail.android.maildetail.presentation.util.toFormattedAutoDeleteExpiration
import ch.protonmail.android.maildetail.presentation.util.toFormattedDurationParts
import ch.protonmail.android.mailmessage.domain.model.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject
import kotlin.time.toKotlinDuration

class MessageBannersUiModelMapper @Inject constructor(@ApplicationContext val context: Context) {

    fun createMessageBannersUiModel(message: Message) = MessageBannersUiModel(
        shouldShowPhishingBanner = message.isPhishing(),
        expirationBannerText = formatExpirationTime(message),
        autoDeleteBannerText = formatAutoDeleteTime(message)
    )

    private fun formatExpirationTime(message: Message): TextUiModel? {

        val duration = Duration.between(
            Instant.now(),
            Instant.ofEpochSecond(message.expirationTime)
        ).toKotlinDuration()

        val formattedExpiration = duration.toFormattedDurationParts(context.resources).joinToString(separator = ", ")

        return formattedExpiration.takeIfNotEmpty()?.takeIf { message.isExpirationFrozen() }?.let {
            TextUiModel(context.resources.getString(R.string.message_expiration_banner_text, it))
        }
    }

    private fun formatAutoDeleteTime(message: Message): TextUiModel? {

        val duration = Duration.between(
            Instant.now(),
            Instant.ofEpochSecond(message.expirationTime)
        ).toKotlinDuration()

        val formattedExpiration = duration.toFormattedAutoDeleteExpiration(context.resources)

        return formattedExpiration?.takeIf { !message.isExpirationFrozen() }?.let {
            TextUiModel(formattedExpiration)
        }

    }
}
