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

package ch.protonmail.android.uitest.robot.helpers.section

import java.io.File
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.test.robot.ProtonMailSectionRobot
import ch.protonmail.android.uitest.robot.helpers.DeviceRobot
import kotlin.test.assertNotNull

@AttachTo(targets = [DeviceRobot::class], identifier = "storage")
internal class DeviceRobotExternalStorageSection : ProtonMailSectionRobot {

    private val baseDir = File(DefaultDownloadPath)

    @VerifiesOuter
    inner class Verify {

        private val String.rawName: String
            get() = this.split(".")[0]

        private val String.extension: String
            get() = this.split(".")[1]

        fun containsFileInDownloadsWithName(fileName: String) {
            // Multiple files with the same name have "(1)", "(2)"... appended before the extension.
            // We want to match "File.jpg"/"File (1).jpg" and so on.
            val regex = "${fileName.rawName}(\\s)?(\\([0-9]+\\))?\\.${fileName.extension}".toRegex()

            val actualFile = baseDir.listFiles()?.any {
                it.name.matches(regex)
            }

            assertNotNull(actualFile)
        }
    }

    private companion object {

        const val DefaultDownloadPath = "/sdcard/Download"
    }
}
