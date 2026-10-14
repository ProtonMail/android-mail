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

import android.os.Build
import ch.protonmail.android.mailbugreport.domain.model.IssueReport
import ch.protonmail.android.mailbugreport.domain.model.IssueReportField
import ch.protonmail.android.mailcommon.domain.AppInformation
import javax.inject.Inject

class CreateIssueReport @Inject constructor(
    private val appInformation: AppInformation
) {

    @Suppress("LongParameterList")
    operator fun invoke(
        summary: IssueReportField.Summary,
        stepsToReproduce: IssueReportField.StepsToReproduce,
        expectedResult: IssueReportField.ExpectedResult,
        actualResult: IssueReportField.ActualResult,
        shouldIncludeLogs: IssueReportField.ShouldIncludeLogs,
        additionalFilePaths: IssueReportField.AdditionalFilePaths
    ): IssueReport {
        // This condition can be dropped later when Rust respects the value for additional logs as well.
        val additionalFiles = if (shouldIncludeLogs.value) additionalFilePaths else NoAdditionalFilePaths

        return IssueReport(
            operatingSystemVersion = IssueReportField.OperatingSystemVersion(Build.VERSION.SDK_INT.toString()),
            clientVersion = IssueReportField.ClientVersion(formatAppVersionName()),
            summary = summary,
            stepsToReproduce = stepsToReproduce,
            expectedResult = expectedResult,
            actualResult = actualResult,
            shouldIncludeLogs = shouldIncludeLogs,
            additionalLogFiles = additionalFiles
        )
    }

    private fun formatAppVersionName() =
        "${appInformation.appVersionNameWithInstallSource} (${appInformation.appVersionCode})"

    private companion object {

        val NoAdditionalFilePaths = IssueReportField.AdditionalFilePaths(emptyList())
    }
}
