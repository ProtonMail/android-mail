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

package ch.protonmail.android.mailbugreport.domain.usecase

import ch.protonmail.android.mailbugreport.domain.model.IssueReport
import ch.protonmail.android.mailbugreport.domain.model.IssueReportField
import ch.protonmail.android.mailcommon.domain.AppInformation
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CreateIssueReportTest {

    private val appInformation = AppInformation(
        appVersionName = "7.12.0",
        appInstallSourceSuffix = "+apk",
        appVersionCode = 1
    )
    private val createIssueReport = CreateIssueReport(appInformation)

    @Test
    fun `should generate the issue report entity`() {
        // Given
        val summary = IssueReportField.Summary("summary")
        val reproSteps = IssueReportField.StepsToReproduce("repro")
        val expectedResult = IssueReportField.ExpectedResult("expect")
        val actualResult = IssueReportField.ActualResult("actual")
        val includeLogs = IssueReportField.ShouldIncludeLogs(true)
        val additionalFilePaths = IssueReportField.AdditionalFilePaths(listOf("path1", "path2"))

        val expectedIssueReport = IssueReport(
            operatingSystem = IssueReportField.OperatingSystem("Android"),
            operatingSystemVersion = IssueReportField.OperatingSystemVersion("0"),
            client = IssueReportField.Client("Android Proton Mail"),
            clientVersion = IssueReportField.ClientVersion("7.12.0+apk (1)"),
            title = IssueReportField.Title("Proton Mail Android App - Bug report"),
            summary = summary,
            stepsToReproduce = reproSteps,
            expectedResult = expectedResult,
            actualResult = actualResult,
            shouldIncludeLogs = includeLogs,
            additionalLogFiles = additionalFilePaths
        )

        // When
        val actual =
            createIssueReport(summary, reproSteps, expectedResult, actualResult, includeLogs, additionalFilePaths)

        // Then
        assertEquals(expectedIssueReport, actual)
    }

    @Test
    fun `should generate the issue report entity with no additional logs when includeLogs is false`() {
        // Given
        val summary = IssueReportField.Summary("summary")
        val reproSteps = IssueReportField.StepsToReproduce("repro")
        val expectedResult = IssueReportField.ExpectedResult("expect")
        val actualResult = IssueReportField.ActualResult("actual")
        val includeLogs = IssueReportField.ShouldIncludeLogs(false)
        val additionalFilePaths = IssueReportField.AdditionalFilePaths(listOf("path1", "path2"))

        val expectedIssueReport = IssueReport(
            operatingSystem = IssueReportField.OperatingSystem("Android"),
            operatingSystemVersion = IssueReportField.OperatingSystemVersion("0"),
            client = IssueReportField.Client("Android Proton Mail"),
            clientVersion = IssueReportField.ClientVersion("7.12.0+apk (1)"),
            title = IssueReportField.Title("Proton Mail Android App - Bug report"),
            summary = summary,
            stepsToReproduce = reproSteps,
            expectedResult = expectedResult,
            actualResult = actualResult,
            shouldIncludeLogs = includeLogs,
            additionalLogFiles = IssueReportField.AdditionalFilePaths(emptyList())
        )

        // When
        val actual =
            createIssueReport(summary, reproSteps, expectedResult, actualResult, includeLogs, additionalFilePaths)

        // Then
        assertEquals(expectedIssueReport, actual)
    }
}
