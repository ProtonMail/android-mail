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

package ch.protonmail.android.mailbugreport.presentation.model

import java.io.File

sealed interface ApplicationLogsOperation {

    sealed interface ApplicationLogsAction : ApplicationLogsOperation {

        sealed interface Export : ApplicationLogsAction {
            data object ShareLogs : Export
            data object ExportLogs : Export
        }

        sealed interface View : ApplicationLogsAction {
            data object ViewLogcat : View
            data object ViewEvents : View
        }
    }

    sealed interface ApplicationLogsEvent : ApplicationLogsOperation {
        sealed interface Export : ApplicationLogsEvent {
            data class ShareReady(val file: File) : Export
            data class ExportReady(val file: File) : Export
        }

        sealed interface View : ApplicationLogsEvent {
            data object LogcatReady : View
            data object EventsReady : View
        }
    }
}
