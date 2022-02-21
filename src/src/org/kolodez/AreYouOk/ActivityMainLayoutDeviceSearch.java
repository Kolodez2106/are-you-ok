package org.kolodez.AreYouOk;

// VERIFIED

import java.util.IllegalFormatException;
import java.util.List;

import android.app.Activity; // API 1

import android.bluetooth.BluetoothAdapter; // API 5
import android.bluetooth.BluetoothDevice; // API 5
import android.bluetooth.BluetoothManager; // API 18

import android.content.BroadcastReceiver; // API 1
import android.content.Context; // API 1
import android.content.Intent; // API 1
import android.content.IntentFilter; // API 1
import android.content.pm.PackageManager; // API 1

import android.view.InflateException; // API 1
import android.view.LayoutInflater; // API 1
import android.view.View; // API 1
import android.view.ViewGroup; // API 1

import android.widget.ArrayAdapter; // API 1
import android.widget.Button; // API 1
import android.widget.ListView; // API 1
import android.widget.TextView; // API 1

/*
 * This class is only available since API 18.
 * bindToActivity() must have been called before this layout is opened. (Assumption 1)
 */
public class ActivityMainLayoutDeviceSearch extends MultipleLayoutActivityLayout {
	
	private ListView listView;
	private MyArrayAdapter adapter = null;
	
	private Button[] button = new Button[2];
	
	private static int[] buttonIdArray = {
		R.id.ButtonDeviceSearchRestart,
		R.id.ButtonDeviceSearchCancel
	};
	
	private TextView tvError;
	
	private Discovery discovery;
	
	
	
	protected void onStart (int i) {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
        activity.setContentView (R.layout.device_search); // Activity: API 1, returns void, nothing thrown
        
        this.tvError = (TextView) activity.findViewById (R.id.TextDeviceSearchError); // Activity: API 1, may return null, nothing thrown
        if (this.tvError == null) {
			activity.changeLayout (ActivityMain.LAYOUT_MAIN);
			return;
		}
        
        for (int iButton = 0; iButton < this.button.length; iButton++) {
			this.button [iButton] = (Button) activity.findViewById (buttonIdArray [iButton]); // Activity: API 1, may return null, nothing thrown
			if (this.button [iButton] == null) {
				activity.changeLayout (ActivityMain.LAYOUT_MAIN);
				return;
			}
			
			ButtonListener listener = this.new ButtonListener (buttonIdArray [iButton]);
			this.button [iButton].setOnClickListener (listener); // View: API 1, returns void, nothing thrown
		}
		
		
		this.listView = (ListView) activity.findViewById (R.id.ListViewDevices); // Activity: API 1, may return null, nothing thrown
		if (this.listView == null) {
			activity.changeLayout (ActivityMain.LAYOUT_MAIN);
			return;
		}
		
		
		this.adapter = new MyArrayAdapter (activity);
		this.listView.setAdapter (this.adapter); // ListView: API 1, returns void, nothing thrown
		
		
		
		this.discovery = this.new Discovery();
		
		String errorMsg = this.discovery.start();
		if (errorMsg != null) {
			this.discovery = null;
			try {
				this.tvError.setText (errorMsg); // TextView: API 1, returns void, IllegalArgumentException is thrown
			} catch (IllegalArgumentException e) { }
		}
		
	}
	
	
	protected void onEnd () {
		if (this.discovery != null) {
			this.discovery.end();
			this.discovery = null;
		}
		
		try {
			this.adapter.clear (); // ArrayAdapter: API 1, returns void, throws UnsupportedOperationException
		} catch (UnsupportedOperationException e) { }
	}
	
	
	
