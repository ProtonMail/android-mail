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

package ch.protonmail.android.mailbyoe.domain.model

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ByoeConnectErrorTypeTest(
    private val code: String?,
    private val expected: ByoeConnectErrorType
) {

    @Test
    fun `given a web error code, when mapping it, then it is the matching error type`() {
        // When
        val actual = ByoeConnectErrorType.fromWebCode(code)

        // Then
        assertEquals(expected, actual)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun data() = listOf(
            arrayOf<Any?>("no-access", ByoeConnectErrorType.NoAccess),
            arrayOf<Any?>("already-added", ByoeConnectErrorType.AlreadyAdded),
            arrayOf<Any?>("create-failed", ByoeConnectErrorType.CreateFailed),
            arrayOf<Any?>("convert-failed", ByoeConnectErrorType.ConvertFailed),
            arrayOf<Any?>("wrong-account", ByoeConnectErrorType.WrongAccount),
            arrayOf<Any?>("token-failed", ByoeConnectErrorType.TokenFailed),
            arrayOf<Any?>("ALREADY_ADDED", ByoeConnectErrorType.AlreadyAdded),
            arrayOf<Any?>("Token_Failed", ByoeConnectErrorType.TokenFailed),
            arrayOf<Any?>("unknown", ByoeConnectErrorType.Unknown),
            arrayOf<Any?>("TEST_ERROR", ByoeConnectErrorType.Unknown),
            arrayOf<Any?>("", ByoeConnectErrorType.Unknown),
            arrayOf<Any?>(null, ByoeConnectErrorType.Unknown)
        )
    }
}
