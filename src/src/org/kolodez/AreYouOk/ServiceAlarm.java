package org.kolodez.AreYouOk;

// VERIFIED

import java.util.IllegalFormatException;

import android.app.Notification; // API 1
import android.app.PendingIntent; // API 1
import android.app.Service; // API 1

import android.content.Context; // API 1
import android.content.Intent; // API 1

import android.os.Binder; // API 1
import android.os.Handler; // API 1
import android.os.IBinder; // API 1
import android.os.PowerManager; // API 1

/*
 * This class is available since API 16.
 */
public class ServiceAlarm extends Service {
	
	private static final int FOREGROUND_NOTIFICATION_ID = 3;
	
	
	private boolean bAlarmEnded = false;
	
	private PowerManager.WakeLock wakeLock;
	private Notification.Builder foregroundNotificationBuilder;
	
	private PeriodicBeep periodicBeep = null;
	private SearchLocation search = null;
	
	private AlarmBinder binder;
	
	
	public void onCreate() {
		super.onCreate(); // Service: API 1, returns void, nothing thrown
		
		String powerService = Context.POWER_SERVICE; // API 1
		
		PowerManager powerManager = (PowerManager) this.getSystemService (POWER_SERVICE); // ContextWrapper: API 1, nothing thrown
		if (powerManager != null) {
			
			int partialWakeLock = PowerManager.PARTIAL_WAKE_LOCK; // API 1
			
			this.wakeLock = powerManager.newWakeLock (partialWakeLock, "AreYouOk::AlarmWakeLockTag"); // PowerManager: API 1, nothing thrown
			
			if (this.wakeLock != null)
				this.wakeLock.acquire(); // PowerManager.WakeLock: API 1, returns void, nothing thrown
		}
		
		
		// move service to foreground
		Intent notificationIntent = new Intent (this, ActivityAlarm.class); // API 1, nothing thrown
		
		PendingIntent pendingIntent = PendingIntent.getActivity (this, 0, notificationIntent, 0); // API 1, nothing thrown
		if (pendingIntent != null) {
			
			this.foregroundNotificationBuilder = new Notification.Builder (this); // API 11, nothing thrown (DEPRECATED: "All posted Notifications must specify a NotificationChannel Id")
			
			this.foregroundNotificationBuilder.setSmallIcon (R.drawable.icon_alarm); // Notification.Builder: API 11, returns same Notification.Builder, nothing thrown
			this.foregroundNotificationBuilder.setContentIntent (pendingIntent); // Notification.Builder: API 11, returns same Notification.Builder, nothing thrown
			this.foregroundNotificationBuilder.setContentTitle ("AreYouOk alarm"); // Notification.Builder: API 11, returns same Notification.Builder, nothing thrown
			
			Notification foregroundNotification = foregroundNotificationBuilder.build(); // Notification.Builder: API 16, nothing thrown
			if (foregroundNotification != null)
				this.startForeground (FOREGROUND_NOTIFICATION_ID, foregroundNotification); // Service: API 5, returns void, nothing thrown
		}
		
		this.binder = this.new AlarmBinder();
		
		// start tone
		this.periodicBeep = PeriodicBeep.create (333, 0, PeriodicBeep.MAX_VOLUME);
		
		
		
		// write sms in 5 seconds and prepare repeated calling
		ServiceAlarm.Contacter contacter = new ServiceAlarm.Contacter();
		
		boolean result = AppState.handler.postDelayed (contacter, 5000); // Handler: API 1, nothing thrown
		if (!result)
			contacter.run();
		
		
		
		// start alarm activity
		Intent intent = new Intent (this, ActivityAlarm.class); // API 1, nothing thrown
		int flagActivityNewTask = Intent.FLAG_ACTIVITY_NEW_TASK; // API 1
		intent.addFlags (flagActivityNewTask); // Intent: API 1, returns same Intent, nothing thrown
		this.startActivity (intent); // ContextWrapper: API 1, returns void nothing thrown
	}
	
	
	public void onDestroy() {
		
		this.bAlarmEnded = true;
		
		if (this.periodicBeep != null) {
			this.periodicBeep.end();
			this.periodicBeep = null;
		}
		
		AppState.mode = AppMode.MONITOR;
		
		Intent intent = new Intent (this, ActivityMain.class); // API 1, nothing thrown
		int flagActivityNewTask = Intent.FLAG_ACTIVITY_NEW_TASK; // API 1
		intent.addFlags (flagActivityNewTask); // Intent: API 1, returns same Intent, nothing thrown
		this.startActivity (intent); // ContextWrapper: API 1, returns void nothing thrown
		
		
		if (this.search != null)
			this.search.close();
		
		if (this.wakeLock != null)
			this.wakeLock.release(); // PowerManager.WakeLock: API 1, returns void, throws nothing
		
		super.onDestroy(); // Service: API 1, returns void, nothing thrown
	}
	
	
	public IBinder onBind (Intent intent) {
		return this.binder;
	}
	
	
	public class AlarmBinder extends Binder {
		public void endAlarm () {
			ServiceAlarm.this.stopSelf(); // Service: API 1, returns void, nothing thrown
		}
	}
	
	
	private class Contacter implements Runnable {
		public void run() {
			if (!ServiceAlarm.this.bAlarmEnded) {
				String smsText = "SOS: I may be in need of help.";
				
				if (AppState.alarmReason == AppState.ALARM_REASON_BAD_PULSE)
					smsText += " (Pulse: " + AppState.alarmReasonPulseOnCheck + ")";
				else if (AppState.alarmReason == AppState.ALARM_REASON_DEVICE_DISCONNECTED)
					smsText += " (Pulse device disconnected)";
				else if (AppState.alarmReason == AppState.ALARM_REASON_NO_ANSWER)
					smsText += " (No answer)";
				
				smsText += " I have not answered whether I am ok for a period of " + AppState.alarmReasonTimeForChecking + " s. I will send my location in a separate message.";
				
				
				
				// search for location
				ServiceAlarm.this.search = SearchLocation.search (ServiceAlarm.this, new ServiceAlarm.LocationListener());
				
				
				for (ContactListEntry contact : AppState.contactList)
					Phone.sendSmsDivided (contact.number, smsText);
				
				// start call
				Caller caller = ServiceAlarm.this.new Caller();
				caller.run();
			}
		}
	}
	
	
	
