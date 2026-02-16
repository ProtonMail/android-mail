/*
 * Copyright (C) 2025 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.dagger

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.core.payment.data.IconResourceManagerRust
import me.proton.android.core.payment.data.PaymentMetricsTrackerRust
import me.proton.android.core.payment.data.SubscriptionManagerRust
import me.proton.android.core.payment.domain.IconResourceManager
import me.proton.android.core.payment.domain.PaymentMetricsTracker
import me.proton.android.core.payment.domain.SubscriptionManager
import me.proton.android.core.payment.presentation.component.PurchaseButtonProcessor
import me.proton.android.core.payment.presentation.component.PurchaseButtonProcessorNative
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface CorePaymentModule {

    @Binds
    fun bindPurchaseButtonProcessor(processor: PurchaseButtonProcessorNative): PurchaseButtonProcessor
}

@Module
@InstallIn(SingletonComponent::class)
interface CorePaymentRustModule {

    @Binds
    @Singleton
    fun provideIconResourceManager(manager: IconResourceManagerRust): IconResourceManager

    @Binds
    @Singleton
    fun providePaymentMetricsTracker(manager: PaymentMetricsTrackerRust): PaymentMetricsTracker

    @Binds
    @Singleton
    fun provideSubscriptionManager(manager: SubscriptionManagerRust): SubscriptionManager
}
