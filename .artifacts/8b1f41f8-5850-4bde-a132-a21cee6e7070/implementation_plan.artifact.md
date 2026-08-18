# Implementation Plan - ZPower Customization & Backup

Complete the ZPower customization features (Title renaming, Advanced color picker, Background textures) and implement dual-mode ZIP backup.

## User Review Required

> [!IMPORTANT]
> The "Complete Backup" will include the app settings (DataStore). Restoring a complete backup will overwrite current app settings like accent color, background style, etc.

## Proposed Changes

### 1. Data and Repository Layer

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- Add `BackupType` enum: `COMPLETE`, `DATA_ONLY`.
- Update `exportDatabaseToZip` to include the DataStore file (`settings.preferences_pb`) when `BackupType.COMPLETE` is selected.
- Update `importDatabaseFromZip` to detect and restore the DataStore file if present in the ZIP.

### 2. Navigation & ViewModel Layer

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Add `exportBackup` method that takes `BackupType`.
- Ensure settings are properly observed and updated.

### 3. UI - Customization Features

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Replace the single row of colors with a grid of vibrant accent colors (8-10 options).
- Add specific background style options: "Liquid Glass Gradient", "Brushed Metal Dark", "Digital Grid".
- Ensure "Root Level Title" text field is functional (it seems to be already, but I'll double check).

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)
- Implement the rendering logic for the new background textures:
    - **Liquid Glass Gradient**: A rich, animated-like linear/radial gradient.
    - **Brushed Metal Dark**: A dark background with a subtle noise/pattern overlay.
    - **Digital Grid**: A futuristic tech-grid pattern drawn using `Modifier.drawBehind`.

### 4. UI - Backup & Polish

#### [MODIFY] [ZPowerTopBar.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/ZPowerTopBar.kt)
- Show an `AlertDialog` when "Export ZIP" is clicked to choose between "Complete Backup" and "Data & Images Only".

#### [MODIFY] [Theme.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/theme/Theme.kt)
- Ensure `LightBrown` is used consistently for text on dark backgrounds.
- Add vibrant color constants to `Color.kt`.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure compilation.

### Manual Verification
- Test Title Renaming: Change title in Settings, verify TopBar and Breadcrumbs update.
- Test Color Grid: Select different accent colors, verify UI updates.
- Test Backgrounds: Switch between the 3 industrial styles, verify visual appearance.
- Test Dual-Mode Backup:
    - Export "Data Only", import on fresh install -> check if data is back but settings are default.
    - Export "Complete", import on fresh install -> check if data and settings (accent color etc.) are restored.
- Verify `CAMERA` flow stability by checking the code and ensuring no obvious crashes in logic.
- Verify square grid cards (2 per row) display `LightBrown` text.
