# Implementation Plan - ZPower Refinement

This plan details the implementation of recursive hierarchy, long-press card previews, and the "Industrial Border Glow" background style.

## Proposed Changes

### 1. Hierarchy Refinement (Recursive Groups)
Ensure that sub-processes can be added indefinitely and the "Add" button is more accessible.

#### [MODIFY] [SubProcessListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SubProcessListScreen.kt)
- Make the "Add Sub-Process" FAB always present (remove `isEditMode` check if requested, or keep it if "always present" implies it should be clearly visible in its role). *Correction*: User says "always present", so I will remove the `isEditMode` check for the FAB here, but keep the dialog logic.
- Ensure the dialog correctly uses the current parent.

### 2. Long-Press Card Preview
Implement a 2-second long-press gesture to show a full-screen preview.

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Add state to hold the card data currently being previewed: `previewCard: PreviewData?`.
- Add functions `showPreview(data)` and `dismissPreview()`.

#### [MODIFY] [SquareGlassCard.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Implement `combinedClickable` or `pointerInput` with `detectTapGestures` on the card.
- Trigger `showPreview` after 2 seconds of long-press.
- Pass the necessary data (name, description, imagePath) to the preview.
- Apply "Border Glow" style to the card border if selected in settings.

#### [NEW] [CardPreviewOverlay.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/CardPreviewOverlay.kt)
- A full-screen overlay that shows the card details (Image + Name + Description).
- Blurred background.
- Scaled up UI.
- Close button.

### 3. "Industrial Border Glow" Background Style
Add a new background style with a black center and glowing accent-colored edges.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Add "Industrial Border Glow" to the background style options.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)
- Implement the "Industrial Border Glow" logic in the background layer.
- Integrate the `CardPreviewOverlay` to show when `viewModel.previewCard` is not null.

## Verification Plan

### Automated Tests
- N/A (UI focused changes)

### Manual Verification
1.  **Hierarchy**: Navigate to a Sub-Process, view its sub-processes, and add a new one. Repeat to verify infinite depth.
2.  **Long-Press**: Long-press a card in any list for 2 seconds. Verify that the full-screen preview appears with a blurred background.
3.  **Border Glow**: Go to Settings, select "Industrial Border Glow". Verify that the background changes to black with glowing edges.
4.  **Card Polish**: Verify that cards have a glow effect when the "Industrial Border Glow" theme is active.
