# Refine Liquid Glass Aesthetic and Data Deletion Safety

This plan refines the "Liquid Glass" UI components to match a high-end frosted look, implements a dynamic wallpaper system based on the user's accent color, and adds a 5-second safety countdown for the "Delete All Data" action.

## User Review Required

> [!IMPORTANT]
> The "DELETE ALL DATA" button in Settings will now be disabled for 5 seconds upon opening the final confirmation dialog to prevent accidental wipes.

## Proposed Changes

### UI Components

#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Update `GlassCard` and `SquareGlassCard` to use a `0.5.dp` white border with `0.2f` alpha.
- Replace static white background for "Apple Liquid Glass" style with a `Brush.verticalGradient` for realistic lighting.
- Ensure smooth transitions and high-quality frosted look.

### Main Screen & Wallpaper

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)
- Refine the `apple_liquid_glass` background brush.
- Create a `Dynamic Accent Wallpaper` using `accentColor`, a dynamically calculated darker shade, and a soft cream/white tone.
- Use a large `RadialGradient` for a professional wallpaper feel.

### Settings & Safety

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Implement a 5-second countdown for the `showWipeDialogStep2` confirmation dialog.
- Disable the "WIPE EVERYTHING" button until the countdown finishes.
- Update button text to reflect remaining time (e.g., "Yes, Delete Everything (5s)").

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure compilation.

### Manual Verification
- Verify the "Liquid Glass" look in `@Preview` (if available) or by inspecting the code logic.
- Verify the dynamic wallpaper logic in `MainScreen.kt`.
- Verify the 5-second delay in `SettingsScreen.kt`.
