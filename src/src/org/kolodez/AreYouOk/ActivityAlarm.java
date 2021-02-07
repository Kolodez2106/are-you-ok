package org.kolodez.AreYouOk;

// VERIFIED

import android.app.Activity; // API 1

import android.content.ComponentName; // API 1
import android.content.Intent; // API 1
import android.content.ServiceConnection; // API 1

import android.os.Bundle; // API 1
import android.os.IBinder; // API 1

import android.view.View; // API 1
import android.view.Window; // API 1
import android.view.WindowManager; // API 1

import android.widget.Button; // API 1
import android.widget.TextView; // API 1


public class ActivityAlarm extends Activity { // API 4
	
	private Button btnStopAlarm;
	
	private ServiceAlarm.AlarmBinder binder = null;
	private MyAlarmServiceConnection serviceConnection = null;
	
	
	
	protected void onCreate (Bundle savedInstanceState) {
        
        super.onCreate (savedInstanceState); // Activity: API 1, returns void, nothing thrown
        
        this.setContentView (R.layout.alarm); // Activity: API 1, returns void, nothing thrown
		
		this.btnStopAlarm = (Button) this.findViewById (R.id.ButtonStopAlarm); // Activity: API 1, may return null, nothing thrown
		if (this.btnStopAlarm != null) {
			ButtonStopAlarmListener listener = this.new ButtonStopAlarmListener();
			this.btnStopAlarm.setOnClickListener (listener); // View: API 1, returns void, nothing thrown
			
			this.btnStopAlarm.setClickable (false); // View: API 1, returns void, nothing thrown
		}
		
		
		if (AppState.SDK >= 5) { // API 4
			Window window = this.getWindow(); // Activity: API 1, may return null, nothing thrown
			if (window != null) {
				int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON; // API 5, DEPRECATED in API 27
				
				window.addFlags (flags); // Window, API 1, returns void, nothing thrown
			}
		}
		
		
		Intent intent = new Intent (this, ServiceAlarm.class); // API 1, nothing thrown
		this.serviceConnection = this.new MyAlarmServiceConnection();
		
		try {
			boolean success = this.bindService (intent, this.serviceConnection, 0); // ContextWrapper: API 1, throws SecurityException
			if (!success) {
				this.serviceConnection = null;
				this.finish(); // Activity: API 1, returns void, nothing thrown
			}
		} catch (SecurityException e) {
			this.serviceConnection = null;
			this.finish(); // Activity: API 1, returns void, nothing thrown
		}
	}
	
	protected void onDestroy () {
		if (this.serviceConnection != null)
			this.unbindService (this.serviceConnection); // ContextWrapper: API 1, throws nothing
		
		super.onDestroy(); // Activity: API 1, returns void, nothing thrown
	}
	
	
	
	public void onBackPressed () { }
	
	
	private class ButtonStopAlarmListener implements View.OnClickListener {
		// needed since API 1:
		public void onClick (View v) {
			if (ActivityAlarm.this.binder != null) {
				ActivityAlarm.this.binder.endAlarm(); // returns void, nothing thrown
				ActivityAlarm.this.binder = null;
			}
			
			ActivityAlarm.this.finish(); // Activity: API 1, returns void, nothing thrown
		}
	}
	
	
	
	private class MyAlarmServiceConnection implements ServiceConnection { // API 1
		
		// needed since API 1:
		public void onServiceConnected (ComponentName name, IBinder service) {
			ActivityAlarm.this.binder = (ServiceAlarm.AlarmBinder) service;
			
			if (ActivityAlarm.this.btnStopAlarm != null)
				ActivityAlarm.this.btnStopAlarm.setClickable (true); // View: API 1, returns void, nothing thrown
		}
		
		// needed since API 1:
		public void onServiceDisconnected (ComponentName name) {
			ActivityAlarm.this.finish(); // Activity: API 1, returns void, nothing thrown
		}
		
		// needed since API 26:
		public void onBindingDied (ComponentName name) {
			ActivityAlarm.this.finish(); // Activity: API 1, returns void, nothing thrown
		}
		
		// needed since API 28:
		public void onNullBinding (ComponentName name) {
			ActivityAlarm.this.finish(); // Activity: API 1, returns void, nothing thrown
		}
		
	}
	
	
}
