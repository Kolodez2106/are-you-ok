package org.kolodez.AreYouOk;

// VERIFIED

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.UUID;

import android.app.Notification; // API 1
import android.app.PendingIntent; // API 1
import android.app.Service; // API 1

import android.bluetooth.BluetoothDevice; // API 5
import android.bluetooth.BluetoothGatt; // API 18
import android.bluetooth.BluetoothGattCallback; // API 18
import android.bluetooth.BluetoothGattCharacteristic; // API 18
import android.bluetooth.BluetoothGattDescriptor; // API 18
import android.bluetooth.BluetoothGattService; // API 18
import android.bluetooth.BluetoothProfile; // API 11

import android.content.Context; // API 1
import android.content.Intent; // API 1

import android.os.Binder; // API 1
import android.os.Handler; // API 1
import android.os.IBinder; // API 1
import android.os.PowerManager; // API 1

/*
 * This class is available since API 16.
 */
public class ServiceMain extends Service {
	
	private static final int FOREGROUND_NOTIFICATION_ID = 2;
	
	private static final int STANDARD_SNOOZE_MINUTES = 1;
	
	private static final String UUID_HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
	private static final String UUID_BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb";
	private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	
	public static String printTime (Calendar time) {
		String result;
		
		try {
			result = String.format ("%d-%02d-%02d %02d:%02d:%02d", time.get (Calendar.YEAR), time.get (Calendar.MONTH) + 1, time.get (Calendar.DAY_OF_MONTH), time.get (Calendar.HOUR_OF_DAY), time.get (Calendar.MINUTE), time.get (Calendar.SECOND)); // Calendar.get() throws ArrayIndexOutOfBoundsException, format() throws IllegalFormatException, NullPointerException
		} catch (ArrayIndexOutOfBoundsException e) { result = null; }
		catch (IllegalFormatException e) { result = null; }
		catch (NullPointerException e) { result = null; }
		
		return result;
	}
	
	
	
	
	
	private PowerManager.WakeLock wakeLock;
	
	private boolean bServiceActive = false;
	
	private boolean bDeviceConnected = false;
	private boolean bMonitorPulseSnoozed = false; // if bDeviceConnected is false, this must be false
	private boolean bConnectingProcess = false; // true when pulse is not monitored, but the device is searched for
	
	Checker pendingChecker; // as long as the service is active, if bDeviceConnected is false, this should not be null; if bDeviceConnected is true, this is not null if and only if a check is ongoing
	PulseCheckResumer pendingPulseCheckResumer = null; // as long as the service is active, if bMonitorPulseSnoozed is true, this should be not null; otherwise null
	
	private String connectionStatus = "not connected";
	
	private BluetoothDevice device = null;
	
	private int timeForChecking;
	private int minPulse, maxPulse;
	private int pulse = -1;
	
	private int batteryLevel = -1;
	
	private BluetoothGatt gatt = null;
	private BluetoothGattCharacteristic batteryCharacteristic = null;
	private boolean bPulseCharacteristicFound = false;
	private boolean bBatteryCharacteristicFound = false;
	
	
	
	private MyBinder binder;
	private ActivityMainLayoutMain.ServiceListener listener = null;
	
	private Notification.Builder foregroundNotificationBuilder;
	
