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

package ch.protonmail.android.mailcommon.domain.sample

import me.proton.core.label.domain.entity.LabelId

object LabelIdSample {

    val AllDraft = LabelId("1")
    val AllMail = LabelId("5")
    val AllSent = LabelId("2")
    val Archive = LabelId("6")
    val Document = LabelId("document")
    val Folder2021 = LabelId("2021")
    val Folder2022 = LabelId("2022")
    val Label2021 = LabelId("Label2021")
    val Label2022 = LabelId("Label2022")
    val LabelCoworkers = LabelId("LabelCoworkers")
    val LabelFriends = LabelId("LabelFriends")
    val Inbox = LabelId("0")
    val News = LabelId("news")
    val Starred = LabelId("10")
    val Trash = LabelId("3")
    val Spam = LabelId("4")

    fun build() = LabelId("label")
}
