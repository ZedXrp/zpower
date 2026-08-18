# Project Plan

Hierarchical Industrial Asset Management Dashboard for a Thermal Area.
The app follows a 4-level hierarchy: Thermal Area -> Rooms -> Panels -> Relays -> Child Processes.
Each Child Process contains Name, Image, Description, Work, and Uses.

Key Features:
- Offline-first: Local data persistence.
- View Mode: Hierarchical navigation.
- Edit Mode: CRUD operations (Add, Edit, Delete) for all levels.
- Global Search: Search across all levels and navigate to results.
- Breadcrumb Navigation: For easy upward navigation.
- Import/Export: JSON file based data portability.
- UI Design: Dark mode with 'Liquid Glass' (Glassmorphism) effect. Semi-transparent backgrounds, frosted glass cards, glowing accents.
- No internet requirement.
- Adaptive UI for phone and tablet.

## Project Brief

# Project Brief: ZPower Industrial Asset Management

## Features (MVP)
*   **Deep Hierarchical Navigation**: Navigate a five-level industrial structure (Thermal Area → Room → Panel → Relay → Child Process) with persistent breadcrumb support for easy upward traversal.
*   **Offline-First Asset Management**: Full local CRUD (Create, Read, Update, Delete) capabilities for all hierarchical levels, ensuring engineers can manage assets in network-isolated industrial environments.
*   **Global Asset Search**: A unified search engine that scans across all levels of the hierarchy, allowing users to jump directly to specific assets or processes from anywhere in the app.
*   **JSON Data Portability**: Import and export functionality for the entire asset database using JSON files, enabling easy backups and data sharing between devices without an internet connection.

## High-Level Tech Stack
*   **Kotlin & Coroutines**: Core language and framework for business logic, asynchronous data operations, and reactive state management.
*   **Jetpack Compose**: Modern declarative UI toolkit used to implement the 'Liquid Glass' (Glassmorphism) design system.
*   **Jetpack Navigation 3**: A state-driven navigation architecture providing granular control over the hierarchical backstack.
*   **Compose Material Adaptive**: Implementation of adaptive UI patterns (like List-Detail and Supporting Pane) to optimize the dashboard for both phone and tablet form factors.
*   **Room Persistence**: Local SQLite-based database for robust, offline-first data storage of industrial assets.

## Implementation Steps

### Task_1_DataLayer: Set up Room database with 5-level hierarchy entities (Thermal Area, Room, Panel, Relay, Child Process) and implement JSON Import/Export logic.
- **Status:** COMPLETED
- **Updates:** Set up Room database with 5-level hierarchy entities (Thermal Area, Room, Panel, Relay, Child Process).
- **Acceptance Criteria:**
  - Room entities and DAOs implemented for all 5 levels
  - JSON Import/Export utility functional
  - Database builds and supports CRUD operations

### Task_2_NavigationAndAdaptiveShell: Implement Navigation graph for hierarchical traversal, breadcrumb support, and Adaptive Scaffold (List-Detail) for phone and tablet compatibility.
- **Status:** COMPLETED
- **Updates:** Implemented Jetpack Navigation with type-safe routes for all 5 levels.
- **Acceptance Criteria:**
  - Navigation between levels works (Thermal Area -> ... -> Child Process)
  - Breadcrumbs correctly reflect the current hierarchy path
  - Adaptive UI adjusts layout for phone vs tablet

### Task_3_HierarchyCRUDAndSearch: Develop UI for Thermal Area, Room, Panel, and Relay management with CRUD operations and implement the Global Search feature.
- **Status:** COMPLETED
- **Updates:** Replaced placeholders with functional list screens for Thermal Area, Room, Panel, and Relay.
- **Acceptance Criteria:**
  - CRUD screens/dialogs for first 4 levels functional
  - Global Search scans and displays results across all levels
  - Integration with Room database completed for these levels