	public void onCreate() {
		super.onCreate(); // Service: API 1, returns void, nothing thrown
		
		this.bServiceActive = true;
		
		
		String powerService = Context.POWER_SERVICE; // API 1
		
		PowerManager powerManager = (PowerManager) this.getSystemService (POWER_SERVICE); // ContextWrapper: API 1, nothing thrown
		if (powerManager == null)
			this.stopSelf(); // Service: API 1, returns void, nothing thrown
		
		int partialWakeLock = PowerManager.PARTIAL_WAKE_LOCK; // API 1
		
		this.wakeLock = powerManager.newWakeLock (partialWakeLock, "AreYouOk::MonitorWakeLockTag"); // PowerManager: API 1, nothing thrown
		if (this.wakeLock == null)
			this.stopSelf(); // Service: API 1, returns void, nothing thrown
		
		this.wakeLock.acquire(); // PowerManager.WakeLock: API 1, returns void, nothing thrown
		// TODO disable wake lock on snooze without pulse
		
		
		// post next check
		try {
			this.pendingChecker = this.new Checker (AppState.ALARM_REASON_NO_ANSWER, 0, 0, STANDARD_SNOOZE_MINUTES, 0);
		} catch (IllegalArgumentException e) {
			this.stopSelf(); // Service: API 1, returns void, nothing thrown
			return;
		}
		this.pendingChecker.post();
		
		this.timeForChecking = AppState.timeForChecking;
		this.minPulse = AppState.minPulse;
		this.maxPulse = AppState.maxPulse;
		
		this.binder = new ServiceMain.MyBinder();
		
		// move service to foreground
		Intent notificationIntent = new Intent (this, ActivityMain.class); // API 1, nothing thrown
		
		PendingIntent pendingIntent = PendingIntent.getActivity (this, 0, notificationIntent, 0); // API 1, nothing thrown
		if (pendingIntent == null) {
			this.stopSelf(); // Service: API 1, returns void, nothing thrown
			return;
		}
		
		this.foregroundNotificationBuilder = new Notification.Builder (this); // API 11, nothing thrown (DEPRECATED: "All posted Notifications must specify a NotificationChannel Id")
		
		this.foregroundNotificationBuilder.setSmallIcon (R.drawable.icon); // Notification.Builder: API 11, returns same Notification.Builder, nothing thrown
		this.foregroundNotificationBuilder.setContentIntent (pendingIntent); // Notification.Builder: API 11, returns same Notification.Builder, nothing thrown
		this.foregroundNotificationBuilder.setContentTitle ("Are you ok?"); // Notification.Builder: API 11, returns same Notification.Builder, nothing thrown
		
		Notification foregroundNotification = foregroundNotificationBuilder.build(); // Notification.Builder: API 16, nothing thrown
		if (foregroundNotification == null) {
			this.stopSelf(); // Service: API 1, returns void, nothing thrown
			return;
		}
		
		this.startForeground (FOREGROUND_NOTIFICATION_ID, foregroundNotification); // Service: API 5, returns void, nothing thrown
		
		
		AppState.bMonitoring = true;
	}
	
	
	public void onDestroy() {
		
		this.bServiceActive = false;
		
		if (this.pendingChecker != null) {
			this.pendingChecker.cancel();
			this.pendingChecker = null;
		}
		
		if (this.pendingPulseCheckResumer != null)
			this.pendingPulseCheckResumer = null;
		
		if (this.gatt != null) {
			this.gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
			this.gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
			this.gatt = null;
		}
		
		if (this.wakeLock != null) {
			this.wakeLock.release(); // PowerManager.WakeLock: API 1, returns void, throws nothing
			this.wakeLock = null;
		}
		
		
		super.onDestroy(); // Service: API 1, returns void, nothing thrown
	}
	
	
	
	public IBinder onBind (Intent intent) {
		return this.binder;
	}
	
	
	private void updateActivityIfPossible () {
		if (this.listener != null)
			this.listener.update();
	}
	
	
	
	private class BatteryStatusReader implements Runnable {
		public void run() {
			if (
				(ServiceMain.this.gatt != null) &&
				(ServiceMain.this.batteryCharacteristic != null)
			) {
				ServiceMain.this.gatt.readCharacteristic (ServiceMain.this.batteryCharacteristic); // BluetoothGatt: API 18, returns boolean, nothing thrown
				
				AppState.handler.postDelayed (this, 10000); // Handler: API 1, nothing thrown
			}
		}
	}
	
	
	private class PulseCheckResumer implements Runnable {
		
		private Calendar scheduledTime;
		
		private int hours;
		private int minutes;
		private int seconds;
		
