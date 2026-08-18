# Fix Liquid Glass UI and Slider Behavior

This plan addresses UI issues in the "Liquid Glass" theme, specifically fixing the laggy settings sliders, correcting background dimming logic, and implementing a multi-layered glass card to ensure text legibility while maintaining the glass effect.

## User Review Required

> [!IMPORTANT]
> The "True Backdrop Blur" on Android 12+ using `RenderEffect` blurs the content of the layer it is applied to. By moving this to a bottom layer, we ensure the foreground text and images remain sharp. However, it still only blurs the color/tint of that bottom layer, not the actual wallpaper behind the app, unless a complex background-capturing technique is used. The proposed multi-layer approach is the standard way to achieve a "Glass" look with sharp text in Jetpack Compose.

## Proposed Changes

### [Settings] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)

- **Local State for Sliders**: Introduce `localBlurIntensity` and `localWallpaperDim` using `mutableStateOf`.
- **Smooth Sliding**: Update local state on `onValueChange` for real-time UI feedback without ViewModel overhead.
- **Delayed ViewModel Update**: Call `viewModel.updateBlurIntensity` and `viewModel.updateWallpaperDim` only in `onValueChangeFinished`.

### [Core UI] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)

- **Sharp Background**: Ensure `Modifier.blur` is NOT applied to the background layer when `BackgroundStyle.LIQUID_GLASS` is active.
- **Wallpaper Dim Placement**: Verify the `Wallpaper Dim` box is correctly placed between the background and the main content.

### [Components] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)

- **Multi-Layer GlassCard**:
    - **Layer 1 (Bottom)**: A `Box` with `graphicsLayer { renderEffect = ... }` and a slight white tint (`0.06f`). This layer provides the blurred glass effect.
    - **Layer 2 (Middle)**: Drawing logic for specular highlights and glass rims moved to a separate `Box` or `drawBehind` that isn't blurred.
    - **Layer 3 (Top)**: The `Column` containing text and images, ensuring they remain 100% sharp.
- **SquareGlassCard Refactor**: Similar multi-layer logic for the square variant.
- **Backward Compatibility**: Ensure `RenderEffect` is only used on API 31+ (Android 12) to prevent crashes.

## Verification Plan

### Automated Tests
- Run existing UI tests to ensure no regressions in navigation or basic settings functionality.

### Manual Verification
- **Sliders**: Open Settings and slide the Refraction and Wallpaper Dim sliders. Verify they are smooth and only update the system state once released.
- **Liquid Glass Effect**: Check that card text is sharp and readable even with high refraction intensity.
- **Background**: Verify the wallpaper remains sharp in Liquid Glass mode but the cards show the "glass" effect.
- **Dimming**: Verify the "Wallpaper Dim" slider correctly darkens the area behind the cards but not the cards' content itself.
