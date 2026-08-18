# Walkthrough - Universal Image Support and Batch Entry

Implemented universal image support across all hierarchy levels and added batch-entry logic to the dialogs.

## Changes Made

### 1. Database & Entities
- Added `imagePath: String? = null` to `ThermalArea`, `RoomEntity`, `PanelEntity`, and `RelayEntity`.
- Incremented database version to `2` and enabled destructive migration for easy transition.

### 2. Universal Image Picker
- Created `ImageSourceSelector` dialog in `GlassComponents.kt` with "Camera" and "Gallery" options.
- Integrated `ActivityResultContracts.TakePicture` and `PickVisualMedia` in both `HierarchyEntityDialog` and `ChildProcessDialog`.
- All images are processed and compressed to WebP via `DatabaseRepository`.

### 3. Batch Entry ("Add More") Logic
- Updated `HierarchyEntityDialog` and `ChildProcessDialog` to maintain a temporary list of items.
- Added "Add Another Item" button to both dialogs.
- "Save All" button inserts all pending items into the database at once.

### 4. UI Enhancements
- Updated `GlassCard` to display the level-specific image as a blurred background if available.
- Updated all list screens (`ThermalAreaListScreen`, `RoomListScreen`, etc.) to pass the `imagePath` to `GlassCard`.

## Verification Results

### Build
- Successfully built with `./gradlew :app:assembleDebug`.

### UI Components
- `GlassCard` now supports background images.
- Dialogs now support multiple entries and image selection from both camera and gallery.