		public PulseCheckResumer (int hours, int minutes, int seconds) throws IllegalArgumentException {
			
			this.scheduledTime = new GregorianCalendar(); // throws nothing
			this.scheduledTime.add (Calendar.HOUR_OF_DAY, hours); // GregorianCalendar: throws IllegalArgumentException
			this.scheduledTime.add (Calendar.MINUTE, minutes);
			this.scheduledTime.add (Calendar.SECOND, seconds);
			
			this.hours = hours;
			this.minutes = minutes;
			this.seconds = seconds;
		}
		
		// call this immediately
		public void post() {
			if (ServiceMain.this.bServiceActive)
				AppState.handler.postDelayed (this, ((this.hours * 60 + this.minutes) * 60 + this.seconds) * 1000); // Handler: API 1, returns boolean, nothing thrown
		}
		
		public void run() {
			if (ServiceMain.this.pendingPulseCheckResumer != this) // this resumer was cancelled
				return;
			
			ServiceMain.this.bMonitorPulseSnoozed = false;
		}
	}
	
	
	private class Checker implements Runnable {
		
		private int pendingAlarmReasonReason;
		private int pulse;
		private int timeForCheckingWhenStarted;
		private Calendar scheduledTime; // null means to start check immediately
		private Calendar timeOfAlarm; // null until check starts; when it starts, the alarm is posted and this time is set
		private PeriodicBeep beep;
		
		// if hours = minutes = seconds = 0, no exception is thrown
		private Checker (int pendingAlarmReasonReason, int pulse, int hours, int minutes, int seconds) throws IllegalArgumentException {
			this.pendingAlarmReasonReason = pendingAlarmReasonReason;
			this.pulse = pulse;
			
			if ((hours == 0) && (minutes == 0) && (seconds == 0)) {
				this.scheduledTime = null;
			}
			else {
				this.scheduledTime = new GregorianCalendar(); // throws nothing
				this.scheduledTime.add (Calendar.HOUR_OF_DAY, hours); // GregorianCalendar: throws IllegalArgumentException
				this.scheduledTime.add (Calendar.MINUTE, minutes);
				this.scheduledTime.add (Calendar.SECOND, seconds);
			}
			
			this.timeOfAlarm = null;
			this.beep = null;
		}
		
		public void post () {
			boolean bPostImmediately;
			long timeDifference = 0;
			
			if (ServiceMain.this.bServiceActive) {
				if (this.scheduledTime == null)
					bPostImmediately = true;
				else {
					timeDifference = this.scheduledTime.getTimeInMillis() // Calendar: throws nothing
						- (new GregorianCalendar()). // throws nothing
							getTimeInMillis(); // Calendar: throws nothing
					
					bPostImmediately = (timeDifference <= 0);
				}
				
				if (bPostImmediately)
					AppState.handler.post (this); // Handler: API 1, nothing thrown
				else
					AppState.handler.postDelayed (this, timeDifference); // Handler: API 1, returns boolean, nothing thrown
			}
		}
		
		public void cancel () {
			if (this.beep != null) {
				this.beep.end();
				this.beep = null;
			}
		}
		
