package org.kolodez.AreYouOk;

// VERIFIED

import java.util.List;
import java.util.LinkedList;

import org.json.JSONArray; // API 1
import org.json.JSONObject; // API 1
import org.json.JSONException; // API 1

import android.os.Build; // API 1
import android.os.Handler; // API 1
import android.os.Looper; // API 1

import android.content.Context; // API 1
import android.content.SharedPreferences; // API 1

import android.preference.PreferenceManager; // API 1, DEPRECATED since API 29



public class AppState { // API 4
	
	public static final int SDK = Build.VERSION.SDK_INT; // API 4
	
	private static boolean bInitialized = false;
	
	public static int mode = AppMode.MONITOR;
	
	public static boolean bMonitoring = false;
	
	public static int minPulse = 40;
	public static int maxPulse = 100;
	public static int timeForChecking = 120; // in seconds
	
	
	public static final int ALARM_REASON_BAD_PULSE = 0;
	public static final int ALARM_REASON_DEVICE_DISCONNECTED = 1;
	public static final int ALARM_REASON_NO_ANSWER = 2;
	
	public static int alarmReason;
	
	public static int alarmReasonPulseOnCheck;
	public static int alarmReasonTimeForChecking;
	
	public static ContactList contactList; // not null after initIfNecessary ()
	
	public static Handler handler; // not null after initIfNecessary ()
	
	
	public static void initIfNecessary (Context context) throws NullPointerException {
		if (!bInitialized) {
			Looper looper = Looper.getMainLooper(); // API 1, nothing thrown
			if (looper == null)
				throw new NullPointerException ("Looper.getMainLooper() returned null");
			
			handler = new Handler (looper); // API 1, nothing thrown
			
			contactList = new ContactList (context);
			contactList.readFromSharedPreferences ();
			
			bInitialized = true;
		}
	}
	
	public static void writeContactsToSharedPreferences () {
		contactList.writeToSharedPreferences ();
	}
	
	
	
	public static class ContactList extends LinkedList <ContactListEntry> {
		
		private static final String PREFERENCE_KEY_CONTACT_LIST = "ContactList";
		
		private SharedPreferences sharedPreferences;
		private SharedPreferences.Editor editor;
		
		private ContactList (Context context) {
			super (); // LinkedList: nothing thrown
			
			this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences (context); // API 1, does not return null, nothing thrown
			this.editor = this.sharedPreferences.edit(); // SharedPreferences: API 1, does not return null, nothing thrown
		}
		
		private void writeToSharedPreferences () {
			
			List <JSONObject> list = new LinkedList <JSONObject> (); // nothing thrown
			
			for (ContactListEntry entry : this) {
				JSONObject object = new JSONObject(); // API 1, nothing thrown
				
				try {
					object.put ("name", entry.name); // JSONObject: API 1, returns same JSONObject, throws JSONException
					object.put ("number", entry.number);
					list.add (object); // throws: UnsupportedOperationException, ClassCastException, NullPointerException, IllegalArgumentException
				}
				catch (JSONException e) { }
				catch (UnsupportedOperationException e) { }
				catch (ClassCastException e) { }
				catch (NullPointerException e) { }
				catch (IllegalArgumentException e) { }
			}
			
			JSONArray array = new JSONArray (list); // API 1, nothing thrown
			String arrayString = array.toString(); // JSONArray: API 1, nothing thrown
			
			this.editor.putString (PREFERENCE_KEY_CONTACT_LIST, arrayString); // SharedPreferences.Editor: API 1, returns same SharedPreferences.Editor, nothing thrown
			
			if (SDK >= 9)
				this.editor.apply(); // SharedPreferences.Editor: API 9, returns void, nothing thrown
			else
				this.editor.commit(); // SharedPreferences.Editor: API 1, returns boolean, nothing thrown
		}
		
		private void readFromSharedPreferences () {
			
			String preferenceValue = null;
			
			try {
				preferenceValue = this.sharedPreferences.getString (PREFERENCE_KEY_CONTACT_LIST, null); // SharedPreferences: API 1, throws ClassCastException (this.sharedPreferences is not null, see constructor)
			} catch (ClassCastException e) { }
			
			if (preferenceValue == null)
				this.clear(); // LinkedList: returns void, nothing thrown
			else {
				JSONArray array;
				
				try {
					array = new JSONArray (preferenceValue); // API 1, throws JSONException
				} catch (JSONException e) {
					this.clear(); // LinkedList: returns void, nothing thrown
					return;
				}
				
				
				int length = array.length(); // JSONArray: API 1, nothing thrown
				
				for (int i = 0; i < length; i++) {
					JSONObject object = array.optJSONObject (i); // JSONArray: API 1, may return null, nothing thrown
					
					if (object != null) {
						String name, number;
						try {
							name = object.getString ("name"); // JSONObject: API 1, throws JSONException
							number = object.getString ("number"); // JSONObject: API 1, throws JSONException
						} catch (JSONException e) { continue; }
						
						if ((name == null) || (number == null))
							continue;
						
						ContactListEntry entry = new ContactListEntry (name, number);
						
						this.addLast (entry); // LinkedList: nothing thrown
					}
				}
			}
		}
	}
	
}