### Task_4_ChildProcessAndGlassmorphism: Implement detailed Child Process UI (Name, Image, Description, Work, Uses) and apply Liquid Glass (Glassmorphism) dark mode theme globally.
- **Status:** COMPLETED
- **Updates:** Implemented detailed Child Process view with Name, Image (Coil), Description, Work, and Uses.
- **Acceptance Criteria:**
  - Child Process CRUD with image support implemented
  - Glassmorphism styling applied to all UI components
  - Dark mode is the primary theme

### Task_5_RunAndVerify: Final application run to verify stability, alignment with user requirements, and UI consistency.
- **Status:** COMPLETED
- **Updates:** Verified that the project builds successfully via assembleDebug.
- **Acceptance Criteria:**
  - App builds and runs without crashes
  - All features (Search, Breadcrumbs, Import/Export, Hierarchy) are functional
  - Existing tests pass
  - Final UI review matches Industrial Dashboard requirements

### Task_6_FeatureEnhancements: Implement Image Management (Photo Picker & 'ZPower' storage), Zip Import/Export (JSON + Images), Hierarchical Search paths, and Autofill logic.
- **Status:** COMPLETED
- **Updates:** Implemented image picking via Photo Picker and local persistence in 'ZPower/images'.
- **Acceptance Criteria:**
  - Images picked via Photo Picker are stored locally in the 'ZPower' folder
  - Zip utility correctly bundles/extracts JSON data and images
  - Search results display full hierarchical paths (Thermal Area > Room > ...)
  - Add/Edit dialogs provide autofill suggestions based on existing data

### Task_7_UIRefinementAndFinalVerify: Refine 'Liquid Glass' UI with vibrant Material 3 accents and perform a final run to verify stability and feature alignment.
- **Status:** COMPLETED
- **Updates:** Final UI refinement and stability check completed.
- **Acceptance Criteria:**
  - UI theme is refined with vibrant Liquid Glass effects and Material 3 components
  - App builds and runs without crashes
  - All new enhancements (Images, Zip, Search Paths, Autofill) are verified
  - Existing tests pass and core functionality is preserved

### Task_10_CriticalFixesAndStorage: Fix camera URI/FileProvider crashes, repair Zip import/export logic, and relocate storage path to 'Gold Knowledge' directory with DataStore persistence for settings.
- **Status:** COMPLETED
- **Updates:** Fixed camera crashes by correctly configuring FileProvider and URI generation.
- **Acceptance Criteria:**
  - Camera capture works without crashes using proper FileProvider logic
  - Zip import/export bundling and restoration logic is fixed and functional
  - All app data and images are relocated to and managed within the 'Gold Knowledge' folder
  - DataStore is implemented for persistent UI settings

### Task_11_UICustomizationAndFinalVerify: Implement rectangular cards, full-screen viewer, blur/color/size customization, and dynamic titles. Perform final run and verify stability.
- **Status:** COMPLETED
- **Updates:** Updated UI to use rectangular cards with adjustable sizes (Small, Medium, Large).
- **Acceptance Criteria:**
  - UI updated with rectangular cards and full-screen image viewer overlay
  - Settings screen supports blur intensity, custom accent colors, card resizing, and title renaming
  - Adaptive theme dynamically responds to all UI settings
  - App builds and runs without crashes; critic_agent verifies requirement alignment
  - Make sure all existing tests pass and app does not crash

### Task_12_UIOverhaulAndPermissions: Transition all list screens to a 2-column square LazyVerticalGrid, implement responsive square cards with text overlays, and handle CAMERA permissions with 'Gold Knowledge' folder setup.
- **Status:** COMPLETED
- **Updates:** Transitioned all hierarchy list screens to a 2-column square grid layout using LazyVerticalGrid.
- **Acceptance Criteria:**
  - All list screens updated to 2-column square grid layout
  - Cards feature square aspect ratio and text overlays
  - CAMERA permission management and 'Gold Knowledge' folder creation implemented
  - Full-screen image viewer with close button is functional

