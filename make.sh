#!/bin/bash

rm -v -f -r ./obj
rm -v -f -r ./bin
rm -v -f -r ./key
mkdir ./obj
mkdir ./bin
mkdir ./key
aapt package -v -f -m -S ./src/res/ -J ./src/src/ -M ./src/AndroidManifest.xml -I /usr/lib/android-sdk/platforms/android-23/android.jar
javac -d ./obj/ -source 1.7 -target 1.7 -classpath /usr/lib/android-sdk/platforms/android-23/android.jar -sourcepath ./src/src/ ./src/src/org/kolodez/AreYouOk/*
/usr/lib/android-sdk/build-tools/27.0.1/dx --dex --output=./bin/classes.dex ./obj/
aapt package -f -M ./src/AndroidManifest.xml -S ./src/res/ -I /usr/lib/android-sdk/platforms/android-23/android.jar -F ./bin/AreYouOk.unsigned.apk ./bin
keytool -genkeypair -validity 10000 -dname "CN=Kolodez, OU=Kolodez, O=Kolodez, C=US" -keystore ./key/mykey.keystore -storepass mypass -keypass mypass -alias myalias -keyalg RSA
jarsigner -keystore ./key/mykey.keystore -storepass mypass -keypass mypass -signedjar ./bin/AreYouOk.signed.apk ./bin/AreYouOk.unsigned.apk myalias
zipalign -f 4 ./bin/AreYouOk.signed.apk ./bin/AreYouOk.apk
