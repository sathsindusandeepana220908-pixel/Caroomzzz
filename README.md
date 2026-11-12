# Caroomzzz
A lightweight native Android carrom game (Android 8.0+, API 26+) with offline practice, local AI and Wi-Fi LAN multiplayer.

## LAN setup
1. On phone A, enable Android hotspot and tap **Host LAN**.
2. On phone B, connect to that hotspot and tap **Join LAN**. Use **Find host** or enter the host IP shown on phone A.
3. The host is authoritative for turn order and physics. Both phones receive the same board state.
4. Bluetooth is not included in this first build; Wi-Fi Direct/hotspot LAN is the priority and works without internet.

## Build
GitHub Actions builds a debug APK on every push. Download **Caroomzzz-debug-apk** from the Actions run artifacts.
