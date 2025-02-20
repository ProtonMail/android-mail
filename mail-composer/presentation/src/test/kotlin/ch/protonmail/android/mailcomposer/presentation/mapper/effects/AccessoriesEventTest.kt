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

package ch.protonmail.android.mailcomposer.presentation.mapper.effects

import ch.protonmail.android.mailcomposer.domain.model.MessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailcomposer.presentation.model.operations.AccessoriesEvent
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.AccessoriesStateModification
import ch.protonmail.android.mailcomposer.presentation.reducer.modifications.ComposerStateModifications
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@RunWith(Parameterized::class)
internal class AccessoriesEventTest(
    @Suppress("unused") private val testName: String,
    private val effect: AccessoriesEvent,
    private val expectedModification: ComposerStateModifications
) {

    @Test
    fun `should map to the correct modification`() {
        val actualModification = effect.toStateModifications()
        assertEquals(expectedModification, actualModification)
    }

    companion object {

        private val password = MessagePassword(UserId("123"), MessageId("456"), "password", null)
        private val expiration = MessageExpirationTime(UserId("123"), MessageId("456"), 2.hours)
        private val nullExpiration = null

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "OnPasswordChanged to modification",
                AccessoriesEvent.OnPasswordChanged(password),
                ComposerStateModifications(
                    accessoriesModification = AccessoriesStateModification.MessagePasswordUpdated(
                        password
                    )
                )
            ),
            arrayOf(
                "OnExpirationChanged (not null( to modification",
                AccessoriesEvent.OnExpirationChanged(expiration),
                ComposerStateModifications(
                    accessoriesModification = AccessoriesStateModification.MessageExpirationUpdated(
                        expiration.expiresIn
                    )
                )
            ),
            arrayOf(
                "OnExpirationChanged (not null( to modification",
                AccessoriesEvent.OnExpirationChanged(nullExpiration),
                ComposerStateModifications(
                    accessoriesModification = AccessoriesStateModification.MessageExpirationUpdated(Duration.ZERO)
                )
            )
        )
    }
}