	private class Caller implements Runnable {
		private int iContact = 0;
		
		public void run() {
			if (!ServiceAlarm.this.bAlarmEnded) {
				
				if (this.iContact >= AppState.contactList.size()) // nothing thrown
					this.iContact = 0;
				
				ContactListEntry contact;
				try {
					contact = AppState.contactList.get (this.iContact); // throws IndexOutOfBoundsException
				} catch (IndexOutOfBoundsException e) {
					contact = null;
				}
				
				if (contact != null)
					Phone.call (ServiceAlarm.this, contact.number);
				
				this.iContact += 1;
				
				if (!ServiceAlarm.this.bAlarmEnded)
					AppState.handler.postDelayed (this, 180000); // Handler: API 1, returns boolean, nothing thrown
					// TODO change time to flexible
			}
		}
	}
	
	
	
	public static class LocationListener implements SearchLocation.MyListener {
		
		public void locationObtained (SearchLocation.LocationData locationData) {
			
			String text;
			try {
				text = String.format ("My location (obtained by \"%s\" at %s): %f, %f (68%% confidence radius: %.0f m)", locationData.getProvider(), locationData.getTime(), locationData.getLatitude(), locationData.getLongitude(), locationData.getHorizontalConfidenceRadius()); // format() throws IllegalFormatException, NullPointerException
			} catch (IllegalFormatException e) { text = null; }
			catch (NullPointerException e) { text = null; }
			
			if (text != null) {
				for (ContactListEntry contact : AppState.contactList) {
					if (contact != null)
						Phone.sendSmsDivided (contact.number, text);
				}
			}
		}
	}
	
}
