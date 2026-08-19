ZPower 🪫 
Documentation
ZPower is a high-end, Industrial Asset Management Tool built on an Offline-First philosophy. Unlike standard note-taking apps, it is engineered specifically to organize complex machinery, thermal areas, and heavy industrial data.
💎 1. Core Logic: Parent-Child Hierarchy (6+ Levels)
Industrial setups operate strictly in levels (e.g., Plant > Room > Panel > Relay). ZPower follows this exact architecture:
Thermal Area (Root)
Room
Panel
Relay
Child Process
Sub-Process (Infinite): Beyond Level 6, you can nest data infinitely deep (Level 7, 8, 9, etc.).
Why it matters: It ensures you can locate even the smallest relay or component precisely mapped under its designated parent environment within a massive plant.
🛠️ 2. Key Functions & Features
🖼️ High-Resolution Visuals
Full-Screen Image View: Tap any card's image to view it full-screen in high resolution with pinch-to-zoom support.
2-Second Long-Press Preview: Long-press any card for 2 seconds to inspect complete item details inside a floating "Bubble Glass" pop-up without entering the node.
Navigation: Tap directly on the card's text area to navigate inside that node.
WebP Compression: Automatically converts all camera captures and gallery uploads into WebP format—preserving original visual quality while cutting file sizes by up to 90% (e.g., shrinking a 5MB image down to ~200KB).
🧠 Smart Data Entry
Deep Branch Autofill: If you create "Panel 1" containing 50 relays, you can create "Panel 2," select "Panel 1" from suggestions, and tap Full Branch. ZPower instantly clones the entire sub-tree—all 50 relays and their nested parameters—into Panel 2.
Undo/Redo: Correct typing mistakes directly in the Add/Edit window using dedicated Undo/Redo controls in the bottom-left corner.
Batch Entry (+ Queue): Queue multiple items quickly by tapping +, then commit everything simultaneously using Save All.
🔍 Search & Discovery
Contextual Search: Selecting any item from search results takes you directly to its exact parent folder alongside all sibling entries.
Full Path Visibility: Displays the complete operational breadcrumb path directly in results (e.g., Plant > Room A > Panel 5).
📦 3. Data & Cloud Ecosystem (Telegram)
📂 Local Mirroring
The app designates the local directory Documents/Gold Knowledge/ as its Master Mirror.
Any in-app edit immediately writes to the local data.json file.
Modifying data.json externally via PC and tapping Refresh inside the app instantly reloads the updated structure.
☁️ Telegram Cloud Sync
Personal/Group Backup: Packages the entire environment into a compressed ZIP file and delivers it to your linked Telegram Bot or Group with one tap.
Selective Restore: Choose between a complete restore or a text-only recovery during ZIP imports (text-only imports complete almost instantaneously).
Secure API: API tokens remain encrypted and locked. You can strip API credentials during export to prevent unauthorized bot access.
🎨 4. Liquid Glass UI Customization
Designed to move away from rigid, utilitarian industrial interfaces:
3-Point Gradient: Fully customizable 3-stop dynamic gradient background.
Custom Wallpaper & Dimmer: Import custom gallery backgrounds and control background contrast using a dedicated dimming slider.
Apple-Style Glass: Real-time physical glass materials featuring authentic reflections, subtle internal glow, and backdrop-blur effects.
Dynamic Glass Text Color: Adaptive text coloring to maintain contrast across light and dark custom themes.
🛡️ 5. Why ZPower Stands Out
Complete Privacy: Operates with zero third-party telemetry or cloud servers. All assets exist strictly on your local device (Gold Knowledge directory) or inside your private Telegram instance.
True Portability: Copy the Gold Knowledge folder to any new device to resume work instantly without migration hurdles.
High-Performance Architecture: Optimized local database combined with WebP compression ensures zero UI lag, even across databases containing 10,000+ items.
Accident Prevention: Destructive actions like "Delete All Data" are safeguarded behind a strict 5-second hold-to-confirm safety lock.
Developer: @zedxrp
Support Email: abdullahexpain@gmail.com
Donation: paypal.me/abdullahexplain# zpower