		public void run () {
			
			if (ServiceMain.this.pendingChecker != this) // this checker was cancelled
				return;
			
			if (this.timeOfAlarm == null) { // start check "are you ok?" (else: start alarm)
				
				if (this.scheduledTime != null) {
					
					Calendar currentTime = new GregorianCalendar(); // throws nothing
					
					boolean bScheduledTimeInPast;
					try {
						bScheduledTimeInPast = currentTime.compareTo (this.scheduledTime) >= 0; // Calendar: throws NullPointerException, IllegalArgumentException
					} catch (NullPointerException e) { bScheduledTimeInPast = true; }
					catch (IllegalArgumentException e) { bScheduledTimeInPast = true; }
					
					if (!bScheduledTimeInPast) {
						AppState.handler.postDelayed (this, 1000); // Handler: API 1, returns boolean, nothing thrown
						return;
					}
					
				}
				
				
				this.timeForCheckingWhenStarted = ServiceMain.this.timeForChecking;
				
				this.beep = PeriodicBeep.create (1000, this.timeForCheckingWhenStarted * 1000 / 3, PeriodicBeep.MAX_VOLUME);
				
				this.timeOfAlarm = new GregorianCalendar(); // throws nothing
				try {
					this.timeOfAlarm.add (Calendar.SECOND, this.timeForCheckingWhenStarted); // GregorianCalendar: throws IllegalArgumentException
				} catch (IllegalArgumentException e) { }
				
				AppState.handler.postDelayed (this, 1000 * this.timeForCheckingWhenStarted); // Handler: API 1, returns boolean, nothing thrown
				
				Intent intent = new Intent (ServiceMain.this, ActivityMain.class); // API 1, nothing thrown
				int flagActivityNewTask = Intent.FLAG_ACTIVITY_NEW_TASK; // API 1
				intent.addFlags (flagActivityNewTask); // Intent: API 1, returns same Intent, nothing thrown
				ServiceMain.this.startActivity (intent); // ContextWrapper: API 1, returns void nothing thrown
				
				ServiceMain.this.updateActivityIfPossible();
			}
			else { // start alarm
				
				AppState.mode = AppMode.ALARM;
				AppState.bMonitoring = false;
				AppState.minPulse = ServiceMain.this.minPulse;
				AppState.maxPulse = ServiceMain.this.maxPulse;
				AppState.timeForChecking = ServiceMain.this.timeForChecking;
				AppState.alarmReason = this.pendingAlarmReasonReason;
				AppState.alarmReasonPulseOnCheck = this.pulse;
				AppState.alarmReasonTimeForChecking = this.timeForCheckingWhenStarted;
				
				Intent intent = new Intent (ServiceMain.this, ServiceAlarm.class); // API 1, nothing thrown
				try {
					ServiceMain.this.startService (intent); // ContextWrapper: API 1, returns ComponentName, throws SecurityException
				} catch (SecurityException e) { }
				
				ServiceMain.this.stopSelf(); // Service: API 1, returns void, nothing thrown
				// onDestroy() will cancel this checker beep
			}
		}
	}
	
	
	
	
	
	
	public class MyBinder extends Binder {
		
		// manage activity listener:
		
		public void registerStateChangeListener (ActivityMainLayoutMain.ServiceListener listener) { ServiceMain.this.listener = listener; }
		
		public void unregisterStateChangeListener () { ServiceMain.this.listener = null; }
		
		
		
		// end service
		
		public void end() {
			if (ServiceMain.this.bServiceActive) {
				AppState.bMonitoring = false;
				AppState.minPulse = ServiceMain.this.minPulse;
				AppState.maxPulse = ServiceMain.this.maxPulse;
				AppState.timeForChecking = ServiceMain.this.timeForChecking;
				
				ServiceMain.this.stopSelf(); // Service: API 1, returns void, nothing thrown
			}
		}
		
		
		
		// get status functions:
		
		public boolean isChecking () { return (ServiceMain.this.pendingChecker != null) && (ServiceMain.this.pendingChecker.timeOfAlarm != null); }
		
		public boolean isDeviceConnected () { return ServiceMain.this.bDeviceConnected; }
		
		public boolean isPulseMonitorSnoozed () { return ServiceMain.this.bMonitorPulseSnoozed; }
		
		public boolean isConnectingProcess () { return  ServiceMain.this.bConnectingProcess; }
		
		public String getConnectionStatus () { return ServiceMain.this.connectionStatus; }
		
		public Calendar getNextCheckTime () {
			if (ServiceMain.this.bDeviceConnected) {
				if (ServiceMain.this.bMonitorPulseSnoozed) {
					if (ServiceMain.this.pendingPulseCheckResumer != null)
						return ServiceMain.this.pendingPulseCheckResumer.scheduledTime;
					else
						return null;
				}
				else
					return null;
			}
			else {
				if (ServiceMain.this.pendingChecker != null)
					return ServiceMain.this.pendingChecker.scheduledTime;
				else
					return null;
			}
		}
		
