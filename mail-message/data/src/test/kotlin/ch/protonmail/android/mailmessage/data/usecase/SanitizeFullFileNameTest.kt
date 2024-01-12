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

package ch.protonmail.android.mailmessage.data.usecase

import ch.protonmail.android.mailmessage.data.local.usecase.SanitizeFullFileName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class SanitizeFullFileNameTest(private val testInput: TestInput) {

    private val sanitizeFileName = SanitizeFullFileName()

    @Test
    fun `should provide the sanitized name when invoked`() = with(testInput) {
        // When
        val actualDefaults = sanitizeFileName(testInput.toSanitize)

        // Then
        assertEquals(expectedSanitized, actualDefaults)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                toSanitize = "myfile.zip",
                expectedSanitized = "myfile.zip"
            ),
            TestInput(
                toSanitize = "my file.zip",
                expectedSanitized = "my file.zip"
            ),
            TestInput(
                toSanitize = "double\nsentence.rs",
                expectedSanitized = "double_sentence.rs"
            ),
            TestInput(
                toSanitize = "filewith:colon.rs",
                expectedSanitized = "filewith_colon.rs"
            ),
            TestInput(
                toSanitize = """some\backslash.rs""",
                expectedSanitized = "some_backslash.rs"
            ),
            TestInput(
                toSanitize = "some/slash.rs",
                expectedSanitized = "some_slash.rs"
            ),
            TestInput(
                toSanitize = "../../traversal.rs",
                expectedSanitized = ".._.._traversal.rs"
            ),
            TestInput(
                toSanitize = "..//..//traversal.rs",
                expectedSanitized = "..__..__traversal.rs"
            ),
            TestInput(
                toSanitize = "./traversal.rs",
                expectedSanitized = "._traversal.rs"
            ),
            TestInput(
                toSanitize = ".//traversal.rs",
                expectedSanitized = ".__traversal.rs"
            ),
            TestInput(
                toSanitize = "./../a/file",
                expectedSanitized = "._.._a_file"
            ),
            TestInput(
                toSanitize = "C://Proton/file.rs",
                expectedSanitized = "C___Proton_file.rs"
            ),
            TestInput(
                toSanitize = "path/to/some/other/file.rs",
                expectedSanitized = "path_to_some_other_file.rs"
            ),
            TestInput(
                toSanitize = "p|ped.rs",
                expectedSanitized = "p_ped.rs"
            ),
            TestInput(
                toSanitize = "*.|.*",
                expectedSanitized = "_._._"
            ),
            TestInput(
                toSanitize = "\u0000a test.rs",
                expectedSanitized = "_a test.rs"
            ),
            TestInput(
                toSanitize = "you're a st*r.rs",
                expectedSanitized = "you're a st_r.rs"
            ),
            TestInput(
                toSanitize = ".....",
                expectedSanitized = "_"
            ),
            TestInput(
                toSanitize = "what?did?you?do",
                expectedSanitized = "what_did_you_do"
            ),
            TestInput(
                toSanitize = """5>>"4.zip"""",
                expectedSanitized = "5___4.zip_"
            ),
            TestInput(
                toSanitize = """xargs -n4 <<<"${'$'}var2"""",
                expectedSanitized = "xargs -n4 ____" + "$" + "var2_"
            ),
            TestInput(
                toSanitize = "\"coding\"",
                expectedSanitized = "_coding_"
            ),
            TestInput(
                toSanitize = "spacing. ",
                expectedSanitized = "spacing_"
            )
        )
    }

    data class TestInput(
        val toSanitize: String,
        val expectedSanitized: String
    )
}
