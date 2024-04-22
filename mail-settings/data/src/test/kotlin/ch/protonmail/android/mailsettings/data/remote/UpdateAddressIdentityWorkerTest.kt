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

package ch.protonmail.android.mailsettings.data.remote

import java.net.UnknownHostException
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.AddressIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailsettings.data.remote.UpdateAddressIdentityWorker.Companion.KeyUpdateDataError
import ch.protonmail.android.mailsettings.domain.model.DisplayName
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.repository.AddressIdentityRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import me.proton.core.key.data.api.response.AddressResponse
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.ApiResult
import me.proton.core.user.data.api.AddressApi
import me.proton.core.user.data.api.request.UpdateAddressRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UpdateAddressIdentityWorkerTest {

    private val workManager = mockk<WorkManager>()
    private val addressApi = mockk<AddressApi>()
    private val apiProvider = mockk<ApiProvider>()
    private val params = mockk<WorkerParameters>()
    private val repository = mockk<AddressIdentityRepository>()

    private val worker: UpdateAddressIdentityWorker by lazy {
        UpdateAddressIdentityWorker(
            context = mockk(),
            workerParameters = params,
            repository = repository,
            apiProvider = apiProvider
        )
    }

    @BeforeTest
    fun setup() {
        setupWorkManager()
        setupApiProvider()
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should enqueue worker with the correct constraints`() {
        // Given
        val expectedNetworkType = NetworkType.CONNECTED

        // When
        Enqueuer(workManager)
            .enqueue<UpdateAddressIdentityWorker>(UserId, UpdateAddressIdentityWorker.params(UserId, AddressId))

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        assertEquals(expectedNetworkType, requestSlot.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `should call the API with the correct parameters`() = runTest {
        // Given
        expectValidDisplayName()
        expectValidSignature()
        expectSuccessfulApiInteraction()

        // When
        val result = worker.doWork()

        // Then
        coVerify(exactly = 1) { addressApi.updateAddress(id = AddressId.id, request = UpdateRequest) }
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `should return failure when there is no display name stored`() = runTest {
        // Given
        expectNoDisplayName()
        val expectedError = ListenableWorker.Result.failure(
            workDataOf(KeyUpdateDataError to "Unable to fetch local display name.")
        )

        // When
        val actual = worker.doWork()

        // Then
        assertEquals(expectedError, actual)
    }

    @Test
    fun `should return failure when there is no signature value stored`() = runTest {
        // Given
        expectValidDisplayName()
        expectNoSignatureValue()
        val expectedError = ListenableWorker.Result.failure(
            workDataOf(KeyUpdateDataError to "Unable to fetch local signature value.")
        )

        // When
        val actual = worker.doWork()

        // Then
        assertEquals(expectedError, actual)
    }

    @Test
    fun `should return retry when the API call is not successful and the error is retryable`() = runTest {
        // Given
        expectValidDisplayName()
        expectValidSignature()
        expectUnSuccessfulApiInteraction(RetryableException)

        // When
        val actual = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), actual)
    }

    @Test
    fun `should return error if the API fails with a non retryable error`() = runTest {
        // Given
        expectValidDisplayName()
        expectValidSignature()
        expectUnSuccessfulApiInteraction(NonRetryableException)

        // When
        val actual = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.failure(), actual)
    }

    private fun expectValidDisplayName() {
        coEvery { repository.getDisplayName(AddressId) } returns DisplayName(DisplayName).right()
    }

    private fun expectNoDisplayName() {
        coEvery { repository.getDisplayName(AddressId) } returns DataError.Local.NoDataCached.left()
    }

    private fun expectValidSignature() {
        coEvery {
            repository.getSignatureValue(AddressId)
        } returns SignatureValue(SignatureValue).right()
    }

    private fun expectNoSignatureValue() {
        coEvery { repository.getSignatureValue(AddressId) } returns DataError.Local.NoDataCached.left()
    }

    private fun expectSuccessfulApiInteraction() {
        coEvery { addressApi.updateAddress(AddressId.id, UpdateRequest) } just Runs
    }

    private fun expectUnSuccessfulApiInteraction(exception: Exception) {
        coEvery { addressApi.updateAddress(AddressId.id, UpdateRequest) } throws exception
    }

    private fun setupApiProvider() {


        coEvery { apiProvider.get<AddressApi>(UserId).invoke<AddressResponse>(block = any()) } coAnswers {
            val block = firstArg<suspend AddressApi.() -> AddressResponse>()
            try {
                ApiResult.Success(block(addressApi))
            } catch (e: Exception) {
                when (e) {
                    NonRetryableException -> ApiResult.Error.Parse(e)
                    RetryableException -> ApiResult.Error.Connection()
                    else -> throw e
                }
            }
        }
    }

    private fun setupWorkManager() {
        every { params.taskExecutor } returns mockk(relaxed = true)
        every { params.inputData.getString(UpdateAddressIdentityWorker.RawUserIdKey) } returns UserId.id
        every { params.inputData.getString(UpdateAddressIdentityWorker.RawAddressIdKey) } returns AddressId.id

        coEvery { workManager.enqueue(ofType<OneTimeWorkRequest>()) } returns mockk()
    }

    private companion object {

        const val DisplayName = "display"
        const val SignatureValue = "signature"

        val UserId = UserIdSample.Primary
        val AddressId = AddressIdSample.Primary
        val UpdateRequest = UpdateAddressRequest(DisplayName, SignatureValue)

        val NonRetryableException = SerializationException()
        val RetryableException = UnknownHostException()
    }
}
