# Implementation Plan - Refine API Key handling for Sharing and Importing

Refine ZPower's API Key handling by implementing selective settings import (Smart Import), a toggle for including API keys in exports, and robust scrubbing logic.

## User Review Required

> [!IMPORTANT]
> The "Smart Import" logic requires a new dialog when a backup containing a Telegram Bot Token is imported. This will pause the import process for user input.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [SettingsRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/SettingsRepository.kt)
- Update `PreferencesKeys` to use `share_bot_api_in_backup` instead of `include_bot_api_in_backup`.
- Update methods to reflect the naming change (`shareBotApiInBackup`).

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- Refactor `importDatabaseFromZip` to handle the settings file separately.
- Add `processSettingsImport` logic:
    - Extract settings to a temporary file.
    - Check for `telegram_bot_token` in the extracted file.
    - Implement a way to merge settings while optionally excluding the bot token.
- Ensure scrubbing logic in `exportDatabaseToZip` uses the correct key.

---

### [UI/Logic Layer]

#### [MODIFY] [NavigationViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/navigation/NavigationViewModel.kt)
- Add state for `ImportApiKeyChoiceDialog` (e.g., `showImportApiKeyDialog`, `pendingSettingsFile`).
- Handle the user's choice ("Keep Current" vs "Replace").
- Update `importDataFromZip` and `importSpecificTelegramBackup` to support the new flow.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Add "Include API Key in Exports" toggle in the Telegram section.
- Implement the `ImportApiKeyChoiceDialog` UI.

## Verification Plan

### Automated Tests
- N/A (Manual verification on device is preferred for ZIP/Storage logic).

### Manual Verification
1. **Export with/without API Key**:
    - Toggle "Include API Key in Exports" ON, export ZIP, verify token is present in `settings.preferences_pb` inside ZIP.
    - Toggle "Include API Key in Exports" OFF, export ZIP, verify token is ABSENT.
2. **Smart Import**:
    - Import a ZIP with a token when the app already has one -> Verify dialog appears.
    - Choose "Keep Current" -> Verify all settings except the token are restored.
    - Choose "Replace" -> Verify all settings including the token are restored.
    - Import a ZIP without a token -> Verify current token is kept.
