# Refine ZPower Feedback Implementation Plan

This plan addresses user feedback regarding search navigation context, card UI enhancements, undo/redo button placement, and long-press interaction fixes.

## Proposed Changes

### [Navigation & Search]

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Update `navigateToSearchResult` to navigate to the parent's list screen.
  - Room -> ThermalArea's RoomList.
  - Panel -> Room's PanelList.
  - Relay -> Panel's RelayList.
  - ChildProcess -> Relay's ChildProcessList.
  - SubProcess -> Parent's SubProcessList.

### [UI Components]

#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- **SquareGlassCard**:
  - Increase Edit/Delete icon size to 36dp.
  - Change icon background to a transparent glass effect (`Color.White.copy(alpha = 0.1f)`).
  - Fix long-press detection using `detectTapGestures`.
  - Add a nearly transparent "Preview" (eye) icon in the center of the image as a fallback.
  - Increase the height of the text section below the image.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)
- Remove global Undo/Redo buttons from the bottom left.

#### [MODIFY] [HierarchyEntityDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/HierarchyEntityDialog.kt)
#### [MODIFY] [ChildProcessDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/ChildProcessDialog.kt)
#### [MODIFY] [SubProcessDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/SubProcessDialog.kt)
- Ensure Undo/Redo buttons are correctly positioned in the bottom-left of the dialog's action row. (They are already there, but I will double-check the styling/spacing).

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to ensure compilation.

### Manual Verification
1. **Search**: Search for a Room, click it, and verify it navigates to the Room list of its area, showing the room among siblings.
2. **Card UI**: Check `SquareGlassCard` edit/delete icons for size and glass background. Verify text section height.
3. **Undo/Redo**: Verify buttons are gone from `MainScreen` and present in dialogs.
4. **Long-Press**: Test long-press on cards to trigger preview. Check the new "Eye" icon.
