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

package ch.protonmail.android.composer.data.sample

import ch.protonmail.android.composer.data.remote.resource.DraftMessageResource
import ch.protonmail.android.composer.data.remote.resource.UpdateDraftBody
import me.proton.core.crypto.common.pgp.Armored

object UpdateDraftBodySample {

    val RemoteDraft = build(message = DraftMessageResourceSample.RemoteDraft)

    fun build(
        message: DraftMessageResource = DraftMessageResourceSample.NewDraftWithSubject,
        attachmentKeyPackets: Map<String, Armored> = emptyMap()
    ) = UpdateDraftBody(
        message = message,
        attachmentKeyPackets = attachmentKeyPackets
    )
}
