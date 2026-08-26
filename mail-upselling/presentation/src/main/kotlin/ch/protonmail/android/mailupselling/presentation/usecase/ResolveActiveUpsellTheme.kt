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

package ch.protonmail.android.mailupselling.presentation.usecase

import ch.protonmail.android.mailupselling.presentation.model.UpsellContentTheme
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Per spec: the modal content is shown randomly from 7 variants, rotating every 14 days to one not yet shown;
 * when all have been shown the selection resets.
 *
 * A per-user shuffle is indexed by the 14-day window, so each user gets their own random order and
 * sees every variant once per cycle.
 */
class ResolveActiveUpsellTheme @Inject constructor(
    private val clock: Clock
) {

    operator fun invoke(userId: UserId): UpsellContentTheme {
        val themes = UpsellContentTheme.entries
        val window = clock.now().toEpochMilliseconds() / 1.days.inWholeMilliseconds / ROTATION_DAYS
        val cycle = window / themes.size
        val order = themes.shuffled(Random(userId.id.hashCode() + cycle))
        return order[(window % themes.size).toInt()]
    }

    private companion object {
        const val ROTATION_DAYS = 14L
    }
}