		public long getDifferenceToCheckTime () {
			Calendar nextCheckTime = this.getNextCheckTime();
			
			if (nextCheckTime != null)
				return nextCheckTime.getTimeInMillis() // Calendar: throws nothing
					- (new GregorianCalendar()) // throws nothing
						.getTimeInMillis(); // Calendar: throws nothing
			else
				return 0;
		}
		
		public long getDifferenceToAlarmTime () {
			Calendar nextAlarmTime;
			
			if (ServiceMain.this.pendingChecker != null)
				nextAlarmTime = ServiceMain.this.pendingChecker.timeOfAlarm;
			else
				nextAlarmTime = null;
			
			if (nextAlarmTime != null)
				return nextAlarmTime.getTimeInMillis() - // Calendar: throws nothing
					(new GregorianCalendar()). // throws nothing
						getTimeInMillis(); // Calendar: throws nothing
			else
				return 0;
		}
		
		public int getPulse () { return ServiceMain.this.pulse; }
		
		public int getMinPulse () { return ServiceMain.this.minPulse; }
		
		public int getMaxPulse () { return ServiceMain.this.maxPulse; }
		
		public int getBatteryLevel () { return ServiceMain.this.batteryLevel; }
		
		public int getTimeForChecking () { return ServiceMain.this.timeForChecking; }
		
		
		
		
		// set status functions:
		
		public void snooze (int hours, int minutes, int seconds) throws IllegalArgumentException {
			if (ServiceMain.this.bDeviceConnected) {
				PulseCheckResumer resumer = ServiceMain.this.new PulseCheckResumer (hours, minutes, seconds); // throws IllegalArgumentException
				resumer.post();
				
				if (ServiceMain.this.pendingChecker != null) {
					ServiceMain.this.pendingChecker.cancel();
					ServiceMain.this.pendingChecker = null;
				}
				
				ServiceMain.this.bMonitorPulseSnoozed = true;
				ServiceMain.this.pendingPulseCheckResumer = resumer;
			}
			
			else {
				
				Checker checker = ServiceMain.this.new Checker (AppState.ALARM_REASON_NO_ANSWER, 0, hours, minutes, seconds); // throws IllegalArgumentException
				checker.post();
				
				if (ServiceMain.this.pendingChecker != null)
					ServiceMain.this.pendingChecker.cancel();
				
				ServiceMain.this.pendingChecker = checker;
			}
			
			ServiceMain.this.updateActivityIfPossible();
		}
		
		// id is -1 for min pulse, 1 for max pulse and 0 for time until alarm
		public void addValue (int id, int summand) {
			
			int target;
			
			if (id == -1)
				target = ServiceMain.this.minPulse;
			else if (id == 1)
				target = ServiceMain.this.maxPulse;
			else if (id == 0)
				target = ServiceMain.this.timeForChecking;
			else
				target = 0;
			
			
			
			target += summand;
			
			if ((0 < target) && (target < 200)) {
				
				if (id == -1)
					ServiceMain.this.minPulse = target;
				else if (id == 1)
					ServiceMain.this.maxPulse = target;
				else if (id == 0)
					ServiceMain.this.timeForChecking = target;
				
				ServiceMain.this.updateActivityIfPossible();
			}
			
		}
		
		
		
		public void disconnectDevice () {
			ServiceMain.this.disconnectDevice();
		}
		
