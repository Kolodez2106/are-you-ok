#!/bin/bash

adb shell am force-stop org.kolodez.AreYouOk
adb uninstall org.kolodez.AreYouOk
adb install -d ./bin/AreYouOk.apk
adb shell am start -c api.android.intent.LAUNCHER -a api.android.category.MAIN -n org.kolodez.AreYouOk/org.kolodez.AreYouOk.ActivityMain
