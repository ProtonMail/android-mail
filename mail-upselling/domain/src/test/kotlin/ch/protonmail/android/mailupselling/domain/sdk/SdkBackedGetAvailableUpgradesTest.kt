/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailupselling.domain.sdk

import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.android.core.payment.domain.model.ProductOfferList
import me.proton.android.core.payment.domain.usecase.GetAvailableUpgrades
import me.proton.android.payment.product.model.Product
import me.proton.android.payment.product.usecase.GetProducts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class SdkBackedGetAvailableUpgradesTest {

    private val legacy = mockk<GetAvailableUpgrades>()
    private val getProducts = mockk<GetProducts>()
    private val getProductsLazy = mockk<Lazy<GetProducts>> {
        every { get() } returns getProducts
    }
    private val mapper = mockk<SdkProductOfferMapper>()
    private val flag = mockk<FeatureFlag<Boolean>>()

    private val subject = SdkBackedGetAvailableUpgrades(
        legacyGetAvailableUpgrades = legacy,
        getProducts = getProductsLazy,
        mapper = mapper,
        sdkUpgradesReadEnabled = flag
    )

    @Test
    fun `when FF is OFF returns legacy verbatim without resolving the SDK use case`() = runTest {
        val legacyResult = listOf<ProductOfferList>(mockk(), mockk())
        coEvery { flag.get() } returns false
        coEvery { legacy() } returns legacyResult

        val result = subject()

        assertEquals(legacyResult, result)
        verify(exactly = 0) { getProductsLazy.get() }
        coVerify(exactly = 0) { getProducts.invoke() }
        coVerify(exactly = 0) { mapper.mapToProductOfferList(any(), any()) }
    }

    @Test
    fun `when FF is ON returns mapped composite of SDK products with legacy customerId by productId`() = runTest {
        val legacyMeta = mockk<me.proton.android.core.payment.domain.model.ProductMetadata> {
            every { productId } returns "google_mail_plus_1_yearly"
        }
        val legacyEntry = mockk<ProductOfferList> {
            every { metadata } returns legacyMeta
        }
        val sdkProducts = listOf<Product>(mockk())
        val mapped = listOf<ProductOfferList>(mockk())

        coEvery { flag.get() } returns true
        coEvery { legacy() } returns listOf(legacyEntry)
        coEvery { getProducts.invoke() } returns Result.success(sdkProducts)
        every {
            mapper.mapToProductOfferList(
                sdkProducts = sdkProducts,
                legacyByProductId = mapOf("google_mail_plus_1_yearly" to legacyEntry)
            )
        } returns mapped

        val result = subject()

        assertEquals(mapped, result)
    }

    @Test
    fun `when FF is ON and SDK call fails the failure propagates as exception`() = runTest {
        coEvery { flag.get() } returns true
        coEvery { legacy() } returns emptyList()
        coEvery { getProducts.invoke() } returns Result.failure(RuntimeException("boom"))

        assertFailsWith<RuntimeException> { subject() }
    }

    @Test
    fun `when FF is ON and legacy call fails the failure propagates as exception`() = runTest {
        coEvery { flag.get() } returns true
        coEvery { legacy() } throws RuntimeException("legacy down")
        coEvery { getProducts.invoke() } returns Result.success(emptyList())

        assertFailsWith<RuntimeException> { subject() }
    }
}
