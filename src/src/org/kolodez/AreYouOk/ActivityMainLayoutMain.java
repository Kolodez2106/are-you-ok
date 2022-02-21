package org.kolodez.AreYouOk;

// VERIFIED

import java.util.Calendar;

import android.app.Activity; // API 1

import android.bluetooth.BluetoothDevice; // API 5

import android.content.ComponentName; // API 1
import android.content.Context; // API 1
import android.content.Intent; // API 1
import android.content.ServiceConnection; // API 1

import android.graphics.Color; // API 1

import android.os.Handler; // API 1
import android.os.IBinder; // API 1

import android.text.Editable; // API 1

import android.view.View; // API 1
import android.view.Window; // API 1
import android.view.WindowManager; // API 1

import android.widget.Button; // API 1
import android.widget.EditText; // API 1
import android.widget.TextView; // API 1


/*
 * This class is only available since API 5.
 * bindToActivity() must have been called before this layout is opened. (Assumption 1)
 */
public class ActivityMainLayoutMain extends MultipleLayoutActivityLayout {
	
	private final static int[] buttonIdArray = {
		R.id.ButtonStartStopMonitor,
		R.id.ButtonSnooze,
		R.id.ButtonConnectDisconnect,
		R.id.ButtonEditContacts,
		R.id.ButtonMinPulseDown,
		R.id.ButtonMinPulseUp,
		R.id.ButtonMaxPulseDown,
		R.id.ButtonMaxPulseUp,
		R.id.ButtonTimeForCheckingDown,
		R.id.ButtonTimeForCheckingUp,
		R.id.ButtonMainTestLocation,
		R.id.ButtonMainTestBeep,
		R.id.ButtonMainHelp
	};
	
	private final static int N_BUTTONS = 13;
	
	private final static int BUTTON_START_STOP_MONITOR = 0;
	private final static int BUTTON_SNOOZE = 1;
	private final static int BUTTON_CONNECT_DISCONNECT = 2;
	private final static int BUTTON_MIN_PULSE_DOWN = 4;
	private final static int BUTTON_MIN_PULSE_UP = 5;
	private final static int BUTTON_MAX_PULSE_DOWN = 6;
	private final static int BUTTON_MAX_PULSE_UP = 7;
	private final static int BUTTON_TIME_FOR_CHECKING_DOWN = 8;
	private final static int BUTTON_TIME_FOR_CHECKING_UP = 9;
	private final static int BUTTON_TEST_LOCATION = 10;
	private final static int BUTTON_TEST_BEEP = 11;
	private final static int BUTTON_HELP = 12;
	
	
	private final static int[] textViewIdArray = {
		R.id.TextNextCheck,
		R.id.TextPulse,
		R.id.TextAreYouOk,
		R.id.TextShortContactList,
		R.id.TextMinPulseValue,
		R.id.TextMaxPulseValue,
		R.id.TextTimeForCheckingValue
	};
	
	private final static int N_TEXTVIEWS = 7;
	
	private final static int TEXTVIEW_NEXT_CHECK = 0;
	private final static int TEXTVIEW_PULSE = 1;
	private final static int TEXTVIEW_ARE_YOU_OK = 2;
	private final static int TEXTVIEW_CONTACT_LIST = 3;
	private final static int TEXTVIEW_MIN_PULSE_VALUE = 4;
	private final static int TEXTVIEW_MAX_PULSE_VALUE = 5;
	private final static int TEXTVIEW_TIME_FOR_CHECKING_VALUE = 6;
	
	
	// we later use that [0]=EditMinutes, [1]=EditHours
	private final static int[] editTextIdArray = {
		R.id.EditMinutes,
		R.id.EditHours
	};
	
	private final static int N_EDITTEXTS = 2;
	
	
	
	private Button[] button = new Button [N_BUTTONS];
	private TextView[] textView = new TextView [N_TEXTVIEWS];
	private EditText[] editText = new EditText [N_EDITTEXTS];
	
	
	private ServiceMain.MyBinder binder = null;
	private MyServiceConnection serviceConnection = null;
	