	private void closeDeviceSearch () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		activity.changeLayout (ActivityMain.LAYOUT_MAIN);
	}
	
	private void restartDeviceSearch () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		activity.changeLayout (ActivityMain.LAYOUT_DEVICE_SEARCH);
	}
	
	private void selectDevice (BluetoothDevice device) {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		activity.connectDevice (device);
	}
	
	private void onDeviceFound (BluetoothDevice device) {
		if (device == null)
			return;
		
		String address = device.getAddress (); // BluetoothDevice: API 5, nothing thrown
		if (address == null)
			return;
		
		boolean bDeviceAlreadyFound = false;
		int nOldDevices = this.adapter.getCount (); // ArrayAdapter: API 1, nothing thrown
		
		for (int i = 0; i < nOldDevices; i++) {
			BluetoothDevice deviceOld = this.adapter.getItem (i); // ArrayAdapter: API 1, nothing thrown
			String addressOld = deviceOld.getAddress(); // BluetoothDevice: API 5, nothing thrown
			
			if (address.equals (addressOld)) {
				bDeviceAlreadyFound = true;
				break;
			}
		}
		
		if (!bDeviceAlreadyFound) {
			try {
				this.adapter.add (device); // ArrayAdapter: API 1, returns void, throws UnsupportedOperationException
			} catch (UnsupportedOperationException e) { }
		}
	}
	
	
	
	private class ButtonListener implements View.OnClickListener {
		private int id;
		
		public ButtonListener (int id) { this.id = id; }
		
		public void onClick (View v) {
			if (this.id == R.id.ButtonDeviceSearchCancel)
				ActivityMainLayoutDeviceSearch.this.closeDeviceSearch();
			
			else if (this.id == R.id.ButtonDeviceSearchRestart)
				ActivityMainLayoutDeviceSearch.this.restartDeviceSearch();
		}
	}
	
	
	private class MyArrayAdapter extends ArrayAdapter <BluetoothDevice> { // API 5
		
		public MyArrayAdapter (Context context) {
			super (context, android.R.layout.simple_list_item_1); // ArrayAdapter: API 1, nothing thrown
		}
		
		public View getView (int position, View convertView, ViewGroup parent) {
			ActivityMain activity = (ActivityMain) ActivityMainLayoutDeviceSearch.this.getActivity (); // this does not return null due to Assumption 1
			
			View result = convertView;
			if (result == null) {
				String layoutInflaterService = Context.LAYOUT_INFLATER_SERVICE; // API 1
				LayoutInflater inflater = (LayoutInflater) activity.getSystemService (layoutInflaterService); // Context: API 1, nothing thrown
				
				try {
					result = inflater.inflate (R.layout.list_item, null); // LayoutInflater: API 1, throws InflateException
				} catch (InflateException e) {
					activity.changeLayout (ActivityMain.LAYOUT_MAIN);
					return null;
				}
				
				if (result == null) {
					activity.changeLayout (ActivityMain.LAYOUT_MAIN);
					return null;
				}
			}
			
			TextView text = (TextView) result.findViewById (R.id.ListItemText); // View: API 1, may return null, nothing thrown
			if (text == null) {
				activity.changeLayout (ActivityMain.LAYOUT_MAIN);
				return null;
			}
			
			BluetoothDevice device = this.getItem (position); // ArrayAdapter: API 1, nothing thrown
			String newText;
			
			if (device == null)
				newText = "null";
			else {
				String name = device.getName(); // BluetoothDevice: API 5, nothing thrown
				String address = device.getAddress(); // BluetoothDevice: API 5, nothing thrown
				
				if (name == null)
					name = "null";
				if (address == null)
					address = "null";
				
				try {
					newText = String.format("%s (%s)", name, address); // throws IllegalFormatException, NullPointerException
				} catch (IllegalFormatException e) { newText = "null"; }
				catch (NullPointerException e) { newText = "null"; }
			}
			
			try {
				text.setText (newText); // TextView: API 1, returns void, IllegalArgumentException is thrown
			} catch (IllegalArgumentException e) { }
			
			Button selectButton = (Button) result.findViewById (R.id.ListItemButton); // View: API 1, may return null, nothing thrown
			if (selectButton != null) {
				
				ListButtonListener listener = this.new ListButtonListener (device);
				selectButton.setOnClickListener (listener); // View: API 1, returns void, nothing thrown
				
				try {
					selectButton.setText ("select"); // TextView: API 1, returns void, IllegalArgumentException is thrown
				} catch (IllegalArgumentException e) { }
			}
			
			return result;
		}
		
		private class ListButtonListener implements View.OnClickListener {
			private BluetoothDevice device;
			public ListButtonListener (BluetoothDevice device) { this.device = device; }
			public void onClick (View v) {
				ActivityMainLayoutDeviceSearch.this.selectDevice (this.device);
			}
		}
		
	}
	
	
	
	private class Discovery extends BroadcastReceiver {
		
		private BluetoothAdapter bluetoothAdapter;
		
		private ActivityMain activity;
		
		
		private Discovery() {
			this.activity = (ActivityMain) ActivityMainLayoutDeviceSearch.this.getActivity (); // this does not return null due to Assumption 1
		}
		
		
		// if the return value is not null, it contains an error message
		private String setAdapter() {
			PackageManager packageManager = this.activity.getPackageManager(); // Context: API 1, nothing thrown
			if (packageManager == null)
				return "activity.getPackageManager() returned null";
			
			String bluetoothService = Context.BLUETOOTH_SERVICE; // API 18
			BluetoothManager bluetoothManager;
			try {
				bluetoothManager = (BluetoothManager) activity.getSystemService (bluetoothService); // Context: API 1, nothing thrown
			} catch (ClassCastException e) { return "activity.getSystemService (Context.BLUETOOTH_SERVICE) could not be casted to BluetoothManager"; }
			if (bluetoothManager == null)
				return "activity.getSystemService (Context.BLUETOOTH_SERVICE) returned null";
			
			this.bluetoothAdapter = bluetoothManager.getAdapter(); // BluetoothManager: API 18, nothing thrown
			if (this.bluetoothAdapter == null)
				return "bluetoothManager.getAdapter() returned null";
			else
				return null;
		}
		
		
		// if this function returned null ( = no error), a single call to end() later is necessary
		private String start () {
			
			String error = this.setAdapter();
			if (error != null)
				return error;
			
			if (!this.bluetoothAdapter.isEnabled()) // BluetoothAdapter: API 5, nothing thrown
				return "bluetooth adapter not enabled";
			
			
			if (this.bluetoothAdapter.isDiscovering()) { // BluetoothAdapter: API 5, nothing thrown
				if (!this.bluetoothAdapter.cancelDiscovery()) // BluetoothAdapter: API 5, nothing thrown
					return "bluetoothAdapter.cancelDiscovery() failed";
			}
			
			
			String actionFound = BluetoothDevice.ACTION_FOUND; // API 5
			IntentFilter intentFilter = new IntentFilter (actionFound); // API 1, nothing thrown
			
			this.activity.registerReceiver (this, intentFilter); // ContextWrapper: API 1, returns Intent (if sticky intent found), throws nothing
			
			if (!this.bluetoothAdapter.startDiscovery()) { // BluetoothAdapter: API 5, nothing thrown
				this.activity.unregisterReceiver (this); // ContextWrapper: API 1, returns void, throws nothing
				return "bluetoothAdapter.startDiscovery() failed";
			}
			
			return null;
		}
		
		// call only if start() returned true
		private void end () {
			
			if (this.bluetoothAdapter.isDiscovering()) { // BluetoothAdapter: API 5, nothing thrown
				this.bluetoothAdapter.cancelDiscovery(); // BluetoothAdapter: API 5, returns boolean, throws nothing
			}
			
			this.activity.unregisterReceiver (this); // ContextWrapper: API 1, returns void, throws nothing
		}
		
		
		
		public void onReceive (Context context, Intent intent) {
			String action = intent.getAction(); // Intent: API 1, nothing thrown
			String actionFound = BluetoothDevice.ACTION_FOUND; // API 5
			
			if ((actionFound != null) && actionFound.equals(action)){ // String: API 1, nothing thrown
				
				String extraDevice = BluetoothDevice.EXTRA_DEVICE; // API 5
				
				BluetoothDevice device = intent.getParcelableExtra (extraDevice); // Intent: API 1, nothing thrown
				
				ActivityMainLayoutDeviceSearch.this.onDeviceFound (device);
			}
		}
		
	}
	
}