		public void connectDevice (BluetoothDevice device) {
			ServiceMain.this.connectDevice (device);
		}
		
	}
	
	
	
	
	private void disconnectDevice() {
		this.bDeviceConnected = false;
		this.bMonitorPulseSnoozed = false;
		
		if (this.pendingChecker != null) {
			this.pendingChecker.cancel();
			this.pendingChecker = null;
		}
		
		try {
			this.pendingChecker = this.new Checker (AppState.ALARM_REASON_NO_ANSWER, 0, 0, STANDARD_SNOOZE_MINUTES, 0);
		} catch (IllegalArgumentException e) {
			this.stopSelf(); // Service: API 1, returns void, nothing thrown
			return;
		}
		this.pendingChecker.post();
		
		this.pendingPulseCheckResumer = null;
		
		this.connectionStatus = "not connected";
		
		this.device = null;
		this.pulse = -1;
		this.batteryLevel = -1;
		
		if (this.gatt != null) {
			this.gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
			this.gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
			this.gatt = null;
		}
		this.batteryCharacteristic = null;
		this.bPulseCharacteristicFound = false;
		this.bBatteryCharacteristicFound = false;
		
		this.updateActivityIfPossible();
	}
	
	
	private void connectDevice (BluetoothDevice device) {
		if ((AppState.SDK >= 18) && this.bServiceActive) {
			this.connectionStatus = "connecting to device ...";
			
			this.device = device;
			
			try {
				this.gatt = device.connectGatt (this, false, new MyBluetoothGattCallback()); // BluetoothDevice: API 18, throws IllegalArgumentException
			} catch (IllegalArgumentException e) { this.gatt = null; }
			
			if (this.gatt == null)
				this.connectionStatus = "connection failed";
			
			this.updateActivityIfPossible();
		}
	}
	
	
	// API 18
	private void extractPulse (BluetoothGattCharacteristic pulseCharacteristic) {
		
		int properties = pulseCharacteristic.getProperties(); // BluetoothGattCharacteristic: API 18, throws nothing
		
		int format;
		
		if ((properties & 0x01) != 0) // TODO This is copied from https://github.com/DennisMat/VitalSigns, but 1 = BluetoothGattCharacteristic.PROPERTY_BROADCAST.
			format = BluetoothGattCharacteristic.FORMAT_UINT16; // API 18
		
		else
			format = BluetoothGattCharacteristic.FORMAT_UINT8; // API 18
		
		Integer intPulse = pulseCharacteristic.getIntValue (format, 1); // BluetoothGattCharacteristic: API 18, throws nothing
		if (intPulse == null)
			this.pulse = -1;
		else
			this.pulse = intPulse;
		
		this.updateActivityIfPossible();
		
		if (
			this.bServiceActive && this.bDeviceConnected && !this.bMonitorPulseSnoozed &&
			(this.pendingChecker == null) &&
			((this.pulse < this.minPulse) || (this.maxPulse < this.pulse))
		) {
			this.pendingChecker = this.new Checker (AppState.ALARM_REASON_BAD_PULSE, this.pulse, 0, 0, 0); // throws nothing
			this.pendingChecker.post();
		}
	}
	
	
	// API 18
	private void extractBatteryLevel (BluetoothGattCharacteristic characteristic) {
		int format = BluetoothGattCharacteristic.FORMAT_UINT8; // API 18
		
		Integer intBatteryLevel = characteristic.getIntValue (format, 0); // BluetoothGattCharacteristic: API 18, throws nothing
		
		if (intBatteryLevel == null)
			this.batteryLevel = -1;
		else
			this.batteryLevel = intBatteryLevel;
	}
	
	
	// API 18
	private class MyBluetoothGattCallback extends BluetoothGattCallback {
		
		public void onCharacteristicChanged (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			
			if ((gatt != null) && (characteristic != null)) {
				
				if (ServiceMain.this.bServiceActive) {
					UUID uuid = characteristic.getUuid(); // BluetoothGattCharacteristic: API 18, throws nothing
					if (uuid == null)
						return;
					
					String uuidString = uuid.toString(); // UUID: API 1, throws nothing
					if (uuidString == null)
						return;
					
					if (uuidString.equals (UUID_HEART_RATE_MEASUREMENT)) // throws nothing
						ServiceMain.this.extractPulse (characteristic);
				}
				else {
					gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
					gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
				}
			}
		}
		
