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
package ch.protonmail.android.uitest.robot.mailbox.composer

import androidx.annotation.IdRes

/**
 * Class represents Message Attachments.
 */
@Suppress("unused", "ExpressionBodySyntax")
open class MessageAttachmentsRobot {

    fun addImageCaptureAttachment(@IdRes drawable: Int): ComposerRobot =
        mockCameraImageCapture(drawable)

    fun addTwoImageCaptureAttachments(
        @IdRes firstDrawable: Int,
        @IdRes secondDrawable: Int
    ): ComposerRobot =
        mockCameraImageCapture(firstDrawable)
            .attachments()
            .mockCameraImageCapture(secondDrawable)

    fun addFileAttachment(@IdRes drawable: Int): ComposerRobot =
        mockFileAttachment(drawable)

    private fun mockCameraImageCapture(@IdRes drawableId: Int): ComposerRobot {
        return ComposerRobot()
    }

    private fun mockFileAttachment(@IdRes drawable: Int): ComposerRobot {
        return ComposerRobot()
    }
}
