# Refine ZPower with Telegram personal filtering and Apple Liquid Glass UI

This plan outlines the updates to ZPower, including refined Telegram backup filtering, a new "Apple Liquid Glass" background style, and UI improvements.

## Proposed Changes

### [Component] Data Repository

#### [MODIFY] [DatabaseRepository.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/data/repository/DatabaseRepository.kt)
- Update `fetchBackupListFromTelegram` to filter updates based on the sender's or forwarder's User ID (`from.id`) matching the configured `telegramChatId`.
- Ensure only ZIP files from the current user are displayed in the "Recent Backups" list.

### [Component] UI Screens & Components

#### [MODIFY] [MainScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/MainScreen.kt)
- Add `"apple_liquid_glass"` background style.
- Implement a pastel gradient (light blue, pink, white) for the liquid glass effect.
- Apply a heavy blur (30.dp) when this style is selected.

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/screens/SettingsScreen.kt)
- Add "Apple Style" option to the Background Style selection.
- Add user instruction tip for finding Telegram User ID using `@missrose_bot`.
- Update `TutorialSection` if necessary to include the new tip.

#### [MODIFY] [GlassComponents.kt](file:///C:/Users/Admin/AndroidStudioProjects/ZPower/app/src/main/java/com/app/zpower/ui/components/GlassComponents.kt)
- Update `GlassCard` and `SquareGlassCard` to accept `backgroundStyle`.
- Refine card styling for `"apple_liquid_glass"`:
    - Use more subtle borders.
    - Adjust background transparency.
    - Soften shadows or highlights.

## Verification Plan

### Manual Verification
1. **Telegram Filtering**:
   - Send/forward a ZIP backup to the bot from the configured User ID.
   - Send/forward a ZIP backup from a different account (if possible) or observe group updates.
   - Verify that only the backup from the configured User ID appears in the app's "Recent Backups" list.
2. **Apple Liquid Glass Background**:
   - Select "Apple Style" in Settings.
   - Verify the background looks like frosted glass with pastel colors.
   - Verify text contrast remains high (using `LightBrown`).
3. **Card UI**:
   - Verify cards have a more subtle "liquid glass" look when the style is selected.
4. **Settings Tip**:
   - Verify the instruction tip is visible in the Telegram settings section.
