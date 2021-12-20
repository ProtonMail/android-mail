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
package ch.protonmail.android.uitests.robots.mailbox

@Suppress("ExpressionBodySyntax")
interface ApplyLabelRobotInterface {

    fun addLabel(name: String): Any {
        labelName(name)
            .add()
        return this
    }

    fun labelName(name: String): ApplyLabelRobotInterface {
        return this
    }

    fun selectLabelByName(name: String): ApplyLabelRobotInterface {
        return this
    }

    fun checkAlsoArchiveCheckBox(): ApplyLabelRobotInterface {
        return this
    }

    fun apply(): Any {
        return this
    }

    fun applyAndArchive(): Any {
        apply()
        return this
    }

    fun add() {}

    fun closeLabelModal(): Any {
        return Any()
    }
}
