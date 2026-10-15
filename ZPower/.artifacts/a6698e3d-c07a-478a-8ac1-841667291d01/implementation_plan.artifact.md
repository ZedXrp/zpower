# Implementation Plan - Search Navigation & Card Preview Improvements

This plan outlines the changes to improve search navigation (drilling one level inside) and enhance the card long-press preview experience.

## User Review Required

> [!IMPORTANT]
> The search navigation for `child_process` and `sub_process` will now navigate directly to their respective `SubProcessList` instead of their detail screens, to satisfy the "Drill 1 Level Inside" requirement.

## Proposed Changes

### Navigation Improvements

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Update `navigateToSearchResult` to implement "Drill 1 Level Inside" for `child_process` and `sub_process`.
- For `child_process`, it will navigate to its child `SubProcessList`.
- For `sub_process`, it will navigate to its child `SubProcessList`.
- Ensure hierarchy is preserved in breadcrumbs.

### Card Preview Improvements

#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Update `SquareGlassCard` long-press detection to ~2.5 seconds.
- Ensure `onLongPress` and `onRelease` are correctly wired.

#### [MODIFY] [CardPreviewOverlay.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/CardPreviewOverlay.kt)
- Make background darker and more blurred.
- Display full Name, Title, and Description.
- Add a prominent, styled floating Close Button (✕) in the top-right corner.
- Implement `BackHandler` for clean dismissal.
- Ensure tap-outside dismisses the preview.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew assembleDebug` to ensure no compilation errors.

### Manual Verification
1. **Search Navigation**:
   - Search for a Child Process and click it. Verify it opens the `SubProcessList` for that process.
   - Search for a Sub-Process and click it. Verify it opens its own `SubProcessList`.
   - Verify breadcrumbs allow navigating back up the hierarchy.
2. **Card Long Press**:
   - Long press a card for ~2.5 seconds. Verify the preview overlay appears.
   - Verify the overlay is full-screen with a blurred dark background.
   - Verify all details (image, name, description) are shown.
   - Verify the Close button works.
   - Verify tap-outside and back-press work.
