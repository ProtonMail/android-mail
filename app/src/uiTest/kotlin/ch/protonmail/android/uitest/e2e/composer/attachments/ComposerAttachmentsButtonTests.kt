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

package ch.protonmail.android.uitest.e2e.composer.attachments

import java.time.Instant
import androidx.test.filters.SdkSuppress
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.topAppBarSection
import ch.protonmail.android.uitest.robot.detail.model.attachments.AttachmentDetailItemEntry
import ch.protonmail.android.uitest.robot.detail.section.attachmentsSection
import ch.protonmail.android.uitest.robot.detail.section.verify
import ch.protonmail.android.uitest.robot.helpers.deviceRobot
import ch.protonmail.android.uitest.robot.helpers.section.intents
import ch.protonmail.android.uitest.robot.helpers.section.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Before
import org.junit.Test

@RegressionTest
@SdkSuppress(minSdkVersion = 30, maxSdkVersion = 32)
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class ComposerAttachmentsButtonTests : MockedNetworkTest(), ComposerAttachmentsTests {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    private lateinit var attachmentName: String
    private val defaultExpectedEntry: AttachmentDetailItemEntry
        get() = AttachmentDetailItemEntry(
            index = 0,
            fileName = attachmentName,
            fileSize = "78 kB",
            hasDeleteIcon = true
        )

    @Before
    fun setupTests() {
        attachmentName = "${Instant.now().epochSecond}.jpg"
        val uri = initFakeFileUri("placeholder_image.jpg", attachmentName, "image/jpg")
        stubPickerActivityResultWithUri(uri)

        mockWebServer.dispatcher combineWith mockNetworkDispatcher()
        navigator { navigateTo(Destination.Composer) }
    }

    @Test
    @TestId("226087", "226088")
    fun testMainAttachmentsButtonInteractions() {
        composerRobot {
            topAppBarSection { tapAttachmentsButton() }
        }

        deviceRobot {
            intents { verify { filePickerIntentWasLaunched() } }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    @TestId("226090")
    fun testAttachmentChipEntryUponPicking() {
        composerRobot {
            topAppBarSection { tapAttachmentsButton() }
            attachmentsSection { verify { hasAttachments(defaultExpectedEntry) } }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    @TestId("226091")
    fun testAttachmentChipDuplicateEntryUponPicking() {
        val expectedEntries = arrayOf(
            defaultExpectedEntry,
            defaultExpectedEntry.copy(index = 1)
        )

        composerRobot {
            topAppBarSection { tapAttachmentsButton() }
            attachmentsSection { verify { hasAttachments(defaultExpectedEntry) } }

            topAppBarSection { tapAttachmentsButton() }
            attachmentsSection { verify { hasAttachments(*expectedEntries) } }
        }
    }
}
