/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */
package ch.protonmail.android.design.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color

private object ProtonPalette {

    // ------------------------ All Colors Light UI -----------------------------
    // Dark Gray Tones
    val Charade = Color(0xFF0F0F0F)
    val EerieBlack = Color(0xFF191927)
    val Shark = Color(0xFF31343A)
    val Trout = Color(0xFF535964)
    val OsloGray = Color(0xFF848993)
    val Manatee = Color(0xFF989BA2)
    val Aluminium = Color(0xFFA7AAB0)
    val Ghost = Color(0xFFC8CBD0)
    val FrenchGrey = Color(0xFFC8C8CC)
    val AthensGray = Color(0xFFE9EAEC)
    val FrostGray = Color(0xFFEFEEF2)
    val Porcelain = Color(0xFFF4F5F8)
    val White = Color(0xFFFFFFFF)

    // Purple Tones
    val DeepCove = Color(0xFF181039)
    val PortGore = Color(0xFF241C43)
    val MulledWine = Color(0xFF524D68)
    val Topaz = Color(0xFF747088)
    val CadetBlue = Color(0xFFA7A4B5)

    // Brand Tones
    val Chambray = Color(0xFF372580) // plus-30
    val SanMarino = Color(0xFF4D34B3) // plus-20
    val PurpleHeart = Color(0xFF5C3FD9) // plus-10
    val CornflowerBlue = Color(0xFF6D4AFF) // norm
    val Portage = Color(0xFF8A6EFF) // minus-10
    val Perano = Color(0xFFC4B7FF) // minus-20
    val BlueChalk = Color(0xFFEAe5FF) // minus-30
    val Magnolia = Color(0xFFF5F2FF) // minus-40

    // Warm Tones
    val Stiletto = Color(0xFF8A2E3F)
    val MexicanRed = Color(0xFFA51F3E)
    val MaroonFlush = Color(0xFFBA1E55)
    val Amaranth = Color(0xFFE9345B)
    val Cosmos = Color(0xFFFFD8DE)
    val Azalea = Color(0xFFF9CDD6)
    val PeruTan = Color(0xFF8A3600)
    val Pearl = Color(0xFFA4512F)
    val Orange = Color(0xFFFF6713)
    val Romantic = Color(0xFFFFD0B3)
    val SeashellPeach = Color(0xFFFFF0E8)
    val TexasRose = Color(0xFFFFB84D)
    val BrightSun = Color(0xFFFFD143)
    val Dolly = Color(0xFFFFF28E)

    // Green Tones
    val GreenPea = Color(0xFF1E5C4F)
    val TePapaGreen = Color(0xFF1E574B)
    val Genoa = Color(0xFF0F735A)
    val Gossamer = Color(0xFF059A6F)
    val MountainMeadow = Color(0xFF1ED19C)
    val Apple = Color(0xFF3CBB3A)
    val Paradiso = Color(0xFF3C8B8C)
    val AquaIsland = Color(0xFFB1E1D4)
    val Iceberg = Color(0xFFDBF3EE)

    // Purple Tones
    val RoyalPurple = Color(0xFF6638B7)
    val Heliotrope = Color(0xFF9553F9)
    val Melrose = Color(0xFF9C89FF)
    val MediumRedViolet = Color(0xFFA839A4)
    val RipePlum = Color(0xFF52006A)

    // Blue Tones
    val BayOfMany = Color(0xFF213474)
    val Cobalt = Color(0xFF0047AB)
    val DodgerBlue = Color(0xFF4989FF)
    val PictonBlue = Color(0xFF29C0E6)
    val RoyalBlue = Color(0xFF415DF0)

    // ------------------------ All Colors Dark UI -----------------------------
    // Gray Tones
    val Platinum = Color(0xFFEDEDEE)
    val SantasGray = Color(0xFFA4A4AB)
    val SonicSilver = Color(0xFF75757D)
    val CadetGrey = Color(0xFF50505B)
    val Tuna = Color(0xFF393945)
    val Gunmetal = Color(0xFF2B2B38)
    val MidnightBlue = Color(0xFF222230)
    val Obsidian = Color(0xFF20202E)
    val Swamp = Color(0xFF1E1E2B)

    // Blue and Indigo Tones
    val PaleBlue = Color(0xFFD0D0FF)
    val Periwinkle = Color(0xFFADADFB)
    val BlueBell = Color(0xFF9292F9)
    val LightSlateBlue = Color(0xFF7777F8)
    val LightVioletBlue = Color(0xFF6464CE)
    val DuskyIndigo = Color(0xFF4D4D9C)
    val Rhino = Color(0xFF35356A)
    val DarkBlue = Color(0xFF282848)

    // Pink and Orange Tones
    val DeepPink = Color(0xFFC9396C)
    val FlamingoPink = Color(0xFFE15976)
    val AtomicTangerine = Color(0xFFFF9B62)
    val PeachYellow = Color(0xFFFFC978)
    val YellowOrange = Color(0xFFFFDA6A)

    // Green Tones
    val ViridianGreen = Color(0xFF4B8281)
    val Turquoise = Color(0xFF62ADA5)
    val SeafoamGreen = Color(0xFF85C990)
    val MintGreen = Color(0xFF66C166)
    val MysticTeal = Color(0xFF41888F)

    // Purple and Violet Tones
    // val PurpleHeart = Color(0xFF865CD0) - Already defined in Light UI
    val LavenderPurple = Color(0xFFA779FF)
    val PeriwinkleBlue = Color(0xFFC2B6FF)
    val Amethyst = Color(0xFF9A4C99)
    val DeepPurple = Color(0xFF642A78)

