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

package me.proton.android.core.devicemigration.presentation.origin.qr

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat.getInsetsController
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.Size
import dagger.hilt.android.AndroidEntryPoint
import me.proton.android.core.devicemigration.presentation.R
import me.proton.android.core.devicemigration.presentation.databinding.ActivityEdmQrCaptureBinding
import me.proton.core.presentation.ui.ProtonActivity
import me.proton.core.presentation.utils.doOnApplyWindowInsets
import me.proton.core.presentation.utils.launchOnScreenView
import me.proton.core.presentation.utils.onClick
import me.proton.core.presentation.utils.viewBinding
import uniffi.mail_account_uniffi.QrLoginScanScreenViewTotalScreenId
import uniffi.mail_account_uniffi.qrLoginScanScreenTotal
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Modeled after [com.journeyapps.barcodescanner.CaptureActivity].
 */
@AndroidEntryPoint
public class EdmQrCaptureActivity : ProtonActivity() {

    private val binding: ActivityEdmQrCaptureBinding by viewBinding(ActivityEdmQrCaptureBinding::inflate)
    private lateinit var capture: CaptureManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initEventListeners()
        adjustLayout()

        capture = CaptureManager(this, binding.zxingBarcodeScanner).apply {
            initializeFromIntent(intent, savedInstanceState)
            decode()
        }
        capture.setShowMissingCameraPermissionDialog(false)

        launchOnScreenView {
            qrLoginScanScreenTotal(QrLoginScanScreenViewTotalScreenId.CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }


    @Deprecated("Deprecated in androidx-activity, but required for CaptureManager.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
        binding.zxingBarcodeScanner.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)

    private fun initEventListeners() {
        binding.closeButton.onClick {
            setResult(RESULT_CANCELED)
            finish()
        }
        binding.enterCodeButton.onClick {
            setResult(RESULT_CANCELED, Intent().putExtra(RESULT_MANUAL_INPUT_REQUESTED, true))
            finish()
        }
    }

    private fun adjustLayout() {
        val defaultSize = AppCompatResources.getDrawable(this, R.drawable.edm_qr_square)!!.intrinsicWidth
        binding.zxingBarcodeScanner.doOnLayout { view ->
            val a = min((min(view.width, view.height) * SIZE_MULTIPLIER).roundToInt(), defaultSize)
            binding.zxingBarcodeScanner.barcodeView.framingRectSize = Size(a, a)
            binding.zxingBarcodeScanner.statusView.updateLayoutParams<MarginLayoutParams> {
                topMargin = view.height / 2 + a / 2 + resources.getDimensionPixelSize(R.dimen.gap_huge)
            }
        }

        binding.enterCodeButton.doOnApplyWindowInsets { view, windowInsetsCompat, initialMargin, _ ->
            val bars = windowInsetsCompat.getInsets(systemBars())
            view.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = bars.bottom + initialMargin.bottom
            }
        }

        binding.closeButton.doOnApplyWindowInsets { view, windowInsetsCompat, initialMargin, _ ->
            val bars = windowInsetsCompat.getInsets(displayCutout() or systemBars())
            view.updateLayoutParams<MarginLayoutParams> {
                topMargin = bars.top + initialMargin.top
            }
        }
    }

    internal companion object {

        const val RESULT_MANUAL_INPUT_REQUESTED = "RESULT_MANUAL_INPUT_REQUESTED"
        private const val SIZE_MULTIPLIER = 0.8
    }
}