### Task_13_AdvancedCustomizationAndBackup: Implement title renaming, grid-based color picker, background textures, and dual-mode Zip backup (Complete vs Data Only). Run final verification.
- **Status:** COMPLETED
- **Updates:** Implemented dynamic title renaming for the root hierarchy level.
- **Acceptance Criteria:**
  - Title editor for 'Home' level (Thermal Area) implemented in Settings
  - Advanced grid color selector and industrial background textures integrated
  - Zip utility rewritten with popup for 'Complete Backup' vs 'Data Only'
  - Preferences DataStore expanded and WebP compression maintained
  - App builds successfully, all existing tests pass, and app does not crash
  - Critic_agent verifies stability and alignment with requirements

### Task_14_UIAndUXRefinement: Refine card layout (buttons top, data below), increase dialog sizes, fix Settings cursor bug, refine adaptive layout, and implement Undo/Redo.
- **Status:** COMPLETED
- **Updates:** Redesigned SquareGlassCard: Edit (top-left), Delete (top-right), Text placed below image.
- **Acceptance Criteria:**
  - Edit button top-left, Delete button top-right on cards
  - Data (Name/Description) placed below card images
  - Delete confirmation dialog implemented
  - Add/Edit dialog sizes increased
  - Settings cursor jumping fixed
  - Adaptive List-Detail layout refined for bottom left/right positioning
  - Undo/Redo mechanism functional for item changes

### Task_15_StabilityStorageAndVerify: Overhaul Zip utility for stability, implement public 'Gold Knowledge' folder with Scoped Storage permissions, and perform final run and verify.
- **Status:** COMPLETED
- **Updates:** Overhauled Zip utility using Zip4j for robust, crash-free backups.
- **Acceptance Criteria:**
  - Zip utility overhauled for robust stream handling and stability
  - 'Gold Knowledge' folder managed in user-accessible public storage (e.g., Documents)
  - Storage permissions handled correctly for Scoped Storage
  - App builds successfully and runs without crashes
  - All existing tests pass
  - Critic_agent verifies stability and requirement alignment

### Task_16_PublicStorageAndZipStability: Relocate 'Gold Knowledge' storage to public Documents folder, handle storage permissions, and ensure Zip4j stability.
- **Status:** COMPLETED
- **Updates:** Enforced Documents/Gold Knowledge as the primary storage location.
- **Acceptance Criteria:**
  - Folder moved to Environment.DIRECTORY_DOCUMENTS
  - READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, and MANAGE_EXTERNAL_STORAGE handled
  - Zip4j operations use streams, Dispatchers.IO, and robust try-catch

### Task_17_UIUXRefinement: Reposition Undo/Redo buttons and enlarge Add/Edit dialogs for better industrial UX.
- **Status:** COMPLETED
- **Updates:** Repositioned Undo/Redo to the bottom-left of all data-entry dialogs.
Enlarged dialogs to 95% width for industrial-grade data entry.
Ensured all Settings labels use high-visibility LightBrown accents.
Improved SquareGlassCard layout with better text spacing and visibility.
- **Acceptance Criteria:**
  - Undo/Redo buttons moved to bottom-left of the action bar
  - Add/Edit dialogs enlarged for visibility
  - Material 3 layout spacing refined
- **Duration:** N/A

### Task_18_TelegramMultiSyncAndSecurity: Implement Telegram Multi-Sync (sequential sendDocument with file_id), Security (Hidden Bot API with password unlock), and contextual filtering.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Multi-recipient sync implemented using OkHttp and file_id mapping
  - Bot API field hidden with 'Locked' status; unlocked via '@#Abdullah542543'
  - DataStore updated for recipient list and group forwarding preferences
  - Import via Link features 'Check Link' metadata logic
  - Telegram Username included in captions and thumbnails shown in lists
  - API_KEY integration is functional and secure
- **StartTime:** 2026-08-17 20:26:46 IST

### Task_19_RunAndVerify: Final application run to verify Telegram features, storage integrity, and overall stability.
- **Status:** PENDING
- **Acceptance Criteria:**
  - App builds successfully and does not crash
  - Multi-recipient sync verified for group and personal IDs
  - Security lock correctly protects custom Bot API settings
  - Recent backups forwarding discovered via getUpdates
  - 'Gold Knowledge' folder integrity maintained across all operations
  - All existing industrial dashboard tests pass

