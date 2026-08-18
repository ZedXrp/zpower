# Refine "Liquid Glass" Realism & Update Credits

This plan refines the "Liquid Glass" theme with realistic glass material effects and updates the About & Credits sections with new contact and donation information.

## User Review Required

> [!NOTE]
> The "Liquid Distortion" effect using `RenderEffect` requires Android 12 (API 31) or higher. On older versions, the effect will gracefully fallback to standard glass.

> [!IMPORTANT]
> The logo `res/drawable/credits_logo.png` is expected to be present. I will use a fallback icon in the code to ensure the app builds even if the image is missing.

## Proposed Changes

### UI Components

#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Update `GlassCard` and `SquareGlassCard` with:
    - **Internal Glow**: Subtle inner stroke with `accentColor.copy(alpha = 0.1f)`.
    - **Specular Highlights**: 45-degree linear gradient overlay on the top-left.
    - **Multi-layered Rim**: Light outer border and dark inner border.
    - **Distortion Effect**: Apply `RenderEffect` for slight magnification/warp (API 31+).

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)
- Add a dark dimming scrim (`Color.Black.copy(alpha = 0.4f)`) over the background when "Liquid Glass" mode is active.

#### [MODIFY] [AboutDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/AboutDialog.kt)
- Update layout to include the logo.
- Add clickable "Donate" button (https://paypal.me/abdullahexplain).
- Add "Contact Support" link (abdullahexpain@gmail.com).

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Update the Credits section to match the `AboutDialog` info.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project builds successfully.

### Manual Verification
- Verify the "Liquid Glass" cards have the new "glare" and "glow" effects.
- Verify the wallpaper dims when switching to "Liquid Glass" mode.
- Verify the "Donate" and "Contact" links in the About dialog and Settings screen work.
