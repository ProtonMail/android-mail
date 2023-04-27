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

package ch.protonmail.android.uitest.robot.detail

import ch.protonmail.android.uitest.robot.detail.section.DetailBottomSheetSection
import ch.protonmail.android.uitest.robot.detail.section.DetailTopBarSection
import ch.protonmail.android.uitest.robot.detail.section.MessageBodySection
import ch.protonmail.android.uitest.robot.detail.section.MessageHeaderSection
import ch.protonmail.android.uitest.robot.detail.section.conversation.ConversationDetailCollapsedMessagesSection

internal fun ConversationDetailRobot.detailTopBarSection(
    func: DetailTopBarSection.() -> Unit
) = DetailTopBarSection(composeTestRule).apply(func)

internal fun ConversationDetailRobot.messagesCollapsedSection(
    func: ConversationDetailCollapsedMessagesSection.() -> Unit
) = ConversationDetailCollapsedMessagesSection(composeTestRule).apply(func)

internal fun ConversationDetailRobot.messageHeaderSection(
    func: MessageHeaderSection.() -> Unit
) = MessageHeaderSection(composeTestRule).apply(func)

internal fun ConversationDetailRobot.messageBodySection(
    func: MessageBodySection.() -> Unit
) = MessageBodySection(composeTestRule).apply(func)

internal fun ConversationDetailRobot.bottomSheetSection(
    func: DetailBottomSheetSection.() -> Unit
) = DetailBottomSheetSection(composeTestRule).apply(func)
