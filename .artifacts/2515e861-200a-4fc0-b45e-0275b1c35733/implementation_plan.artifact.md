# Implementation Plan - Universal Image Support and Batch Entry

This plan outlines the steps to implement universal image support (Camera/Gallery) across all hierarchy levels and add batch-entry logic to the dialogs.

## User Review Required

> [!IMPORTANT]
> The database schema will be updated, adding `imagePath` to several entities. Existing data will have `null` for these fields.
> A `FileProvider` will be added to `AndroidManifest.xml` to support the Camera.

## Proposed Changes

### 1. Database Schema Update
#### [MODIFY] [Entities.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/entity/Entities.kt)
- Add `val imagePath: String? = null` to `ThermalArea`, `RoomEntity`, `PanelEntity`, and `RelayEntity`.

### 2. Camera Support (Infrastructure)
#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/AndroidManifest.xml)
- Add `<provider>` for `androidx.core.content.FileProvider`.
#### [NEW] [file_paths.xml](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/res/xml/file_paths.xml)
- Define paths for `FileProvider`.

### 3. Universal Image Picker & Repository
#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- Ensure `saveImageToInternalStorage` handles both Uri and potentially raw Bitmaps if needed. (Actually Uri is fine).
- Refine WebP compression.
#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Create `ImageSourceSelector` dialog.
- Update `GlassCard` to display `imagePath`.

### 4. Batch Entry Logic
#### [MODIFY] [HierarchyEntityDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/HierarchyEntityDialog.kt)
- Update to support images.
- Add "Add Another" button and maintain a list of items.
- Change `onConfirm` to return `List<Pair<String, String?>>` (name, description, imagePath?). Actually, maybe a custom data class.
#### [MODIFY] [ChildProcessDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/ChildProcessDialog.kt)
- Use `ImageSourceSelector`.
- Add "Add Another" button and maintain a list of items.
- Change `onConfirm` to return `List<ChildProcess>`.

### 5. Wiring it up in NavigationViewModel / Screens
- Since the dialog signatures change, I need to find where they are used and update them to handle lists.
- Usually `NavigationViewModel` handles the actual database insertion.

## Verification Plan

### Automated Tests
- N/A (Unit tests for repository if available, but primarily manual UI verification)

### Manual Verification
1. Open "Add Thermal Area" dialog.
2. Select an image via Gallery.
3. Click "Add Another".
4. Add another area without an image.
5. Click "Confirm" and verify both are added.
6. Open a Relay and add a "Child Process" using the Camera.
7. Verify images are displayed in `GlassCard`.
