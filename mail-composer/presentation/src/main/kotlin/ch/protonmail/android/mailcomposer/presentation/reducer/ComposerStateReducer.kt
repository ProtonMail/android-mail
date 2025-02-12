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

package ch.protonmail.android.mailcomposer.presentation.reducer

import ch.protonmail.android.mailcomposer.presentation.model.ComposerStates
import ch.protonmail.android.mailcomposer.presentation.model.operations.ComposerStateEvent
import javax.inject.Inject

class ComposerStateReducer @Inject constructor() {

    internal fun reduceNewState(currentStates: ComposerStates, event: ComposerStateEvent): ComposerStates {
        val modifications = event.toStateModifications()

        return currentStates.copy(
            main = modifications.mainModification?.apply(currentStates.main)
                ?: currentStates.main,
            attachments = modifications.attachmentsModification?.apply(currentStates.attachments)
                ?: currentStates.attachments,
            accessories = modifications.accessoriesModification?.apply(currentStates.accessories)
                ?: currentStates.accessories,
            effects = modifications.effectsModification?.apply(currentStates.effects)
                ?: currentStates.effects
        )
    }
}
