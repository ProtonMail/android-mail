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

package ch.protonmail.android.mailmessage.domain.usecase

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class ParseMimeAttachmentHeaders @Inject constructor() {

    operator fun invoke(headers: String): JsonObject = buildJsonObject {
        headers.split(CarriageReturnNewLineDelimiter, NewLineDelimiter).forEach headerSplit@{ headerLine ->
            val headerLineSplit = headerLine.replace(Quote, EMPTY_STRING).split(ColonDelimiter)
            if (headerLineSplit.size != 2) return@headerSplit

            val headerValueSplit = headerLineSplit[1].split(SemiColonDelimiter)
            put(headerLineSplit[0], headerValueSplit[0])

            headerValueSplit.forEachIndexed headerValueSplit@{ index, headerValuePiece ->
                if (index != 0) {
                    val headerValuePieceSplit = headerValuePiece.split(EqualsDelimiter)
                    if (headerValuePieceSplit.size != 2) return@headerValueSplit
                    put(headerValuePieceSplit[0], headerValuePieceSplit[1])
                }
            }
        }
    }

    companion object {
        private const val CarriageReturnNewLineDelimiter = "\r\n"
        private const val NewLineDelimiter = "\n"
        private const val Quote = "\""
        private const val ColonDelimiter = ": "
        private const val SemiColonDelimiter = "; "
        private const val EqualsDelimiter = "="
    }
}
