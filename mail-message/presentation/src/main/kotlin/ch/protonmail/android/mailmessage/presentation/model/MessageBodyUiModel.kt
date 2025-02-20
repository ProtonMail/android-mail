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

package ch.protonmail.android.mailmessage.presentation.model

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.serialization.Serializable
import me.proton.core.user.domain.entity.UserAddress

data class MessageBodyUiModel(
    val messageId: MessageId,
    val messageBody: String,
    val messageBodyWithoutQuote: String,
    val mimeType: MimeTypeUiModel,
    val shouldShowEmbeddedImages: Boolean,
    val shouldShowRemoteContent: Boolean,
    val shouldShowEmbeddedImagesBanner: Boolean,
    val shouldShowRemoteContentBanner: Boolean,
    val shouldShowExpandCollapseButton: Boolean,
    val shouldShowOpenInProtonCalendar: Boolean,
    val attachments: AttachmentGroupUiModel?,
    val userAddress: UserAddress?,
    val viewModePreference: ViewModePreference,
    val printEffect: Effect<Unit>,
    val shouldRestrictWebViewHeight: Boolean,
    val replyEffect: Effect<Unit>,
    val replyAllEffect: Effect<Unit>,
    val forwardEffect: Effect<Unit>
)

enum class MimeTypeUiModel(val value: String) {
    PlainText("text/plain"),
    Html("text/html")
}

@Serializable
enum class ViewModePreference { ThemeDefault, LightMode, DarkMode }
