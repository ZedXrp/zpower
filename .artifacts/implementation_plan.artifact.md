# Refine ZPower Telegram and Settings Features

This plan outlines the changes required to refine the Telegram and Settings features, including security enhancements, UI updates, and workflow improvements.

## Proposed Changes

### [Settings & Repository]

#### [MODIFY] [SettingsRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/SettingsRepository.kt)
- Add `INCLUDE_BOT_API_IN_BACKUP` (Boolean, default `false`).
- Add `IS_CUSTOM_CONFIG_ENABLED` (Boolean, default `false`).
- Ensure `IS_TELEGRAM_ENABLED` default is `false`.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Rename "Community Discovery" -> **"Import backup from Telegram user"**.
- Add a **"Custom Configuration"** toggle at the bottom.
- Implement a 4-second confirmation popup when enabling "Custom Configuration".
- Restrict Bot API and Group settings editing unless "Custom Configuration" is active.
- Add **"Protect Bot API in Backups"** toggle (inverted logic for `includeBotApiInBackup`).

### [Telegram & Backup Logic]

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- Update `exportBackup`: If `COMPLETE` and `includeBotApiInBackup` is `false`, scrub `telegramBotToken` from the exported preferences.
- Update `fetchBackupByLink`: Fetch only metadata (Name, Size, Date) first.
- Handle Chat ID `4408376069` prefixing for groups (e.g., prepend `-100`).

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Update `fetchBackupByLink` to return metadata instead of importing immediately.
- Manage the state for the new settings.

#### [MODIFY] [ZPowerTopBar.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/ZPowerTopBar.kt)
- Implement "Join Group" prompt dialog before proceeding with "Send to Telegram Bot".

## Verification Plan

### Automated Tests
- N/A (Manual UI verification preferred for these specific UX changes).

### Manual Verification
1.  Verify `isTelegramEnabled` is false by default on new install/reset.
2.  Test "Custom Configuration" toggle: confirm 4-second delay and popup.
3.  Check that Bot API settings are locked until "Custom Configuration" is on.
4.  Export a "COMPLETE" backup with "Protect Bot API" ON, then import it on another device (or check the zip content if possible) to ensure the token is missing.
5.  Test `fetchBackupByLink`: confirm it adds to the list instead of immediate import.
6.  Verify Chat ID `4408376069` works for Telegram uploads.
7.  Check "Join Group" prompt appears in `ZPowerTopBar`.
