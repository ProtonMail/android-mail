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

package ch.protonmail.android.mailmessage.presentation.ui

import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent

/**
 * A coarse memory-cost class for a message body, used to size how many bodies the WebView pool keeps alive
 * (ET-6844). It is deliberately derived from the body's rendered height rather than from its source (byte
 * size, image count): a WebView renders at a fixed width, so its memory tracks how TALL the body renders, and
 * the browser already caps a decoded image's cost to its displayed size - so source bytes and image counts
 * overestimate the real footprint while height tracks it. Each class carries a [cost] in abstract units; the
 * pool keeps expanded bodies alive while their summed cost stays within a RAM-scaled budget, so retention
 * tracks content weight instead of a flat headcount.
 */
enum class MessageBodyWeight(val cost: Int) {

    Light(cost = 1),
    Medium(cost = 2),
    Heavy(cost = 4),
    Huge(cost = 8);

    companion object {

        // Rendered-height bands (px), calibrated against measured message heights. Typical light mail renders
        // under MEDIUM_BODY_PX (real bodies topped out ~2,800 px); genuinely huge threads render 15,000+ px, so
        // HUGE_BODY_PX sits there rather than catching merely-long (12-15k) bodies, which are Heavy. A body
        // renders at a fixed width, so its cost grows with height; the peak (visible) cost is what these bands
        // track, and only one body is visible at a time, so keeping more retained bodies does not raise it.
        private const val MEDIUM_BODY_PX = 3_000
        private const val HEAVY_BODY_PX = 7_000
        private const val HUGE_BODY_PX = 15_000

        /**
         * Classifies [content] by how tall it renders ([heightPx], 0 when not measured yet):
         * - a body spilled to a file is always [Huge] - it was too large to hold in memory as text;
         * - otherwise the taller it renders the heavier it is - [Huge] past [HUGE_BODY_PX], [Heavy] past
         *   [HEAVY_BODY_PX], [Medium] past [MEDIUM_BODY_PX], and [Light] below that (including a body not yet
         *   measured, which is treated as cheap until a real height arrives and reclassifies it).
         */
        fun of(content: MessageBodyContent, heightPx: Int): MessageBodyWeight = when (content) {
            is MessageBodyContent.File -> Huge
            is MessageBodyContent.Text -> when {
                heightPx >= HUGE_BODY_PX -> Huge
                heightPx >= HEAVY_BODY_PX -> Heavy
                heightPx >= MEDIUM_BODY_PX -> Medium
                else -> Light
            }
        }
    }
}