    // Blue Tones
    val SteelBlue = Color(0xFF4559A1)
    val SkyBlue = Color(0xFF4A74B1)
    val DuskyBlue = Color(0xFF6A8AC9)
    val VibrantSky = Color(0xFF55A0CB)
    val SoftRoyal = Color(0xFF5D6ECD)
}

@Stable
@Suppress("LongParameterList")
class ProtonColors(
    isDark: Boolean,

    shade100: Color,
    shade90: Color,
    shade80: Color,
    shade60: Color,
    shade50: Color,
    shade45: Color,
    shade40: Color,
    shade20: Color,
    shade15: Color,
    shade10: Color,
    shade0: Color,

    brandPlus30: Color = ProtonPalette.Chambray,
    brandPlus20: Color = ProtonPalette.SanMarino,
    brandPlus10: Color = ProtonPalette.PurpleHeart,
    brandNorm: Color = ProtonPalette.CornflowerBlue,
    brandMinus10: Color = ProtonPalette.Portage,
    brandMinus20: Color = ProtonPalette.Perano,
    brandMinus30: Color = ProtonPalette.BlueChalk,
    brandMinus40: Color = ProtonPalette.Magnolia,

    textNorm: Color = shade100,
    textAccent: Color = brandPlus10,
    textWeak: Color = shade80,
    textHint: Color = shade50,
    textDisabled: Color = shade40,
    textInverted: Color = shade0,
    textSelected: Color = brandPlus10,

    iconNorm: Color = shade100,
    iconAccent: Color = brandPlus10,
    iconWeak: Color = shade80,
    iconHint: Color = shade50,
    iconDisabled: Color = shade40,
    iconInverted: Color = shade0,
    iconSelected: Color = brandPlus10,

    interactionBrandStrongNorm: Color = brandPlus10,
    interactionBrandStrongPressed: Color = brandPlus20,

    interactionFabNorm: Color = shade0,
    interactionFabPressed: Color = shade20,

    interactionWeakNorm: Color = shade10,
    interactionWeakPressed: Color = shade20,
    interactionWeakDisabled: Color = shade10,

    interactionBrandWeakNorm: Color = brandMinus30,
    interactionBrandWeakPressed: Color = brandMinus20,
    interactionBrandWeakDisabled: Color = brandMinus40,

    backgroundNorm: Color = shade0,
    backgroundSecondary: Color = shade10,
    backgroundDeep: Color = shade20,
    backgroundAvatar: Color = shade0,

    backgroundInvertedNorm: Color = shade10,
    backgroundInvertedSecondary: Color = shade0,
    backgroundInvertedDeep: Color = shade20,
    backgroundInvertedBorder: Color = shade10,

    separatorNorm: Color = shade10,
    separatorStrong: Color = shade20,

    borderLight: Color = shade10,
    borderNorm: Color = shade20,
    borderStrong: Color = shade40,

    blenderNorm: Color,

    notificationNorm: Color = shade100,

    notificationError100: Color = ProtonPalette.Azalea,
    notificationError: Color = ProtonPalette.Amaranth,
    notificationError900: Color = ProtonPalette.MexicanRed,

    notificationWarning100: Color = ProtonPalette.Romantic,
    notificationWarning: Color = ProtonPalette.Orange,
    notificationWarning900: Color = ProtonPalette.PeruTan,

    notificationSuccess100: Color = ProtonPalette.AquaIsland,
    notificationSuccess: Color = ProtonPalette.Gossamer,
    notificationSuccess900: Color = ProtonPalette.TePapaGreen,

    interactionBrandDefaultNorm: Color = brandNorm,
    interactionBrandDefaultPressed: Color = brandPlus10,
    interactionBrandDefaultDisabled: Color = brandMinus30,

    floatyBackground: Color = ProtonPalette.EerieBlack,
    floatyPressed: Color = ProtonPalette.Charade,
    floatyText: Color = ProtonPalette.White,

    shadowWeak: Color,
    shadowSoft: Color,
    shadowRaised: Color,
    shadowLifted: Color,
    shadowMedium: Color,

    starDefault: Color = shade50,
    starSelected: Color = ProtonPalette.TexasRose,

    searchHighlightText: Color = ProtonPalette.Charade,
    searchHighlightBackground: Color = ProtonPalette.Dolly,

    sidebarBackground: Color,
    sidebarInteractionPressed: Color,
    sidebarSeparator: Color,
    sidebarTextNorm: Color,
    sidebarTextWeak: Color,
    sidebarTextSelected: Color,
    sidebarIconNorm: Color,
    sidebarIconWeak: Color,
    sidebarIconSelected: Color
) {

    var isDark: Boolean by mutableStateOf(isDark, structuralEqualityPolicy())
        internal set

    var shade100: Color by mutableStateOf(shade100, structuralEqualityPolicy())
        internal set
    var shade90: Color by mutableStateOf(shade90, structuralEqualityPolicy())
        internal set
    var shade80: Color by mutableStateOf(shade80, structuralEqualityPolicy())
        internal set
    var shade60: Color by mutableStateOf(shade60, structuralEqualityPolicy())
        internal set
    var shade50: Color by mutableStateOf(shade50, structuralEqualityPolicy())
        internal set
    var shade45: Color by mutableStateOf(shade45, structuralEqualityPolicy())
        internal set
    var shade40: Color by mutableStateOf(shade40, structuralEqualityPolicy())
        internal set
    var shade20: Color by mutableStateOf(shade20, structuralEqualityPolicy())
        internal set
    var shade15: Color by mutableStateOf(shade15, structuralEqualityPolicy())
        internal set
    var shade10: Color by mutableStateOf(shade10, structuralEqualityPolicy())
        internal set
    var shade0: Color by mutableStateOf(shade0, structuralEqualityPolicy())
        internal set

    var textNorm: Color by mutableStateOf(textNorm, structuralEqualityPolicy())
        internal set
    var textAccent: Color by mutableStateOf(textAccent, structuralEqualityPolicy())
        internal set
    var textWeak: Color by mutableStateOf(textWeak, structuralEqualityPolicy())
        internal set
    var textSelected: Color by mutableStateOf(textSelected, structuralEqualityPolicy())
        internal set
    var textHint: Color by mutableStateOf(textHint, structuralEqualityPolicy())
        internal set
    var textDisabled: Color by mutableStateOf(textDisabled, structuralEqualityPolicy())
        internal set
    var textInverted: Color by mutableStateOf(textInverted, structuralEqualityPolicy())
        internal set

    var iconNorm: Color by mutableStateOf(iconNorm, structuralEqualityPolicy())
        internal set
    var iconAccent: Color by mutableStateOf(iconAccent, structuralEqualityPolicy())
        internal set
    var iconWeak: Color by mutableStateOf(iconWeak, structuralEqualityPolicy())
        internal set
    var iconSelected: Color by mutableStateOf(iconSelected, structuralEqualityPolicy())
        internal set
    var iconHint: Color by mutableStateOf(iconHint, structuralEqualityPolicy())
        internal set
    var iconDisabled: Color by mutableStateOf(iconDisabled, structuralEqualityPolicy())
        internal set
    var iconInverted: Color by mutableStateOf(iconInverted, structuralEqualityPolicy())
        internal set

    var interactionBrandStrongNorm: Color by mutableStateOf(interactionBrandStrongNorm, structuralEqualityPolicy())
        internal set
    var interactionBrandStrongPressed: Color by mutableStateOf(
        interactionBrandStrongPressed, structuralEqualityPolicy()
    )
        internal set


    var interactionFabNorm: Color by mutableStateOf(interactionFabNorm, structuralEqualityPolicy())
        internal set
    var interactionFabPressed: Color by mutableStateOf(interactionFabPressed, structuralEqualityPolicy())
        internal set

    var interactionWeakNorm: Color by mutableStateOf(interactionWeakNorm, structuralEqualityPolicy())
        internal set
    var interactionWeakPressed: Color by mutableStateOf(interactionWeakPressed, structuralEqualityPolicy())
        internal set
    var interactionWeakDisabled: Color by mutableStateOf(interactionWeakDisabled, structuralEqualityPolicy())
        internal set

    var interactionBrandWeakNorm: Color by mutableStateOf(interactionBrandWeakNorm, structuralEqualityPolicy())
        internal set
    var interactionBrandWeakPressed: Color by mutableStateOf(interactionBrandWeakPressed, structuralEqualityPolicy())
        internal set
    var interactionBrandWeakDisabled: Color by mutableStateOf(interactionBrandWeakDisabled, structuralEqualityPolicy())
        internal set

    var backgroundNorm: Color by mutableStateOf(backgroundNorm, structuralEqualityPolicy())
        internal set
    var backgroundSecondary: Color by mutableStateOf(backgroundSecondary, structuralEqualityPolicy())
        internal set
    var backgroundDeep: Color by mutableStateOf(backgroundDeep, structuralEqualityPolicy())
        internal set
    var backgroundAvatar: Color by mutableStateOf(backgroundAvatar, structuralEqualityPolicy())
        internal set

    var backgroundInvertedNorm: Color by mutableStateOf(backgroundInvertedNorm, structuralEqualityPolicy())
        internal set
    var backgroundInvertedSecondary: Color by mutableStateOf(backgroundInvertedSecondary, structuralEqualityPolicy())
        internal set
    var backgroundInvertedDeep: Color by mutableStateOf(backgroundInvertedDeep, structuralEqualityPolicy())
        internal set
    var backgroundInvertedBorder: Color by mutableStateOf(backgroundInvertedBorder, structuralEqualityPolicy())
        internal set

    var separatorNorm: Color by mutableStateOf(separatorNorm, structuralEqualityPolicy())
        internal set
    var separatorStrong: Color by mutableStateOf(separatorStrong, structuralEqualityPolicy())
        internal set

    var borderLight: Color by mutableStateOf(borderLight, structuralEqualityPolicy())
        internal set
    var borderNorm: Color by mutableStateOf(borderNorm, structuralEqualityPolicy())
        internal set
    var borderStrong: Color by mutableStateOf(borderStrong, structuralEqualityPolicy())
        internal set

    var blenderNorm: Color by mutableStateOf(blenderNorm, structuralEqualityPolicy())
        internal set

    var brandPlus30: Color by mutableStateOf(brandPlus30, structuralEqualityPolicy())
        internal set
    var brandPlus20: Color by mutableStateOf(brandPlus20, structuralEqualityPolicy())
        internal set
    var brandPlus10: Color by mutableStateOf(brandPlus10, structuralEqualityPolicy())
        internal set
    var brandNorm: Color by mutableStateOf(brandNorm, structuralEqualityPolicy())
        internal set
    var brandMinus10: Color by mutableStateOf(brandMinus10, structuralEqualityPolicy())
        internal set
    var brandMinus20: Color by mutableStateOf(brandMinus20, structuralEqualityPolicy())
        internal set
    var brandMinus30: Color by mutableStateOf(brandMinus30, structuralEqualityPolicy())
        internal set
    var brandMinus40: Color by mutableStateOf(brandMinus40, structuralEqualityPolicy())
        internal set

    var notificationNorm: Color by mutableStateOf(notificationNorm, structuralEqualityPolicy())
        internal set

    var notificationError100: Color by mutableStateOf(notificationError100, structuralEqualityPolicy())
        internal set
    var notificationError: Color by mutableStateOf(notificationError, structuralEqualityPolicy())
        internal set
    var notificationError900: Color by mutableStateOf(notificationError900, structuralEqualityPolicy())
        internal set

    var notificationWarning100: Color by mutableStateOf(notificationWarning100, structuralEqualityPolicy())
        internal set
    var notificationWarning: Color by mutableStateOf(notificationWarning, structuralEqualityPolicy())
        internal set
    var notificationWarning900: Color by mutableStateOf(notificationWarning900, structuralEqualityPolicy())
        internal set

    var notificationSuccess100: Color by mutableStateOf(notificationSuccess100, structuralEqualityPolicy())
        internal set
    var notificationSuccess: Color by mutableStateOf(notificationSuccess, structuralEqualityPolicy())
        internal set
    var notificationSuccess900: Color by mutableStateOf(notificationSuccess900, structuralEqualityPolicy())
        internal set

    var interactionBrandDefaultNorm: Color by mutableStateOf(interactionBrandDefaultNorm, structuralEqualityPolicy())
        internal set
    var interactionBrandDefaultPressed: Color by mutableStateOf(
        interactionBrandDefaultPressed, structuralEqualityPolicy()
    )
        internal set
    var interactionBrandDefaultDisabled: Color by mutableStateOf(
        interactionBrandDefaultDisabled, structuralEqualityPolicy()
    )
        internal set

    var floatyBackground: Color by mutableStateOf(floatyBackground, structuralEqualityPolicy())
        internal set
    var floatyPressed: Color by mutableStateOf(floatyPressed, structuralEqualityPolicy())
        internal set
    var floatyText: Color by mutableStateOf(floatyText, structuralEqualityPolicy())
        internal set

    var shadowWeak: Color by mutableStateOf(shadowWeak, structuralEqualityPolicy())
        internal set
    var shadowSoft: Color by mutableStateOf(shadowSoft, structuralEqualityPolicy())
        internal set
    var shadowMedium: Color by mutableStateOf(shadowMedium, structuralEqualityPolicy())
        internal set
    var shadowRaised: Color by mutableStateOf(shadowRaised, structuralEqualityPolicy())
        internal set
    var shadowLifted: Color by mutableStateOf(shadowLifted, structuralEqualityPolicy())
        internal set

    var starDefault: Color by mutableStateOf(starDefault, structuralEqualityPolicy())
        internal set
    var starSelected: Color by mutableStateOf(starSelected, structuralEqualityPolicy())
        internal set

    var searchHighlightText: Color by mutableStateOf(searchHighlightText, structuralEqualityPolicy())
        internal set
    var searchHighlightBackground: Color by mutableStateOf(searchHighlightBackground, structuralEqualityPolicy())
        internal set

    var sidebarBackground: Color by mutableStateOf(sidebarBackground, structuralEqualityPolicy())
        internal set
    var sidebarInteractionPressed: Color by mutableStateOf(sidebarInteractionPressed, structuralEqualityPolicy())
        internal set
    var sidebarSeparator: Color by mutableStateOf(sidebarSeparator, structuralEqualityPolicy())
        internal set
    var sidebarTextNorm: Color by mutableStateOf(sidebarTextNorm, structuralEqualityPolicy())
        internal set
    var sidebarTextWeak: Color by mutableStateOf(sidebarTextWeak, structuralEqualityPolicy())
        internal set
    var sidebarTextSelected: Color by mutableStateOf(sidebarTextSelected, structuralEqualityPolicy())
        internal set
    var sidebarIconNorm: Color by mutableStateOf(sidebarIconNorm, structuralEqualityPolicy())
        internal set
    var sidebarIconWeak: Color by mutableStateOf(sidebarIconWeak, structuralEqualityPolicy())
        internal set
    var sidebarIconSelected: Color by mutableStateOf(sidebarIconSelected, structuralEqualityPolicy())
        internal set

    @Suppress("LongMethod")
    fun copy(
        isDark: Boolean = this.isDark,
        shade100: Color = this.shade100,
        shade90: Color = this.shade90,
        shade80: Color = this.shade80,
        shade60: Color = this.shade60,
        shade50: Color = this.shade50,
        shade45: Color = this.shade45,
        shade40: Color = this.shade40,
        shade20: Color = this.shade20,
        shade15: Color = this.shade15,
        shade10: Color = this.shade10,
        shade0: Color = this.shade0,
        textNorm: Color = this.textNorm,
        textAccent: Color = this.textAccent,
        textWeak: Color = this.textWeak,
        textSelected: Color = this.textSelected,
        textHint: Color = this.textHint,
        textDisabled: Color = this.textDisabled,
        textInverted: Color = this.textInverted,
        iconNorm: Color = this.iconNorm,
        iconAccent: Color = this.iconAccent,
        iconWeak: Color = this.iconWeak,
        iconSelected: Color = this.iconSelected,
        iconHint: Color = this.iconHint,
        iconDisabled: Color = this.iconDisabled,
        iconInverted: Color = this.iconInverted,
        interactionBrandStrongNorm: Color = this.interactionBrandStrongNorm,
        interactionBrandStrongPressed: Color = this.interactionBrandStrongPressed,
        interactionFabNorm: Color = this.interactionFabNorm,
        interactionFabPressed: Color = this.interactionFabPressed,
        interactionWeakNorm: Color = this.interactionWeakNorm,
        interactionWeakPressed: Color = this.interactionWeakPressed,
        interactionWeakDisabled: Color = this.interactionWeakDisabled,
        interactionBrandWeakNorm: Color = this.interactionBrandWeakNorm,
        interactionBrandWeakPressed: Color = this.interactionBrandWeakPressed,
        interactionBrandWeakDisabled: Color = this.interactionBrandWeakDisabled,
        backgroundNorm: Color = this.backgroundNorm,
        backgroundSecondary: Color = this.backgroundSecondary,
        backgroundDeep: Color = this.backgroundDeep,
        backgroundAvatar: Color = this.backgroundAvatar,
        backgroundInvertedNorm: Color = this.backgroundInvertedNorm,
        backgroundInvertedSecondary: Color = this.backgroundInvertedSecondary,
        backgroundInvertedDeep: Color = this.backgroundInvertedDeep,
        backgroundInvertedBorder: Color = this.backgroundInvertedBorder,
        separatorNorm: Color = this.separatorNorm,
        separatorStrong: Color = this.separatorStrong,
        borderLight: Color = this.borderLight,
        borderNorm: Color = this.borderNorm,
        borderStrong: Color = this.borderStrong,
        blenderNorm: Color = this.blenderNorm,
        brandPlus30: Color = this.brandPlus30,
        brandPlus20: Color = this.brandPlus20,
        brandPlus10: Color = this.brandPlus10,
        brandNorm: Color = this.brandNorm,
        brandMinus10: Color = this.brandMinus10,
        brandMinus20: Color = this.brandMinus20,
        brandMinus30: Color = this.brandMinus30,
        brandMinus40: Color = this.brandMinus40,
        notificationNorm: Color = this.notificationNorm,
        notificationError100: Color = this.notificationError100,
        notificationError: Color = this.notificationError,
        notificationError900: Color = this.notificationError900,
        notificationWarning100: Color = this.notificationWarning100,
        notificationWarning: Color = this.notificationWarning,
        notificationWarning900: Color = this.notificationWarning900,
        notificationSuccess100: Color = this.notificationSuccess100,
        notificationSuccess: Color = this.notificationSuccess,
        notificationSuccess900: Color = this.notificationSuccess900,
        interactionBrandDefaultNorm: Color = this.interactionBrandDefaultNorm,
        interactionBrandDefaultPressed: Color = this.interactionBrandDefaultPressed,
        interactionBrandDefaultDisabled: Color = this.interactionBrandDefaultDisabled,
        floatyBackground: Color = this.floatyBackground,
        floatyPressed: Color = this.floatyPressed,
        floatyText: Color = this.floatyText,
        starDefault: Color = this.starDefault,
        starSelected: Color = this.starSelected,
        searchHighlightText: Color = this.searchHighlightText,
        searchHighlightBackground: Color = this.searchHighlightBackground,
        shadowRaised: Color = this.shadowRaised,
        shadowLifted: Color = this.shadowLifted,
        shadowWeak: Color = this.shadowWeak,
        shadowSoft: Color = this.shadowSoft,
        shadowMedium: Color = this.shadowMedium,
        sidebarBackground: Color = this.sidebarBackground,
        sidebarInteractionPressed: Color = this.sidebarInteractionPressed,
        sidebarSeparator: Color = this.sidebarSeparator,
        sidebarTextNorm: Color = this.sidebarTextNorm,
        sidebarTextWeak: Color = this.sidebarTextWeak,
        sidebarTextSelected: Color = this.sidebarTextSelected,
        sidebarIconNorm: Color = this.sidebarIconNorm,
        sidebarIconWeak: Color = this.sidebarIconWeak,
        sidebarIconSelected: Color = this.sidebarIconSelected
    ) = ProtonColors(
        isDark = isDark,

        shade100 = shade100,
        shade90 = shade90,
        shade80 = shade80,
        shade60 = shade60,
        shade50 = shade50,
        shade45 = shade45,
        shade40 = shade40,
        shade20 = shade20,
        shade15 = shade15,
        shade10 = shade10,
        shade0 = shade0,

        textNorm = textNorm,
        textAccent = textAccent,
        textWeak = textWeak,
        textHint = textHint,
        textDisabled = textDisabled,
        textInverted = textInverted,
        textSelected = textSelected,

        iconNorm = iconNorm,
        iconAccent = iconAccent,
        iconWeak = iconWeak,
        iconHint = iconHint,
        iconDisabled = iconDisabled,
        iconInverted = iconInverted,
        iconSelected = iconSelected,

        interactionBrandStrongNorm = interactionBrandStrongNorm,
        interactionBrandStrongPressed = interactionBrandStrongPressed,

        interactionFabNorm = interactionFabNorm,
        interactionFabPressed = interactionFabPressed,

        interactionWeakNorm = interactionWeakNorm,
        interactionWeakPressed = interactionWeakPressed,
        interactionWeakDisabled = interactionWeakDisabled,

        interactionBrandWeakNorm = interactionBrandWeakNorm,
        interactionBrandWeakPressed = interactionBrandWeakPressed,
        interactionBrandWeakDisabled = interactionBrandWeakDisabled,

        backgroundNorm = backgroundNorm,
        backgroundSecondary = backgroundSecondary,
        backgroundDeep = backgroundDeep,
        backgroundAvatar = backgroundAvatar,

        backgroundInvertedNorm = backgroundInvertedNorm,
        backgroundInvertedSecondary = backgroundInvertedSecondary,
        backgroundInvertedDeep = backgroundInvertedDeep,
        backgroundInvertedBorder = backgroundInvertedBorder,

        separatorNorm = separatorNorm,
        separatorStrong = separatorStrong,

        borderLight = borderLight,
        borderNorm = borderNorm,
        borderStrong = borderStrong,

        blenderNorm = blenderNorm,

        brandPlus30 = brandPlus30,
        brandPlus20 = brandPlus20,
        brandPlus10 = brandPlus10,
        brandNorm = brandNorm,
        brandMinus10 = brandMinus10,
        brandMinus20 = brandMinus20,
        brandMinus30 = brandMinus30,
        brandMinus40 = brandMinus40,

        notificationNorm = notificationNorm,

        notificationError100 = notificationError100,
        notificationError = notificationError,
        notificationError900 = notificationError900,

        notificationWarning100 = notificationWarning100,
        notificationWarning = notificationWarning,
        notificationWarning900 = notificationWarning900,

        notificationSuccess100 = notificationSuccess100,
        notificationSuccess = notificationSuccess,
        notificationSuccess900 = notificationSuccess900,

        interactionBrandDefaultNorm = interactionBrandDefaultNorm,
        interactionBrandDefaultPressed = interactionBrandDefaultPressed,
        interactionBrandDefaultDisabled = interactionBrandDefaultDisabled,

        floatyBackground = floatyBackground,
        floatyPressed = floatyPressed,
        floatyText = floatyText,

        shadowRaised = shadowRaised,
        shadowLifted = shadowLifted,
        shadowWeak = shadowWeak,
        shadowSoft = shadowSoft,
        shadowMedium = shadowMedium,

        starDefault = starDefault,
        starSelected = starSelected,

        searchHighlightText = searchHighlightText,
        searchHighlightBackground = searchHighlightBackground,

        sidebarBackground = sidebarBackground,
        sidebarInteractionPressed = sidebarInteractionPressed,
        sidebarSeparator = sidebarSeparator,
        sidebarTextNorm = sidebarTextNorm,
        sidebarTextWeak = sidebarTextWeak,
        sidebarTextSelected = sidebarTextSelected,
        sidebarIconNorm = sidebarIconNorm,
        sidebarIconWeak = sidebarIconWeak,
        sidebarIconSelected = sidebarIconSelected
    )

    companion object {

        val Light = baseLight()
        val Dark = baseDark()

        private fun baseLight(
            brandPlus30: Color = ProtonPalette.Chambray,
            brandPlus20: Color = ProtonPalette.SanMarino,
            brandPlus10: Color = ProtonPalette.PurpleHeart,
            brandNorm: Color = ProtonPalette.CornflowerBlue,
            brandMinus10: Color = ProtonPalette.Portage,
            brandMinus20: Color = ProtonPalette.Perano,
            brandMinus30: Color = ProtonPalette.BlueChalk,
            brandMinus40: Color = ProtonPalette.Magnolia
        ) = ProtonColors(
            isDark = false,
            brandPlus30 = brandPlus30,
            brandPlus20 = brandPlus20,
            brandPlus10 = brandPlus10,
            brandNorm = brandNorm,
            brandMinus10 = brandMinus10,
            brandMinus20 = brandMinus20,
            brandMinus30 = brandMinus30,
            brandMinus40 = brandMinus40,
            notificationError100 = ProtonPalette.Azalea,
            notificationError = ProtonPalette.Amaranth,
            notificationError900 = ProtonPalette.MexicanRed,
            notificationWarning100 = ProtonPalette.Romantic,
            notificationWarning = ProtonPalette.Orange,
            notificationWarning900 = ProtonPalette.PeruTan,
            notificationSuccess100 = ProtonPalette.AquaIsland,
            notificationSuccess = ProtonPalette.Gossamer,
            notificationSuccess900 = ProtonPalette.TePapaGreen,
            shade100 = ProtonPalette.Charade,
            shade90 = ProtonPalette.Shark,
            shade80 = ProtonPalette.Trout,
            shade60 = ProtonPalette.OsloGray,
            shade50 = ProtonPalette.Manatee,
            shade45 = ProtonPalette.Aluminium,
            shade40 = ProtonPalette.Ghost,
            shade20 = ProtonPalette.AthensGray,
            shade15 = ProtonPalette.FrostGray,
            shade10 = ProtonPalette.Porcelain,
            shade0 = Color.White,
            shadowWeak = ProtonPalette.Charade.copy(alpha = 0.08f),
            shadowSoft = ProtonPalette.Charade.copy(alpha = 0.6f),
            shadowRaised = ProtonPalette.Charade.copy(alpha = 0.8f),
            shadowLifted = ProtonPalette.Charade.copy(alpha = 0.8f),
            shadowMedium = ProtonPalette.Charade.copy(alpha = 0.16f),
            blenderNorm = ProtonPalette.EerieBlack.copy(alpha = 0.48f),
            textAccent = brandPlus10,
            iconAccent = brandPlus10,
            sidebarBackground = ProtonPalette.DeepCove,
            sidebarInteractionPressed = ProtonPalette.PortGore,
            sidebarSeparator = ProtonPalette.PortGore,
            sidebarTextNorm = ProtonPalette.CadetBlue,
            sidebarTextWeak = ProtonPalette.Topaz,
            sidebarTextSelected = ProtonPalette.BlueChalk,
            sidebarIconNorm = ProtonPalette.CadetBlue,
            sidebarIconWeak = ProtonPalette.Topaz,
            sidebarIconSelected = ProtonPalette.Portage,
            interactionBrandWeakNorm = ProtonPalette.BlueChalk
        )

        @Suppress("LongMethod")
        private fun baseDark(
            brandPlus30: Color = ProtonPalette.PaleBlue,
            brandPlus20: Color = ProtonPalette.Periwinkle,
            brandPlus10: Color = ProtonPalette.BlueBell,
            brandNorm: Color = ProtonPalette.LightSlateBlue,
            brandMinus10: Color = ProtonPalette.LightVioletBlue,
            brandMinus20: Color = ProtonPalette.DuskyIndigo,
            brandMinus30: Color = ProtonPalette.Rhino,
            brandMinus40: Color = ProtonPalette.DarkBlue
        ) = ProtonColors(
            isDark = true,
            brandPlus30 = brandPlus30,
            brandPlus20 = brandPlus20,
            brandPlus10 = brandPlus10,
            brandNorm = brandNorm,
            brandMinus10 = brandMinus10,
            brandMinus20 = brandMinus20,
            brandMinus30 = brandMinus30,
            brandMinus40 = brandMinus40,
            notificationError100 = ProtonPalette.Stiletto,
            notificationError = ProtonPalette.FlamingoPink,
            notificationError900 = ProtonPalette.Cosmos,
            notificationWarning100 = ProtonPalette.Pearl,
            notificationWarning = ProtonPalette.AtomicTangerine,
            notificationWarning900 = ProtonPalette.SeashellPeach,
            notificationSuccess100 = ProtonPalette.GreenPea,
            notificationSuccess = ProtonPalette.Gossamer,
            notificationSuccess900 = ProtonPalette.Iceberg,
            shade100 = ProtonPalette.Platinum,
            shade90 = ProtonPalette.FrenchGrey,
            shade80 = ProtonPalette.SantasGray,
            shade60 = ProtonPalette.SonicSilver,
            shade50 = ProtonPalette.SonicSilver,
            shade45 = ProtonPalette.Tuna,
            shade40 = ProtonPalette.Gunmetal,
            shade20 = ProtonPalette.MidnightBlue,
            shade15 = ProtonPalette.Obsidian,
            shade10 = ProtonPalette.Swamp,
            shade0 = ProtonPalette.EerieBlack,
            shadowWeak = Color.Black.copy(alpha = 0.24f),
            shadowSoft = Color.Black.copy(alpha = 0.8f),
            shadowMedium = Color.Black.copy(alpha = 0.16f),
            shadowRaised = Color.Black.copy(alpha = 0.8f),
            shadowLifted = Color.Black.copy(alpha = 0.86f),
            blenderNorm = Color.Black.copy(alpha = 0.52f),
            textAccent = brandPlus10,
            iconAccent = brandPlus10,
            sidebarBackground = ProtonPalette.EerieBlack,
            sidebarInteractionPressed = ProtonPalette.MidnightBlue,
            sidebarSeparator = ProtonPalette.MidnightBlue,
            sidebarTextNorm = ProtonPalette.SantasGray,
            sidebarTextWeak = ProtonPalette.SonicSilver,
            sidebarTextSelected = ProtonPalette.Platinum,
            sidebarIconNorm = ProtonPalette.SantasGray,
            sidebarIconWeak = ProtonPalette.SonicSilver,
            sidebarIconSelected = ProtonPalette.BlueBell,
            interactionBrandWeakNorm = ProtonPalette.Rhino
        ).let {
            it.copy(
                interactionFabNorm = it.shade40,
                interactionFabPressed = it.shade45,
                interactionWeakNorm = it.shade40,
                interactionWeakPressed = it.shade50,
                interactionWeakDisabled = it.shade10,
                backgroundDeep = it.shade45,
                backgroundAvatar = it.shade45,
                interactionBrandWeakPressed = it.brandMinus20,
                borderLight = it.shade45,
                borderNorm = it.shade45,
                borderStrong = it.shade50,
                starDefault = it.shade80,
                backgroundInvertedNorm = it.shade20,
                backgroundInvertedSecondary = it.shade40,
                backgroundInvertedDeep = it.shade45,
                iconDisabled = it.shade50,
                textDisabled = it.shade50,
                textWeak = it.shade90,
                iconWeak = it.shade90
            )
        }

        fun light(
            brandPlus30: Color = ProtonPalette.Chambray,
            brandPlus20: Color = ProtonPalette.SanMarino,
            brandNorm: Color = ProtonPalette.CornflowerBlue,
            brandMinus20: Color = ProtonPalette.Portage,
            brandMinus40: Color = ProtonPalette.Perano
        ) = baseLight(
            brandPlus30 = brandPlus30,
            brandPlus20 = brandPlus20,
            brandNorm = brandNorm,
            brandMinus20 = brandMinus20,
            brandMinus40 = brandMinus40
        )

        fun dark(
            brandPlus30: Color = ProtonPalette.Rhino,
            brandPlus20: Color = ProtonPalette.DuskyIndigo,
            brandNorm: Color = ProtonPalette.LightSlateBlue,
            brandMinus20: Color = ProtonPalette.BlueBell,
            brandMinus40: Color = ProtonPalette.Periwinkle
        ) = baseDark(
            brandPlus30 = brandPlus30,
            brandPlus20 = brandPlus20,
            brandNorm = brandNorm,
            brandMinus20 = brandMinus20,
            brandMinus40 = brandMinus40
        )
    }
}