		public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			
			if ((gatt != null) && (characteristic != null)) {
				
				if (ServiceMain.this.bServiceActive) {
					if (status == BluetoothGatt.GATT_SUCCESS) {
						UUID uuid = characteristic.getUuid(); // BluetoothGattCharacteristic: API 18, throws nothing
						if (uuid == null)
							return;
						
						String uuidString = uuid.toString(); // UUID: API 1, throws nothing
						if (uuidString == null)
							return;
						
						if (uuidString.equals (UUID_BATTERY_LEVEL)) // throws nothing
							ServiceMain.this.extractBatteryLevel (characteristic);
					}
				}
				else {
					gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
					gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
				}
			}
			
		}
		
		public void onCharacteristicWrite (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) { }
		
		public void onConnectionStateChange (BluetoothGatt gatt, int status, int newState) {
			
			if (gatt != null) {
				
				if (newState == BluetoothProfile.STATE_CONNECTED) { // API 11
					
					if (ServiceMain.this.bServiceActive) {
						boolean bDiscoverStarted = gatt.discoverServices(); // BluetoothGatt: API 18, throws nothing
						
						if (!bDiscoverStarted) {
							gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
							gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
						}
						else {
							ServiceMain.this.connectionStatus = "discovering services ...";
							ServiceMain.this.updateActivityIfPossible();
						}
					}
					else {
						gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
						gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
					}
				}
				
				else { // newState == BluetoothProfile.STATE_DISCONNECTED
					
					ServiceMain.this.bDeviceConnected = false;
					ServiceMain.this.bMonitorPulseSnoozed = false;
					ServiceMain.this.bConnectingProcess = false;
					
					if (ServiceMain.this.bServiceActive && (ServiceMain.this.pendingChecker == null)) {
						ServiceMain.this.pendingChecker = ServiceMain.this.new Checker (AppState.ALARM_REASON_DEVICE_DISCONNECTED, 0, 0, 0, 0); // throws nothing
						ServiceMain.this.pendingChecker.post();
					}
					
					ServiceMain.this.pendingPulseCheckResumer = null;
					
					ServiceMain.this.connectionStatus = "disconnected";
					
					ServiceMain.this.device = null;
					
					ServiceMain.this.pulse = -1;
					ServiceMain.this.batteryLevel = -1;
					
					ServiceMain.this.gatt = null;
					ServiceMain.this.batteryCharacteristic = null;
					ServiceMain.this.bPulseCharacteristicFound = false;
					ServiceMain.this.bBatteryCharacteristicFound = false;
					
					ServiceMain.this.updateActivityIfPossible();
					
				}
			}
		}
		
		public void onDescriptorRead (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) { }
		
		public void onDescriptorWrite (BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) { }
		
		public void onMtuChanged (BluetoothGatt gatt, int mtu, int status) { }
		
		public void onPhyRead (BluetoothGatt gatt, int txPhy, int rxPhy, int status) { }
		
		public void onPhyUpdate (BluetoothGatt gatt, int txPhy, int rxPhy, int status) { }
		
		public void onReadRemoteRssi (BluetoothGatt gatt, int rssi, int status) { }
		
		public void onReliableWriteCompleted (BluetoothGatt gatt, int status) { }
		
