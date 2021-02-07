package org.kolodez.AreYouOk;

// VERIFIED

import android.app.Activity; // API 1

import android.bluetooth.BluetoothDevice; // API 5

import android.content.ActivityNotFoundException; // API 1
import android.content.Intent; // API 1

import android.os.Bundle; // API 1

import android.widget.TextView; // API 1

/*
 * This class is available since API 5.
 */
public class ActivityMain extends MultipleLayoutActivity {
	
	public final static int LAYOUT_MAIN = 0;
	public final static int LAYOUT_CONTACT_LIST = 1;
	public final static int LAYOUT_DEVICE_SEARCH = 2;
	public final static int LAYOUT_TEST_LOCATION = 3;
	public final static int LAYOUT_HELP = 4;
	
	public final static int N_LAYOUTS = 5;
	
	
	
	protected MultipleLayoutActivityLayout[] getLayouts() { return layout; }
	
	private MultipleLayoutActivityLayout[] layout;
	
	private boolean bStarted;
	
	
	
	
	private boolean replaceByOtherActivityIfNecessary() {
		if (AppState.mode != AppMode.MONITOR) {
			
			Intent intent = new Intent (this, AppMode.getActivityClass (AppState.mode)); // API 1, nothring thrown
			
			try {
				this.startActivity (intent); // Activity: API 1, returns void, throws ActivityNotFoundException
			} catch (ActivityNotFoundException e) { }
			
			this.finish (); // Activity: API 1, returns void, nothing thrown
			
			return true;
		}
		else
			return false;
	}
	
	
	
	
	protected void onCreate (Bundle savedInstanceState) {
        
        super.onCreate (savedInstanceState); // Activity: API 1, returns void, nothing thrown
        
        
        if (AppState.SDK < 16) {
			String info = "This app is only supported since API level 16 (Android 4.1 Jelly Bean), and connecting to a pulse device is supported only since API level 21 (Android 5.0 Lollipop).";
			
			this.setContentView (R.layout.not_supported); // Activity: API 1, returns void, nothing thrown
			
			TextView textInfo = (TextView) this.findViewById (R.id.TextNotSupported); // Activity: API 1, may return null, nothing thrown
			
			try {
				textInfo.setText (info); // TextView: API 1, returns void, IllegalArgumentException is thrown
			} catch (IllegalArgumentException e) { }
			
			if (AppState.SDK >= 11) // API 4
				textInfo.setTextIsSelectable (true); // TextView: API 11, returns void, nothing thrown
			
			return;
		}
        
        
        
        try {
			AppState.initIfNecessary (this);
		} catch (NullPointerException e) {
			this.finish(); // Activity: API 1, returns void, nothing thrown
			return;
		}
        
        if (this.replaceByOtherActivityIfNecessary())
			return;
        
        this.layout = new MultipleLayoutActivityLayout [N_LAYOUTS];
		this.layout [LAYOUT_MAIN] = new ActivityMainLayoutMain ();
		this.layout [LAYOUT_CONTACT_LIST] = new ActivityMainLayoutContactList ();
		this.layout [LAYOUT_TEST_LOCATION] = new ActivityMainLayoutLocationTest ();
		this.layout [LAYOUT_HELP] = new ActivityMainLayoutHelp ();
		
		if (AppState.SDK >= 21)
			this.layout [LAYOUT_DEVICE_SEARCH] = new ActivityMainLayoutDeviceSearch ();
		else
			this.layout [LAYOUT_DEVICE_SEARCH] = new MultipleLayoutActivityLayout.EmptyLayout ();
		
		for (int i = 0; i < N_LAYOUTS; i++)
			this.layout [i].bindToActivity (this);
		
		((ActivityMainLayoutMain) this.layout [LAYOUT_MAIN]).init ();
		
		
		this.bStarted = false;
	}
	
	
	protected void onResume() {
		super.onResume(); // Activity: API 1, returns void, nothing thrown
		
		if (AppState.SDK < 16)
			return;
		
		if (this.replaceByOtherActivityIfNecessary ())
			return;
		
		if (!this.bStarted) {
			this.start (LAYOUT_MAIN);
			this.bStarted = true;
		}
		else
			this.changeLayout (LAYOUT_MAIN);
	}
	
	protected void onDestroy () {
		if (this.layout != null)
			((ActivityMainLayoutMain) this.layout [LAYOUT_MAIN]).destroy ();
		
		super.onDestroy();
	}
	
	
	public void onBackPressed () {
		if (AppState.SDK < 16) {
			super.onBackPressed ();
			return;
		}
		
		if (
			(this.getActiveLayout() == LAYOUT_CONTACT_LIST) ||
			(this.getActiveLayout() == LAYOUT_DEVICE_SEARCH) ||
			(this.getActiveLayout() == LAYOUT_TEST_LOCATION) ||
			(this.getActiveLayout() == LAYOUT_HELP)
		)
			this.changeLayout (LAYOUT_MAIN);
	}
	
	
	
	public void connectDevice (BluetoothDevice device) {
		this.changeLayout (LAYOUT_MAIN);
		
		((ActivityMainLayoutMain) ActivityMain.this.layout [LAYOUT_MAIN]).connectDevice (device);
	}
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (this.getActiveLayout() == LAYOUT_CONTACT_LIST)
			((ActivityMainLayoutContactList) this.layout [LAYOUT_CONTACT_LIST]).onActivityResult (requestCode, resultCode, data);
	}
	
}