fun ProtonColors.textNorm(enabled: Boolean = true) = if (enabled) textNorm else textDisabled
fun ProtonColors.textWeak(enabled: Boolean = true) = if (enabled) textWeak else textDisabled
fun ProtonColors.textInverted(enabled: Boolean = true) = if (enabled) textInverted else textDisabled
fun ProtonColors.interactionBrandDefaultNorm(enabled: Boolean = true) =
    if (enabled) interactionBrandDefaultNorm else interactionBrandDefaultDisabled

internal fun ProtonColors.toMaterial3ThemeColors() = ColorScheme(
    primary = brandNorm,
    onPrimary = Color.White,
    primaryContainer = backgroundNorm,
    onPrimaryContainer = textNorm,
    inversePrimary = Color.White,
    secondary = brandNorm,
    onSecondary = Color.White,
    secondaryContainer = backgroundSecondary,
    onSecondaryContainer = textNorm,
    tertiary = brandPlus20,
    onTertiary = Color.White,
    tertiaryContainer = backgroundNorm,
    onTertiaryContainer = textNorm,
    background = backgroundNorm,
    onBackground = textNorm,
    surface = backgroundNorm,
    onSurface = textNorm,
    surfaceVariant = backgroundNorm,
    onSurfaceVariant = textNorm,
    surfaceTint = Color.Unspecified,
    inverseSurface = backgroundNorm,
    inverseOnSurface = textNorm,
    error = notificationError,
    onError = textInverted,
    errorContainer = backgroundNorm,
    onErrorContainer = textNorm,
    outline = brandNorm,
    outlineVariant = brandNorm,
    scrim = blenderNorm,
    surfaceBright = Color.Unspecified,
    surfaceDim = Color.Unspecified,
    surfaceContainer = Color.Unspecified,
    surfaceContainerHigh = Color.Unspecified,
    surfaceContainerHighest = Color.Unspecified,
    surfaceContainerLow = Color.Unspecified,
    surfaceContainerLowest = Color.Unspecified
)

