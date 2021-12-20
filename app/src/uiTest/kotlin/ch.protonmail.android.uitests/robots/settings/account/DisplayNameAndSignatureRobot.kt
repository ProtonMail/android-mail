/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitests.robots.settings.account

import androidx.annotation.StringRes

/**
 * Class represents Display name and Signature view.
 */
@Suppress("unused", "ExpressionBodySyntax")
class DisplayNameAndSignatureRobot {

    fun setSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        return this
    }

    fun setMobileSignatureToggleTo(state: Boolean): DisplayNameAndSignatureRobot {
        return this
    }

    fun setDisplayNameTextTo(text: String): DisplayNameAndSignatureRobot {
        return this
    }

    /**
     * Contains all the validations that can be performed by [DisplayNameAndSignatureRobot].
     */
    class Verify {

        fun signatureToggleCheckedStateIs(state: Boolean): DisplayNameAndSignatureRobot {
            return DisplayNameAndSignatureRobot()
        }

        fun mobileSignatureToggleCheckedStateIs(state: Boolean): DisplayNameAndSignatureRobot {
            return DisplayNameAndSignatureRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    @SuppressWarnings("EmptyFunctionBlock")
    private fun switch(@StringRes tagId: Int) {}
}
