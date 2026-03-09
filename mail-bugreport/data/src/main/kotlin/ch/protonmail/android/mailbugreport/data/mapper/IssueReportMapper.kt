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
import ch.protonmail.android.mailcommon.data.mapper.LocalIssueReport
import uniffi.mail_uniffi.ClientType

internal fun IssueReport.toLocalIssueReport() = LocalIssueReport(
    operatingSystem = operatingSystem.value,
    operatingSystemVersion = operatingSystemVersion.value,
    client = client.value,
    clientVersion = clientVersion.value,
    clientType = ClientType.EMAIL,
    title = title.value,
    summary = summary.value,
    stepsToReproduce = stepsToReproduce.value,
    expectedResult = expectedResult.value,
    actualResult = actualResult.value,
    logs = shouldIncludeLogs.value,
    additionalFiles = additionalLogFiles.value
)
