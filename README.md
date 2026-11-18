# FocusLock: Android app blocker

Pick up to 4 apps and set a timer. Until the timer ends, every other app is blocked,
including Settings, the Play Store and the home screen. The only exception is the
phone app, which stays open for emergency calls. A session cannot be stopped early.

## Build and install
1. Install Android Studio (free).
2. Unzip this folder. In Android Studio choose File > Open and select the `FocusLock` folder.
   Wait for "Gradle sync" to finish. The first sync downloads the build tools.
3. On your phone, turn on Developer options and USB debugging, then connect it by USB.
4. Press the green Run button. The app installs as **FocusLock**.
   To get an APK file instead, use Build > Build App Bundle(s)/APK(s) > Build APK(s).

## First use
1. Open FocusLock and tap **Step 1: Allow usage access**. Find FocusLock and switch it on.
2. Tap **Step 2: Allow display over other apps** and switch it on.
3. Set the timer (up to 12 h), tick up to 4 apps, then tap **Start focus session**.

**Try a 2-minute session first.**

## How it works
- A background service checks about twice a second which app is on screen. It uses
  Usage Access for this, not Accessibility, so it cannot read what you type or see.
- If the app on screen is not allowed, the focus screen comes back. This screen shows
  the time left, your 4 apps and an Emergency call button.
- The block keeps working after a reboot, and it switches off on its own when the
  timer ends.

## Limits
- If one of your 4 apps opens another app (for example, the camera or a browser link),
  that other app is blocked too.
- Rebooting into Android **Safe mode** turns off all downloaded apps, so this gets
  around the block. No normal app can prevent that.
- Some phone makers (Xiaomi, Oppo, Vivo, Realme) close background apps aggressively.
  On those phones, set FocusLock's battery setting to "No restrictions" or "Allow
  background activity".
