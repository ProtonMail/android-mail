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

package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcomposer.domain.repository.CameraTempImageRepository
import ch.protonmail.android.mailcomposer.domain.repository.PastedImageRepository
import javax.inject.Inject

/**
 * Purges the composer's transient staging directories. These hold pasted and captured images only until the
 * attachment layer has copied them into draft storage, so on a cold start any leftovers are orphaned and safe
 * to remove. Intended to be run once at app startup; each repository removes its staging area fire-and-forget.
 */
class ClearComposerStagingFiles @Inject constructor(
    private val pastedImageRepository: PastedImageRepository,
    private val cameraTempImageRepository: CameraTempImageRepository
) {

    operator fun invoke() {
        pastedImageRepository.clearStagingArea()
        cameraTempImageRepository.clearStagingArea()
    }
}
