# Carrom Vision Assist — Status Pill Prototype

Android Kotlin/Compose prototype. The floating overlay now includes a single-line status pill across the top of the screen plus a separate draggable AIM ON/OFF control.

## Overlay status examples
- `JARVIS · Overlay ready`
- `CAPTURE · Starting screen session`
- `CAPTURE · Waiting for first frame`
- `CAPTURE · Frame received`
- `CAPTURE · Stopped`
- `AIM ON · Detection not connected`

The status pill only reports capture lifecycle/frame arrival. Live Carrom Pool board/coin detection and computed aiming are **not implemented yet**; AIM ON is not an actual aim engine.

## Build
Open the repository in GitHub and run **Actions → Android CI → Run workflow**. Download `carrom-vision-assist-debug-apk` if the build succeeds.

## Test
1. Install the generated APK.
2. Open the app and grant Display over other apps.
3. Tap **Start floating aim button** to show the one-line status pill and AIM control.
4. Start screen capture and approve Android's screen capture prompt.
5. Switch to Carrom Pool. The pill should change to `CAPTURE · Frame received` once frames arrive.

Screen capture requires explicit Android consent. If the pill remains at `Waiting for first frame`, capture is not delivering frames; check Android's projection permission and the app's foreground notification.
