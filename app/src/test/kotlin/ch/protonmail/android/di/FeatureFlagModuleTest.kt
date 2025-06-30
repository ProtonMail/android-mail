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

package ch.protonmail.android.di

import ch.protonmail.android.mailcommon.domain.MailFeatureDefaults
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class FeatureFlagModuleTest(private val testInput: TestInput) {

    @Test
    fun `should provide the correct defaults`() = with(testInput) {
        // When
        val actualDefaults = FeatureFlagModule.provideDefaultMailFeatureFlags()

        // Then
        assertEquals(expectedDefaults, actualDefaults)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                buildFlavor = "dev",
                buildDebug = true,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.ConversationMode to true,
                    MailFeatureId.RatingBooster to false,
                    MailFeatureId.NPSFeedback to false
                )
            ),
            TestInput(
                buildFlavor = "alpha",
                buildDebug = true,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.ConversationMode to true,
                    MailFeatureId.RatingBooster to false,
                    MailFeatureId.NPSFeedback to false
                )
            ),
            TestInput(
                buildFlavor = "prod",
                buildDebug = false,
                expectedDefaultsMap = mapOf(
                    MailFeatureId.ConversationMode to true,
                    MailFeatureId.RatingBooster to false,
                    MailFeatureId.NPSFeedback to false
                )
            )
        )
    }

    data class TestInput(
        val buildFlavor: String,
        val buildDebug: Boolean,
        val expectedDefaultsMap: Map<MailFeatureId, Boolean>
    ) {
        val expectedDefaults = MailFeatureDefaults(expectedDefaultsMap)
    }
}
