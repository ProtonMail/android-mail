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

package ch.protonmail.android.uitest.e2e.composer.subject

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.test.annotations.suite.SmokeTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.e2e.composer.ComposerTests
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.composer.section.verify
import ch.protonmail.android.uitest.robot.helpers.deviceRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Before
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerSubjectTests : MockedNetworkTest(), ComposerTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Before
    fun prelude() {
        mockWebServer.dispatcher combineWith composerMockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @SmokeTest
    @TestId("194249", "194250", "194251")
    fun testRomanNumbersSymbolsCharInputInSubject() {
        val expectedInput = (RomanLettersRange + RomanNumbersRange + SymbolsRange).joinToString(separator = "")

        composerRobot {
            typeAndVerifySubject(expectedInput)
        }
    }

    @Test
    @TestId("194252")
    fun testNonRomanLettersAndNumbersCharInputInSubject() {
        composerRobot {
            typeAndVerifySubject(NonRomanChars)
        }
    }

    @Test
    @TestId("194253")
    fun testEmojiInputInSubject() {
        composerRobot {
            typeAndVerifySubject(Emojis)
        }
    }

    @Test
    @TestId("194254", "194255")
    fun testImeActionInSubject() {
        composerRobot {
            subjectSection {
                typeSubject(DefaultSubject)
                performImeAction()
            }

            messageBodySection { verify { hasFocus() } }
        }
    }

    @Test
    @TestId("194260")
    fun testVeryLongSubjectField() {
        val expectedInput = StringBuilder().apply {
            repeat(times = 50) { append(RomanLettersRange + RomanNumbersRange + SymbolsRange) }
        }.toString()

        composerRobot {
            typeAndVerifySubject(expectedInput)
        }
    }

    @Test
    @TestId("194265")
    fun testDeletionInSubject() {
        composerRobot {
            subjectSection {
                typeSubject(DefaultSubject)
                clearField()

                verify { hasEmptySubject() }
            }
        }
    }

    @Test
    @SmokeTest
    @TestId("194273")
    fun testSubjectFieldOnActivityRecreation() {
        composerRobot {
            subjectSection {
                typeSubject(DefaultSubject)
            }
        }

        deviceRobot { triggerActivityRecreation() }

        composerRobot {
            subjectSection {
                verify { hasSubject(DefaultSubject) }
            }
        }
    }

    private fun ComposerRobot.typeAndVerifySubject(expectedInput: String) {
        subjectSection {
            typeSubject(expectedInput)
            verify { hasSubject(expectedInput) }
        }
    }

    private companion object {

        val RomanLettersRange = 'A'.rangeTo('z').filter { it.isLetter() }
        val RomanNumbersRange = 0..9
        val SymbolsRange = '!'.rangeTo('~').filterNot { it.isLetterOrDigit() }
        const val NonRomanChars = "ςερτυθιοπασδφγηξκλζχψωβνμ"
        const val Emojis = "😡😦🥶👻👍"

        const val DefaultSubject = "Subject!!"
    }
}
