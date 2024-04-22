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

package ch.protonmail.android.mailsettings.data.repository.remote

import androidx.work.ExistingWorkPolicy
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailsettings.data.remote.UpdateAddressIdentityWorker
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import org.junit.Test

internal class AddressIdentityRemoteDataSourceImplTest {

    private val enqueuer = mockk<Enqueuer>()
    private val addressIdentityRemoteDataSource = AddressIdentityRemoteDataSourceImpl(enqueuer)

    @Test
    fun `should enqueue the work with the correct parameters`() {
        // Given
        val expectedUserId = UserId("123")
        val expectedAddressId = AddressId("addressId")
        val expectedDisplayName = DisplayName("display-name")
        val expectedSignatureValue = SignatureValue("signature-name")

        val expectedWorkerId = "UpdateAddressIdentityWorker-${expectedAddressId.id}"
        val expectedParams = UpdateAddressIdentityWorker.params(expectedUserId, expectedAddressId)

        every {
            enqueuer.enqueueUniqueWork<UpdateAddressIdentityWorker>(
                expectedUserId,
                expectedWorkerId,
                expectedParams,
                existingWorkPolicy = ExistingWorkPolicy.REPLACE
            )
        } just runs

        // When
        addressIdentityRemoteDataSource.updateAddressIdentity(
            expectedUserId, expectedAddressId, expectedDisplayName, expectedSignatureValue
        )

        // Then
        verify(exactly = 1) {
            enqueuer.enqueueUniqueWork<UpdateAddressIdentityWorker>(
                expectedUserId,
                expectedWorkerId,
                expectedParams,
                existingWorkPolicy = ExistingWorkPolicy.REPLACE
            )
        }
        confirmVerified(enqueuer)
    }
}
