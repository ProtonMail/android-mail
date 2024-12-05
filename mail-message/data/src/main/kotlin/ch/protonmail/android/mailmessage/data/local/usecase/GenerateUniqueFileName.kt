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

package ch.protonmail.android.mailmessage.data.local.usecase

import java.time.Instant
import javax.inject.Inject

class GenerateUniqueFileName @Inject constructor() {

    /**
     * Appends the current timestamp to the provided original name to guarantee uniqueness.
     *
     * This is mostly useful when saving multiple files to external storage with the same name, which
     * are usually handled automatically by the OS up to ~30 occurrences, but might fail after that.
     *
     * In case the resulting String length exceeds 255 characters, truncation is applied on the original filename.
     */
    operator fun invoke(originalName: String): String {
        val timestamp = Instant.now().toEpochMilli()
        val timestampSuffix = "_$timestamp"

        // Special case: if the filename starts with a dot, it's just an extension
        if (originalName.startsWith(".")) {
            return "$timestampSuffix$originalName"
        }

        val lastDot = originalName.lastIndexOf('.')
        val nameWithoutExt = if (lastDot > 0) originalName.substring(0, lastDot) else originalName
        val extension = if (lastDot > 0) originalName.substring(lastDot) else ""

        val newName = "$nameWithoutExt$timestampSuffix$extension"

        return if (newName.length > MaxLength) {
            val timestampLength = timestampSuffix.length
            val extensionLength = extension.length
            val maxNameLength = MaxLength - timestampLength - extensionLength

            "${nameWithoutExt.take(maxNameLength)}$timestampSuffix$extension"
        } else {
            newName
        }
    }

    private companion object {
        const val MaxLength = 255
    }
}