	private PeriodicBeep testBeepInstance;
	
	
	
	private boolean bPeriodicUpdaterStarted = false;
	private boolean bLayoutActive = false;
	
	
	public void init () {
		this.bLayoutActive = true;
		
		if (AppState.bMonitoring)
			this.startMonitor();
	}
	
	
	protected void onStart (int i) {
        Activity activity = this.getActivity (); // this does not return null due to Assumption 1
        
        activity.setContentView (R.layout.monitor); // Activity: API 1, returns void, nothing thrown
        
		
		for (int iTextView = 0; iTextView < textView.length; iTextView++) {
			this.textView [iTextView] = (TextView) activity.findViewById (textViewIdArray [iTextView]); // Activity: API 1, may return null, nothing thrown
			if (this.textView [iTextView] == null) {
				activity.finish ();
				return;
			}
		}
		
		for (int iEditText = 0; iEditText < editText.length; iEditText++) {
			this.editText [iEditText] = (EditText) activity.findViewById (editTextIdArray [iEditText]); // Activity: API 1, may return null, nothing thrown
			if (this.editText [iEditText] == null) {
				activity.finish ();
				return;
			}
		}
		
        for (int iButton = 0; iButton < button.length; iButton++) {
			this.button [iButton] = (Button) activity.findViewById (buttonIdArray [iButton]); // Activity: API 1, may return null, nothing thrown
			if (this.button [iButton] == null) {
				activity.finish ();
				return;
			}
			
			ButtonListener listener = this.new ButtonListener (buttonIdArray [iButton]);
			this.button [iButton].setOnClickListener (listener); // View: API 1, returns void, nothing thrown
		}
		
		
		
		String textContacts;
		if (AppState.contactList.isEmpty ()) { // List: nothing thrown (contactList is not null because AppState.initIfNecessary() is called in ActivityMain.onCreate() before this class is created)
			
			int red = Color.rgb (255,0,0); // API 1, nothing thrown
			this.textView [TEXTVIEW_CONTACT_LIST].setTextColor (red); // TextView: API 1, returns void, nothing thrown
			
			textContacts = "No contacts selected!";
		}
		else {
			
			int white = Color.rgb (255,255,255); // API 1, nothing thrown
			this.textView [TEXTVIEW_CONTACT_LIST].setTextColor (white); // TextView: API 1, returns void, nothing thrown
			
			
			String textLong = "My contacts: ";
			
			for (ContactListEntry entry : AppState.contactList)
				textLong += entry.name + ", ";
			
			int length = textLong.length(); // nothing thrown
			try {
				textContacts = textLong.substring (0, length - 2); // throws IndexOutOfBoundsException
			} catch (IndexOutOfBoundsException e) {
				textContacts = textLong;
			}
		}
		try {
			this.textView [TEXTVIEW_CONTACT_LIST].setText (textContacts); // TextView: API 1, returns void, IllegalArgumentException is thrown
		} catch (IllegalArgumentException e) { }
		
		
		String textMonitoringStartStop;
		if (AppState.bMonitoring) {
			textMonitoringStartStop = "STOP MONITORING";
			this.button [BUTTON_START_STOP_MONITOR].setClickable (this.binder != null); // View: API 1, returns void, nothing thrown
		}
		else {
			textMonitoringStartStop = "START MONITORING";
			this.button [BUTTON_START_STOP_MONITOR].setClickable (true); // View: API 1, returns void, nothing thrown
		}
		try {
			this.button [BUTTON_START_STOP_MONITOR].setText (textMonitoringStartStop); // TextView: API 1, returns void, IllegalArgumentException is thrown
		} catch (IllegalArgumentException e) { }
		
		this.setButtonsClickableDependingOnMonitoring();
		
		Updater updater = this.new Updater (!this.bPeriodicUpdaterStarted);
		AppState.handler.post (updater); // Handler: API 1, returns boolean, nothing thrown
		this.bPeriodicUpdaterStarted = true;
		
		
		this.putInFrontOfLockScreenIfChecking();
	}
	