		public void onServicesDiscovered (BluetoothGatt gatt, int status) {
			if (gatt == null)
				return;
			
			if (ServiceMain.this.bServiceActive) {
				
				if (status == BluetoothGatt.GATT_SUCCESS) {
					List<BluetoothGattService> listServices = gatt.getServices(); // BluetoothGatt: API 18, throws nothing
					if (listServices == null) {
						gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
						gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
						return;
					}
					
					for (BluetoothGattService service : listServices) {
						if (service != null) {
							
							List<BluetoothGattCharacteristic> listCharacteristics =  service.getCharacteristics(); // BluetoothGattService: API 18, throws nothing
							
							if (listCharacteristics != null) {
								for (BluetoothGattCharacteristic characteristic : listCharacteristics) {
									
									UUID uuid = characteristic.getUuid(); // BluetoothGattCharacteristic: API 18, throws nothing
									if (uuid != null) {
										String uuidString = uuid.toString(); // UUID: API 1, throws nothing
										if (
											(uuidString != null) &&
											uuidString.equals (UUID_HEART_RATE_MEASUREMENT)// throws nothing
										) {
										
											ServiceMain.this.bPulseCharacteristicFound = true;
											
											boolean result = gatt.setCharacteristicNotification (characteristic, true); // BluetoothGatt: API 18, throws nothing
											
											if (!result) {
												ServiceMain.this.onConnectionFailed ("setting characteristic notification failed");
												return;
											}
											
											UUID uuidClientCharacteristicConfig;
											try {
												uuidClientCharacteristicConfig = UUID.fromString(UUID_CLIENT_CHARACTERISTIC_CONFIG); // API 1, throws IllegalArgumentException
											} catch (IllegalArgumentException e) { uuidClientCharacteristicConfig = null; }
											
											if (uuidClientCharacteristicConfig == null) {
												ServiceMain.this.onConnectionFailed ("error: uuidClientCharacteristicConfig == null");
												return;
											}
											
											BluetoothGattDescriptor configDescriptor = characteristic.getDescriptor (uuidClientCharacteristicConfig); // BluetoothGattCharacteristic: API 18, throws nothing
											
											if (configDescriptor == null) {
												ServiceMain.this.onConnectionFailed ("error: configDescriptor == null");
												return;
											}
											
											byte[] value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; // API 18
											
											result = configDescriptor.setValue (value); // BluetoothGattDescriptor: API 18, throws nothing
											if (!result) {
												ServiceMain.this.onConnectionFailed ("error: configDescriptor.setValue() returned false");
												return;
											}
											
											result = gatt.writeDescriptor (configDescriptor); // BluetoothGatt: API 18, throws nothing
											if (!result) {
												ServiceMain.this.onConnectionFailed ("error: configDescriptor.setValue() returned false");
												return;
											}
										}
										
										else if (
											(uuidString != null) &&
											uuidString.equals (UUID_BATTERY_LEVEL)// throws nothing
										) {
											ServiceMain.this.batteryCharacteristic = characteristic;
											ServiceMain.this.bBatteryCharacteristicFound = true;
										}
									}
									
									if (ServiceMain.this.bPulseCharacteristicFound && ServiceMain.this.bBatteryCharacteristicFound)
										break;
								}
								
								if (ServiceMain.this.bPulseCharacteristicFound && ServiceMain.this.bBatteryCharacteristicFound)
									break;
							}
						}
					}
				}
				
				
				ServiceMain.this.bConnectingProcess = false;
				
				if (ServiceMain.this.bPulseCharacteristicFound & ServiceMain.this.bBatteryCharacteristicFound) {
					
					ServiceMain.this.bDeviceConnected = true;
					ServiceMain.this.bMonitorPulseSnoozed = false;
					
					if (ServiceMain.this.pendingChecker != null) {
						ServiceMain.this.pendingChecker.cancel();
						ServiceMain.this.pendingChecker = null;
					}
					
					ServiceMain.this.connectionStatus = "connected";
					
					AppState.handler.post (ServiceMain.this.new BatteryStatusReader()); // Handler: API 1, nothing thrown
					
					ServiceMain.this.updateActivityIfPossible();
				}
				
				else
					ServiceMain.this.onConnectionFailed ("desired characteristics not found");
			}
			
			else {
				gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
				gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
			}
			
		}
		
	}
	
	
	
	private void onConnectionFailed (String errorMsg) {
		this.bConnectingProcess = false;
		this.connectionStatus = errorMsg;
		this.device = null;
		
		if (this.gatt != null) {
			this.gatt.disconnect(); // BluetoothGatt: API 18, returns void, throws nothing
			this.gatt.close(); // BluetoothGatt: API 18, returns void, throws nothing
		}
		
		this.batteryCharacteristic = null;
		this.bPulseCharacteristicFound = false;
		this.bBatteryCharacteristicFound = false;
		
		this.updateActivityIfPossible();
	}
	
	
}
