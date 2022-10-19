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

package ch.protonmail.android.testdata.session

import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.account.data.entity.SessionEntity
import me.proton.core.domain.entity.Product
import me.proton.core.network.domain.session.Session

object SessionEntityTestData {

    val Primary = build(SessionTestData.Primary)

    fun build(session: Session = SessionTestData.build()) = SessionEntity(
        accessToken = session.accessToken,
        product = Product.Mail,
        refreshToken = session.refreshToken,
        scopes = session.scopes,
        sessionId = session.sessionId,
        userId = UserIdTestData.Primary
    )
}