@Suppress("LongMethod")
fun ProtonColors.updateColorsFrom(other: ProtonColors) {
    isDark = other.isDark

    shade100 = other.shade100
    shade90 = other.shade90
    shade80 = other.shade80
    shade60 = other.shade60
    shade50 = other.shade50
    shade40 = other.shade40
    shade20 = other.shade20
    shade15 = other.shade15
    shade10 = other.shade10
    shade0 = other.shade0

    textNorm = other.textNorm
    textAccent = other.textAccent
    textWeak = other.textWeak
    textHint = other.textHint
    textDisabled = other.textDisabled
    textInverted = other.textInverted

    iconNorm = other.iconNorm
    iconAccent = other.iconAccent
    iconWeak = other.iconWeak
    iconHint = other.iconHint
    iconDisabled = other.iconDisabled
    iconInverted = other.iconInverted

    interactionBrandStrongNorm = other.interactionBrandStrongNorm
    interactionBrandStrongPressed = other.interactionBrandStrongPressed

    interactionFabNorm = other.interactionFabNorm
    interactionFabPressed = other.interactionFabPressed

    interactionWeakNorm = other.interactionWeakNorm
    interactionWeakPressed = other.interactionWeakPressed
    interactionWeakDisabled = other.interactionWeakDisabled

    interactionBrandWeakNorm = other.interactionBrandWeakNorm
    interactionBrandWeakPressed = other.interactionBrandWeakPressed
    interactionBrandWeakDisabled = other.interactionBrandWeakDisabled

    backgroundNorm = other.backgroundNorm
    backgroundSecondary = other.backgroundSecondary
    backgroundDeep = other.backgroundDeep
    backgroundAvatar = other.backgroundAvatar

    separatorNorm = other.separatorNorm
    separatorStrong = other.separatorStrong

    borderLight = other.borderLight
    borderNorm = other.borderNorm
    borderStrong = other.borderStrong

    blenderNorm = other.blenderNorm

    brandPlus30 = other.brandPlus30
    brandPlus20 = other.brandPlus20
    brandPlus10 = other.brandPlus10
    brandNorm = other.brandNorm
    brandMinus10 = other.brandMinus10
    brandMinus20 = other.brandMinus20
    brandMinus30 = other.brandMinus30
    brandMinus40 = other.brandMinus40

    notificationNorm = other.notificationNorm

    notificationError100 = other.notificationError100
    notificationError = other.notificationError
    notificationError900 = other.notificationError900

    notificationWarning100 = other.notificationWarning100
    notificationWarning = other.notificationWarning
    notificationWarning900 = other.notificationWarning900

    notificationSuccess100 = other.notificationSuccess100
    notificationSuccess = other.notificationSuccess
    notificationSuccess900 = other.notificationSuccess900

    interactionBrandDefaultNorm = other.interactionBrandDefaultNorm
    interactionBrandDefaultPressed = other.interactionBrandDefaultPressed
    interactionBrandDefaultDisabled = other.interactionBrandDefaultDisabled

    floatyBackground = other.floatyBackground
    floatyPressed = other.floatyPressed
    floatyText = other.floatyText

    shadowRaised = other.shadowRaised
    shadowLifted = other.shadowLifted
    shadowWeak = other.shadowWeak
    shadowSoft = other.shadowSoft

    starDefault = other.starDefault
    starSelected = other.starSelected

    searchHighlightText = other.searchHighlightText
    searchHighlightBackground = other.searchHighlightBackground

    sidebarBackground = other.sidebarBackground
    sidebarInteractionPressed = other.sidebarInteractionPressed
    sidebarSeparator = other.sidebarSeparator
    sidebarTextNorm = other.sidebarTextNorm
    sidebarTextWeak = other.sidebarTextWeak
    sidebarTextSelected = other.sidebarTextSelected
    sidebarIconNorm = other.sidebarIconNorm
    sidebarIconWeak = other.sidebarIconWeak
    sidebarIconSelected = other.sidebarIconSelected
}

val LocalColors = staticCompositionLocalOf { ProtonColors.Light }