	protected void onEnd () {
		this.endTestBeep ();
	}
	
	
	public void destroy () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		if (this.serviceConnection != null) {
			activity.unbindService (this.serviceConnection); // ContextWrapper: API 1, throws nothing
			this.serviceConnection = null;
		}
		
		this.bLayoutActive = false;
	}
	
	
	
	
	public void updateNow () {
		ServiceMain.MyBinder myBinder = this.binder; // may be null because service not yet bind
		if (myBinder == null)
			return;
		
		
		Calendar nextCheckTime = myBinder.getNextCheckTime();
		
		String nextCheckTimeDescription;
		if (nextCheckTime == null)
			nextCheckTimeDescription = "null";
		else
			nextCheckTimeDescription = ServiceMain.printTime (nextCheckTime) + " (in " + myBinder.getDifferenceToCheckTime() / 1000 + " s)";
		
		
		try {
			this.textView [TEXTVIEW_MIN_PULSE_VALUE].setText (String.valueOf (myBinder.getMinPulse())); // TextView: API 1, returns void, IllegalArgumentException is thrown (valueof does not throw)
			this.textView [TEXTVIEW_MAX_PULSE_VALUE].setText (String.valueOf (myBinder.getMaxPulse()));
			this.textView [TEXTVIEW_TIME_FOR_CHECKING_VALUE].setText (String.valueOf (myBinder.getTimeForChecking()) + " s");
		} catch (IllegalArgumentException e) { }
		
		
		String textAreYouOk;
		if (myBinder.isChecking())
			textAreYouOk = "Alarm in " + (myBinder.getDifferenceToAlarmTime() / 1000) + ". Please snooze if you are ok.";
		else
			textAreYouOk = "";
		
		try {
			this.textView [TEXTVIEW_ARE_YOU_OK].setText (textAreYouOk); // TextView: API 1, returns void, IllegalArgumentException is thrown
		} catch (IllegalArgumentException e) { }
		
		
		
		String textNextCheck, textPulse, textButtonConnectDisconnect;
		
		if (myBinder.isDeviceConnected()) {
			
			if (myBinder.isPulseMonitorSnoozed())
				textNextCheck = "Pulse snoozed until: " + nextCheckTimeDescription;
			else
				textNextCheck = "Pulse is monitored";
			
			textPulse = "Pulse: " + myBinder.getPulse() + ", battery level: " + myBinder.getBatteryLevel();
			
			textButtonConnectDisconnect = "disconnect pulse device";
			
			this.button [BUTTON_CONNECT_DISCONNECT].setClickable (true); // View: API 1, returns void, nothing thrown
		}
		
		else {
			textNextCheck = "Next check: " + nextCheckTimeDescription;
			
			textPulse = "Pulse: " + myBinder.getConnectionStatus();
			
			if (myBinder.isConnectingProcess())
				textButtonConnectDisconnect = "TODO: stop connecting";
			else {
				if (AppState.SDK >= 18)
					textButtonConnectDisconnect = "connect pulse device";
				else
					textButtonConnectDisconnect = "(pulse device not supported on this Android version)";
			}
			
			this.button [BUTTON_CONNECT_DISCONNECT].setClickable ((AppState.SDK >= 18) && !myBinder.isConnectingProcess()); // View: API 1, returns void, nothing thrown
		}
		
		try {
			this.textView [TEXTVIEW_NEXT_CHECK].setText (textNextCheck); // TextView: API 1, returns void, IllegalArgumentException is thrown
			this.textView [TEXTVIEW_PULSE].setText (textPulse);
			this.button [BUTTON_CONNECT_DISCONNECT].setText (textButtonConnectDisconnect); // TextView: ...
			
		} catch (IllegalArgumentException e) { }
	}
	
	
	private class Updater implements Runnable {
		
		private boolean bPeriodic;
		
		public Updater (boolean bPeriodic) { this.bPeriodic = bPeriodic; }
		
		public void run () {
			if (ActivityMainLayoutMain.this.bLayoutActive) {
				ActivityMainLayoutMain.this.updateNow();
				
				if (this.bPeriodic)
					AppState.handler.postDelayed (this, 1000); // Handler: API 1, returns boolean, nothing thrown
			}
		}
	}
	
	
	
	
	
	private void snooze () {
		if (AppState.bMonitoring && (this.binder != null)) {
			
			int minutes = 0;
			
			for (int i = 0; i < 2; i++) {
				int factor = 0;
				if (editTextIdArray [i] == R.id.EditMinutes)
					factor = 1;
				else if (editTextIdArray [i] == R.id.EditHours)
					factor = 60;
				
				Editable editable = this.editText [i].getText(); // EditText: API 1, nothing thrown
				if (editable == null)
					continue;
				
				String text = editable.toString(); // CharSequence: nothing thrown
				if (text == null)
					continue;
				
				int value;
				try {
					value = Integer.parseInt (text); // throws NumberFormatException
				} catch (NumberFormatException e) { continue; }
				
				minutes += factor * value;
			}
			
			this.binder.snooze (0, minutes, 0);
			
			try {
				this.editText [1].setText ("0"); // TextView: API 1, returns void, IllegalArgumentException is thrown
				this.editText [0].setText ("1");
			} catch (IllegalArgumentException e) { }
		}
	}
	
	
	
	private void addValue (int id, int summand) {
		if (AppState.bMonitoring && (ActivityMainLayoutMain.this.binder != null))
			this.binder.addValue (id, summand);
	}
	
	
	private void connectDisconnect () {
		if (AppState.bMonitoring && (ActivityMainLayoutMain.this.binder != null)) {
			if (this.binder.isDeviceConnected ())
				this.binder.disconnectDevice ();
			else {
				if (AppState.SDK >= 18) {
					ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
					activity.changeLayout (ActivityMain.LAYOUT_DEVICE_SEARCH);
				}
			}
		}
	}
	
	private void editContacts () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		activity.changeLayout (ActivityMain.LAYOUT_CONTACT_LIST);
	}
	
	private void help () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		activity.changeLayout (ActivityMain.LAYOUT_HELP);
	}
	
	private void testLocation () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		activity.changeLayout (ActivityMain.LAYOUT_TEST_LOCATION);
	}
	
	private void testBeep () {
		this.button [BUTTON_TEST_BEEP].setClickable (false); // View: API 1, returns void, nothing thrown
		
		this.testBeepInstance = PeriodicBeep.create (1000, 3000, PeriodicBeep.MAX_VOLUME);
		if (this.testBeepInstance != null)
			AppState.handler.postDelayed (this.new EndTestBeep (), 6000); // Handler: API 1, returns boolean, nothing thrown
	}
	
	private class EndTestBeep implements Runnable {
		public void run() {
			ActivityMainLayoutMain.this.endTestBeep();
		}
	}
	
	private void endTestBeep () {
		if (this.testBeepInstance != null) {
			this.testBeepInstance.end();
			this.testBeepInstance = null;
		}
		
		this.button [BUTTON_TEST_BEEP].setClickable (true); // View: API 1, returns void, nothing thrown
	}
	
	
	
	private void startStopMonitor () {
		if (AppState.bMonitoring)
			this.stopMonitor();
		else
			this.startMonitor();
	}
	
	
	private void startMonitor () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		Intent intent = new Intent (activity, ServiceMain.class); // API 1, nothing thrown
		
		ComponentName componentName;
		try {
			componentName = activity.startService (new Intent (this.getActivity (), ServiceMain.class)); // ContextWrapper: API 1, may return null on error, throws SecurityException
		} catch (SecurityException e) { return; }
		if (componentName == null)
			return;
		
		this.serviceConnection = this.new MyServiceConnection();
		intent = new Intent (activity, ServiceMain.class); // API 1, nothing thrown
		
		try {
			boolean success = activity.bindService (intent, this.serviceConnection, 0); // ContextWrapper: API 1, returns boolean, throws SecurityException
			if (!success)
				this.serviceConnection = null;
		} catch (SecurityException e) {
			this.serviceConnection = null;
		}
	}
	
	private void stopMonitor () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		if (this.binder != null) {
			this.binder.end();
			this.binder = null;
		}
		
		if (this.serviceConnection != null) {
			activity.unbindService (this.serviceConnection); // ContextWrapper: API 1, throws nothing
			this.serviceConnection = null;
		}
		
		this.button [BUTTON_START_STOP_MONITOR].setClickable (true); // View: API 1, returns void, nothing thrown
		
		this.setButtonsClickableDependingOnMonitoring();
		
		try {
			this.button [BUTTON_START_STOP_MONITOR].setText ("START MONITORING");// TextView: API 1, returns void, IllegalArgumentException is thrown
			
			this.textView [TEXTVIEW_NEXT_CHECK].setText("");
			this.textView [TEXTVIEW_PULSE].setText("");
			this.textView [TEXTVIEW_ARE_YOU_OK].setText("");
			
			this.textView [TEXTVIEW_MAX_PULSE_VALUE].setText("");
			this.textView [TEXTVIEW_MIN_PULSE_VALUE].setText("");
			this.textView [TEXTVIEW_TIME_FOR_CHECKING_VALUE].setText("");
		} catch (IllegalArgumentException e) { }
	}
	
	
	
	
	
	
	
	
	
	private class ButtonListener implements View.OnClickListener {
		
		private int id;
		
		public ButtonListener (int id) { this.id = id; }
		
		public void onClick (View v) {
			
			if (this.id == R.id.ButtonStartStopMonitor)
				ActivityMainLayoutMain.this.startStopMonitor();
			
			else if (this.id == R.id.ButtonEditContacts)
				ActivityMainLayoutMain.this.editContacts();
				
			else if (this.id == R.id.ButtonMainTestLocation)
				ActivityMainLayoutMain.this.testLocation();
			
			else if (this.id == R.id.ButtonMainTestBeep)
				ActivityMainLayoutMain.this.testBeep();
			
			else if (this.id == R.id.ButtonMainHelp)
				ActivityMainLayoutMain.this.help();
			
			else {
				if (AppState.bMonitoring && (ActivityMainLayoutMain.this.binder != null)) {
					
					if (this.id == R.id.ButtonSnooze)
						ActivityMainLayoutMain.this.snooze();
					
					else if (this.id == R.id.ButtonConnectDisconnect)
						ActivityMainLayoutMain.this.connectDisconnect();
					
					else if (this.id == R.id.ButtonMinPulseDown)
						ActivityMainLayoutMain.this.binder.addValue (-1, -5);
					
					else if (this.id == R.id.ButtonMinPulseUp)
						ActivityMainLayoutMain.this.binder.addValue (-1, 5);
					
					else if (this.id == R.id.ButtonMaxPulseDown)
						ActivityMainLayoutMain.this.binder.addValue (1, -5);
					
					else if (this.id == R.id.ButtonMaxPulseUp)
						ActivityMainLayoutMain.this.binder.addValue (1, 5);
					
					else if (this.id == R.id.ButtonTimeForCheckingDown)
						ActivityMainLayoutMain.this.binder.addValue (0, -5);
					
					else if (this.id == R.id.ButtonTimeForCheckingUp)
						ActivityMainLayoutMain.this.binder.addValue (0, 5);
					
				}
			}
			
		}
		
	}
	
	
	private void setButtonsClickableDependingOnMonitoring () {
		boolean bClickable = (AppState.bMonitoring && (this.binder != null));
		
		this.button [BUTTON_SNOOZE].setClickable (bClickable); // View: API 1, returns void, nothing thrown
		this.button [BUTTON_CONNECT_DISCONNECT].setClickable (bClickable && (AppState.SDK >= 18));
		this.button [BUTTON_MIN_PULSE_DOWN].setClickable (bClickable);
		this.button [BUTTON_MIN_PULSE_UP].setClickable (bClickable);
		this.button [BUTTON_MAX_PULSE_DOWN].setClickable (bClickable);
		this.button [BUTTON_MAX_PULSE_UP].setClickable (bClickable);
		this.button [BUTTON_TIME_FOR_CHECKING_DOWN].setClickable (bClickable);
		this.button [BUTTON_TIME_FOR_CHECKING_UP].setClickable (bClickable);
	}
	
	
	private class MyServiceConnection implements ServiceConnection {
		
		// needed since API 1:
		public void onServiceConnected (ComponentName name, IBinder service) {
			ActivityMainLayoutMain.this.bindServiceMain (service);
		}
		
		// needed since API 1:
		public void onServiceDisconnected (ComponentName name) {
			ActivityMainLayoutMain.this.unbindServiceMain ();
		}
		
		// needed since API 26:
		public void onBindingDied (ComponentName name) { }
		
		// needed since API 28:
		public void onNullBinding (ComponentName name) { }
		
	}
	
	
	private void bindServiceMain (IBinder service) {
		this.binder = (ServiceMain.MyBinder) service;
		
		if (this.binder != null) {
			this.binder.registerStateChangeListener (new ServiceListener());
			
			this.putInFrontOfLockScreenIfChecking ();
			
			try {
				this.button [BUTTON_START_STOP_MONITOR].setText ("STOP MONITORING"); // TextView: API 1, returns void, IllegalArgumentException is thrown
			} catch (IllegalArgumentException e) { }
			
			this.button [BUTTON_START_STOP_MONITOR].setClickable (true); // View: API 1, returns void, nothing thrown
			
			this.setButtonsClickableDependingOnMonitoring();
			
			this.updateNow ();
		}
	}
	
	private void unbindServiceMain () {
		ActivityMain activity = (ActivityMain) ActivityMainLayoutMain.this.getActivity (); // this does not return null due to Assumption 1
		
		if (this.binder != null) {
			this.binder.unregisterStateChangeListener();
			this.binder = null;
		}
		
		if (this.serviceConnection != null) {
			activity.unbindService (this.serviceConnection);
			this.serviceConnection = null;
		}
		
		try {
			this.button [BUTTON_START_STOP_MONITOR].setText ("START MONITORING"); // TextView: API 1, returns void, IllegalArgumentException is thrown
		} catch (IllegalArgumentException e) { }
		
		this.button [BUTTON_START_STOP_MONITOR].setClickable (true); // View: API 1, returns void, nothing thrown
		
		this.setButtonsClickableDependingOnMonitoring();
	}
	
	public class ServiceListener {
		public void update() {
			ActivityMain activity = (ActivityMain) ActivityMainLayoutMain.this.getActivity (); // this does not return null due to Assumption 1
			
			Updater updater = ActivityMainLayoutMain.this.new Updater (false);
			AppState.handler.post (updater); // Handler: API 1, returns boolean, nothing thrown
		}
	}
	
	
	
	private void putInFrontOfLockScreenIfChecking () {
		if ((this.binder != null) && (this.binder.isChecking())) {
			Activity activity = this.getActivity (); // this does not return null due to Assumption 1
			
			if (AppState.SDK >= 5) { // API 4
				Window window = activity.getWindow(); // Activity: API 1, may return null, nothing thrown
				if (window != null) {
					int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON; // API 5, DEPRECATED in API 27
					
					window.addFlags (flags); // Window: API 1, returns void, nothing thrown
				}
			}
		}
	}
	
	
	public void connectDevice (BluetoothDevice device) {
		if (this.binder != null)
			this.binder.connectDevice (device);
	}
	
	
}
