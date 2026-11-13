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

package ch.protonmail.android.navigation.route

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import ch.protonmail.android.design.compose.navigation.require
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTreatment
import ch.protonmail.android.mailbyoe.presentation.intro.GmailIntroScreen
import ch.protonmail.android.mailbyoe.presentation.navigation.ByoeSurfaceArgument
import ch.protonmail.android.mailbyoe.presentation.navigation.ByoeTreatmentArgument
import ch.protonmail.android.navigation.model.BYOE_TREATMENT_KEY
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.GMAIL_INTRO_SURFACE_KEY

/** [onShown] runs on the screen's first composition: the spotlight is on screen, before any tap. */
internal fun NavGraphBuilder.addByoeSpotlight(
    onShown: (treatment: ByoeTreatment) -> Unit,
    onConnect: (treatment: ByoeTreatment, importEmails: Boolean) -> Unit,
    onLater: (treatment: ByoeTreatment, method: ByoeDismissMethod) -> Unit
) {
    composable(Destination.Screen.ByoeSpotlight.route) {
        val treatment = requireNotNull(ByoeTreatmentArgument.decode(it.require<String>(BYOE_TREATMENT_KEY)))
        LaunchedEffect(Unit) { onShown(treatment) }
        GmailIntroScreen(
            isSpotlight = true,
            treatment = treatment,
            onConnect = { importEmails -> onConnect(treatment, importEmails) },
            onLater = { method -> onLater(treatment, method) }
        )
    }
}

internal fun NavGraphBuilder.addGmailIntro(
    onConnect: (surface: ByoeSurface, importEmails: Boolean) -> Unit,
    onLater: (surface: ByoeSurface, method: ByoeDismissMethod) -> Unit
) {
    composable(Destination.Screen.GmailIntro.route) {
        val surface = requireNotNull(ByoeSurfaceArgument.decode(it.require<String>(GMAIL_INTRO_SURFACE_KEY)))
        GmailIntroScreen(
            isSpotlight = false,
            treatment = requireNotNull(ByoeTreatmentArgument.decode(it.require<String>(BYOE_TREATMENT_KEY))),
            onConnect = { importEmails -> onConnect(surface, importEmails) },
            onLater = { method -> onLater(surface, method) }
        )
    }
}
