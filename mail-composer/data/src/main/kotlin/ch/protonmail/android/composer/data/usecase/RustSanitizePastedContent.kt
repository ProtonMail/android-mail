/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.composer.data.usecase

import ch.protonmail.android.mailcomposer.domain.model.PasteMimeType
import ch.protonmail.android.mailcomposer.domain.usecase.SanitizePastedContent
import uniffi.mail_uniffi.MessageMimeType
import uniffi.mail_uniffi.sanitizePastedContent
import javax.inject.Inject

class RustSanitizePastedContent @Inject constructor() : SanitizePastedContent {

    override fun invoke(content: String, mimeType: PasteMimeType): String =
        sanitizePastedContent(content, mimeType.toRustMimeType())
}

internal fun PasteMimeType.toRustMimeType(): MessageMimeType = when (this) {
    PasteMimeType.Html -> MessageMimeType.TEXT_HTML
    PasteMimeType.PlainText -> MessageMimeType.TEXT_PLAIN
}
