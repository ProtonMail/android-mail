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

package ch.protonmail.android.mailbugreport.data.mapper

import ch.protonmail.android.mailbugreport.domain.model.IssueReport
import ch.protonmail.android.mailbugreport.domain.model.IssueReportField
import ch.protonmail.android.mailcommon.data.mapper.LocalIssueReport
import uniffi.mail_uniffi.ClientType
import kotlin.test.Test
import kotlin.test.assertEquals

internal class IssueReportMapperTest {

    @Test
    fun `should map Mail model to Rust data model correctly`() {
        // Given
        val issueReport = IssueReport(
            operatingSystem = IssueReportField.OperatingSystem("OS"),
            operatingSystemVersion = IssueReportField.OperatingSystemVersion("Version"),
            client = IssueReportField.Client("client"),
            clientVersion = IssueReportField.ClientVersion("client-version"),
            title = IssueReportField.Title("title"),
            summary = IssueReportField.Summary("summary"),
            stepsToReproduce = IssueReportField.StepsToReproduce("steps"),
            expectedResult = IssueReportField.ExpectedResult("expected"),
            actualResult = IssueReportField.ActualResult("actual"),
            shouldIncludeLogs = IssueReportField.ShouldIncludeLogs(true),
            additionalLogFiles = IssueReportField.AdditionalFilePaths(listOf("path1", "path2"))
        )

        val expectedRustType = LocalIssueReport(
            operatingSystem = issueReport.operatingSystem.value,
            operatingSystemVersion = issueReport.operatingSystemVersion.value,
            client = issueReport.client.value,
            clientVersion = issueReport.clientVersion.value,
            clientType = ClientType.EMAIL,
            title = issueReport.title.value,
            summary = issueReport.summary.value,
            stepsToReproduce = issueReport.stepsToReproduce.value,
            expectedResult = issueReport.expectedResult.value,
            actualResult = issueReport.actualResult.value,
            logs = issueReport.shouldIncludeLogs.value,
            additionalFiles = issueReport.additionalLogFiles.value
        )

        // When
        val actual = issueReport.toLocalIssueReport()

        // Then
        assertEquals(expectedRustType, actual)
    }
}
