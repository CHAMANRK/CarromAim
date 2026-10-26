# Carrom Vision Assist — captured-screen preview milestone

This Android project currently includes:
- Android MediaProjection permission flow and foreground capture service.
- A local, throttled screenshot preview (JPEG scaled to max 900 px) delivered only inside the app.
- A **Show captured screen / Hide captured screen** button in the app.
- A synthetic board + simple shot-planner demo.
- A floating AIM ON/OFF control prototype.

## Important current limitations
- The captured preview is the screen image, not an automatically cropped/detected carrom board yet.
- Live board, striker and coin detection is **not implemented** in this milestone.
- The aim overlay is **not yet connected** to live detection or real Carrom Pool shot calculations.
- No automatic shot input is implemented.

## Test flow
1. Build/install the APK from GitHub Actions.
2. Open Carrom Vision Assist and tap **Start screen capture**.
3. Accept Android's screen-capture prompt.
4. Open Carrom Pool, then return to Carrom Vision Assist.
5. Tap **Show captured screen** to inspect the latest captured frame. The frame updates while the capture session remains active.

The project uses Android's explicit user-consent screen-capture API. Captured frames are processed locally and are not uploaded by the app.
