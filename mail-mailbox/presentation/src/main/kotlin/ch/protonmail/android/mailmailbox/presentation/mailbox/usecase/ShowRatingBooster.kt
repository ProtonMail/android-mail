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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import android.app.Activity
import com.google.android.play.core.review.ReviewManager
import timber.log.Timber
import javax.inject.Inject

class ShowRatingBooster @Inject constructor(
    private val reviewManager: ReviewManager
) {

    operator fun invoke(activity: Activity) {
        reviewManager.requestReviewFlow().addOnCompleteListener {
            if (it.isSuccessful) {
                reviewManager.launchReviewFlow(activity, it.result)
            } else {
                Timber.d("Showing the rating booster was unsuccessful", it.exception)
            }
        }
    }
}
