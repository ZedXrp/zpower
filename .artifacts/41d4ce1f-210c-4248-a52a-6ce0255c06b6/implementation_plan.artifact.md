# Deep Autofill and Automated Telegram Fetch

This plan outlines the enhancements to ZPower, including a recursive hierarchy import (Deep Autofill) and an automated UI for fetching backups from Telegram.

## User Review Required

> [!IMPORTANT]
> The "Full Branch" option in Deep Autofill will recursively copy all nested items. This could lead to a large number of database entries if performed on high-level entities (like Thermal Areas).

## Proposed Changes

### Data Layer

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- Implement `duplicateHierarchyBranch(sourceId: Long, sourceType: String, newParentId: Long, newParentType: String)` to recursively copy children entities.
- Ensure all IDs are re-mapped correctly and circular references are avoided.

#### [MODIFY] [Entities.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/entity/Entities.kt)
- Add `@Ignore` fields `sourceId` and `fullBranch` to entities (`ThermalArea`, `RoomEntity`, `PanelEntity`, `RelayEntity`, `ChildProcess`, `SubProcess`) to track deep copy requests.

### UI Layer

#### [MODIFY] [HierarchyEntityDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/HierarchyEntityDialog.kt)
- Update `HierarchyItem` data class to include `sourceId`.
- Update suggestion selection to show a confirmation dialog: "Deep Autofill: Include all sub-items?".
- Implement "Data Only" and "Full Branch" options.

#### [MODIFY] [ChildProcessDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/ChildProcessDialog.kt)
- Similar update as `HierarchyEntityDialog` for `ChildProcess`.

#### [MODIFY] [SubProcessDialog.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/SubProcessDialog.kt)
- Similar update as `HierarchyEntityDialog` for `SubProcess`.

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Add `showTelegramBackupDialog: StateFlow<Boolean>`.
- Update `loadTelegramBackups()` to set `showTelegramBackupDialog` to true upon success.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Observe `showTelegramBackupDialog` and automatically open the restore dialog.
- Show "Checking Telegram..." indicator during fetch.

### Screen Logic

#### [MODIFY] [ThermalAreaListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/ThermalAreaListScreen.kt)
- Call `repository.duplicateHierarchyBranch` if `fullBranch` is requested.

#### [MODIFY] [RoomListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/RoomListScreen.kt)
- Call `repository.duplicateHierarchyBranch` if `fullBranch` is requested.

#### [MODIFY] [PanelListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/PanelListScreen.kt)
- Call `repository.duplicateHierarchyBranch` if `fullBranch` is requested.

#### [MODIFY] [RelayListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/RelayListScreen.kt)
- Call `repository.duplicateHierarchyBranch` if `fullBranch` is requested.

#### [MODIFY] [ChildProcessListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/ChildProcessListScreen.kt)
- Call `repository.duplicateHierarchyBranch` if `fullBranch` is requested.

#### [MODIFY] [SubProcessListScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SubProcessListScreen.kt)
- Call `repository.duplicateHierarchyBranch` if `fullBranch` is requested.

## Verification Plan

### Automated Tests
- Unit test for `duplicateHierarchyBranch` to verify recursive copying and ID re-mapping.
- UI test for suggestion selection and Deep Autofill confirmation dialog.

### Manual Verification
- Verify that selecting "Full Branch" correctly copies all nested sub-items.
- Verify that Telegram fetch automatically pops up the dialog.
- Verify the "Checking Telegram..." indicator.
