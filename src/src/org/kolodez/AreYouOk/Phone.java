package org.kolodez.AreYouOk;

// VERIFIED

// This class is NOT designed for phones with multiple sim cards!

import java.util.ArrayList;

import android.content.ActivityNotFoundException; // API 1
import android.content.Context; // API 1
import android.content.Intent; // API 1

import android.net.Uri; // API 1

import android.telephony.SmsManager; // API 4

public class Phone { // API 4
	
	public static void sendSmsDivided (String phoneNumber, String message) {
		
		SmsManager smsManager = SmsManager.getDefault(); // API 4, nothing thrown
		
		if (smsManager != null) {
			
			ArrayList <String> listOfMessages;
			
			try {
				listOfMessages = smsManager.divideMessage (message); // SmsManager: API 4, throws IllegalArgumentException
				
				smsManager.sendMultipartTextMessage (phoneNumber, null, listOfMessages, null, null); // SmsManager: API 4, returns void, throws IllegalArgumentException
			} catch (IllegalArgumentException e) { }
		}
	}
	
	
	public static void call (Context context, String phoneNumber) {
		
		String actionCall = Intent.ACTION_CALL; // API 1
		
		Uri uri;
		try {
			uri = Uri.parse("tel:" + phoneNumber); // API 1, throws NullPointerException
		} catch (NullPointerException e) { return; }
		
		if (uri == null)
			return;
		
		Intent intent = new Intent (actionCall, uri); // API 1, nothing thrown
		
		int flagActivityNewTask = Intent.FLAG_ACTIVITY_NEW_TASK; // API 1
		intent.addFlags (flagActivityNewTask); // Intent: API 1, returns same Intent, nothing thrown
		
		try {
			context.startActivity (intent); // Context: API 1, returns void, throws ActivityNotFoundException
		} catch (ActivityNotFoundException e) { }
	}
	
}
