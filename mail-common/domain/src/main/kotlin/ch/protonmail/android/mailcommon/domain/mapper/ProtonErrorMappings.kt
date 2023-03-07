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

package ch.protonmail.android.mailcommon.domain.mapper

import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.model.ProtonError.Banned
import ch.protonmail.android.mailcommon.domain.model.ProtonError.Base64Format
import ch.protonmail.android.mailcommon.domain.model.ProtonError.Companion
import ch.protonmail.android.mailcommon.domain.model.ProtonError.InsufficientScope
import ch.protonmail.android.mailcommon.domain.model.ProtonError.MessageSearchQuerySyntax
import ch.protonmail.android.mailcommon.domain.model.ProtonError.MessageUpdateDraftNotDraft
import ch.protonmail.android.mailcommon.domain.model.ProtonError.MessageValidateKeyNotAssociated
import ch.protonmail.android.mailcommon.domain.model.ProtonError.PayloadTooLarge
import ch.protonmail.android.mailcommon.domain.model.ProtonError.PermissionDenied
import ch.protonmail.android.mailcommon.domain.model.ProtonError.Unknown
import ch.protonmail.android.mailcommon.domain.model.ProtonError.UploadFailure

@Suppress("MagicNumber")
fun Companion.fromProtonCode(code: Int): ProtonError =
    when (code) {
        2026 -> PermissionDenied
        2027 -> InsufficientScope
        2028 -> Banned
        2030 -> UploadFailure
        2031 -> PayloadTooLarge
        2063 -> Base64Format
        15_034 -> MessageUpdateDraftNotDraft
        15_213 -> MessageValidateKeyNotAssociated
        15_225 -> MessageSearchQuerySyntax
        else -> Unknown
    }
