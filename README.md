# "Are you ok?" Android app

## Description and manual

Take notice of the important information in the end of this file!

This app is designed to automatically tell selected contacts if you may need help. To turn this functionality on and off, you must press "start monitoring" or "stop monitoring". When monitoring is turned on, the app sometimes triggers a check, which is combined with an audible alarm, in order to ask you whether you are ok. If you do not answer within the specified time period (changeable by the "seconds for checking" buttons), the app triggers the alarm.

The alarm sends an SMS that you may need help and why to all previously selected contacts (we call these contacts "contact list"). If the app can obtain your location, it also sends your location to the contact list. Moreover, all contacts from your contact list are called periodically, one every three minutes (assuming that no dialing blocks the phone app for three minutes or more). The calls have the intention to make sure that your contacts get notified about the SMS as soon as possible.

When monitoring is turned on, it may be in two modes (pulse device connected or not connected).

Mode "pulse device not connected": The time when the next check is scheduled can be seen in the app user interface. You can move ("snooze") the time of the check to an arbitrary time in the future. A snooze is also considered a positive answer to the check (i.e. you seem to be ok), thus, the current check is finished and the audible alarm stops.

Mode "pulse device connected": If you connect a device via bluetooth that supports GATT and the HEART_RATE_MEASUREMENT characteristic, your current pulse is shown in the app user interface. If your pulse is outside of the expected range (changeable by the "min pulse" and "max pulse" buttons) or if the device is disconnected (except if you disconnect it yourself by pressing "disconnect"), a check is triggered. The check can be answered by the "snooze" button. Then, the check finishes. Whenever you press the "snooze" button, the pulse monitoring is "snoozed", i.e. before the specified time, a pulse outside of the expected range will not trigger a check.

IMPORTANT: Before using the app, test the alarm function, e.g. by starting the monitoring, setting the "time for checking" to 5 s and snoozing for 0 min.

IMPORTANT: This app has not been designed for phones with multiple sim cards.

This app works since API level 16 (Android 4.1 Jelly Bean), but connecting to a pulse device is supported only since API level 21 (Android 5.0 Lollipop).

## How to build

Just replace the path of android.jar on your computer in the make.sh file and execute make.sh on your Linux. The AreYouOk.apk file will be put in the ./bin/ directory.

## How to install and start

After building, just copy the AreYouOk.apk file to your phone, install and start it. Or connect your phone with your computer using the USB debugging option and execute start.sh.

## Bugs and problems

Please report bugs and problems to the author in order to support the development: [kolodez2106@gmail.com](mailto:kolodez2106@gmail.com)

* A newly established pulse device connection may be closed if a STATE_DISCONNECTED arrives too late (but this should rarely happen).
* The app cannot be installed to some phones for yet unknown reason: Samsung Galaxy Y Young

## Wish list for improvements

Please tell the author your wishes in order to support the development: kolodez2106@gmail.com

### App

* Put the app to repositories like Google Play.
* Develop an update mechanism.
* Remove the write storage permission.

### App user interface

* Keep the state of ActivityMain even after the activity was invisible or the screen was rotated.
* Develop a more attractive design.
* Tell the user when an SMS is sent.

### Checking

* Enable different checking time periods for different events.
* Also check the phone battery status.

### Beep

* Develop volume control.

### Pulse device

* Develop a possibility to cancel the connection process.

### Contacts

* Develop a more flexible algorithm for making calls and ordering them.
* The app should be working on multi-sim phones.

### Location

* Search for a location as soon as alarm starts and do not wait for 5 seconds.
* Add an optional permanent location tracker and trigger a check on location loss.

### Wake lock

* Disable wake lock if no device is connected and replace by an Android alarm in order to save phone battery.

## Acknowledgements

This project is inspired by the [VitalSigns project](https://github.com/DennisMat/VitalSigns) of [DennisMat](https://github.com/DennisMat) and the [CI Check-In App](https://www.cryonics.org/resources/ci-check-in-app) of [Cryonics Institute](https://www.cryonics.org/). For the inspiration for the build process see the readme of [this hello world project](https://github.com/Kolodez2106/HelloWorld-app-compiled-in-bash).

Sources for images:
* https://openclipart.org/detail/2528/love-heart
* https://openclipart.org/detail/184532/alarm-symbol
