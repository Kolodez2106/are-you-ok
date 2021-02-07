package org.kolodez.AreYouOk;

// VERIFIED

import java.util.IllegalFormatException;
import java.util.List;

import android.app.Activity; // API 1

import android.bluetooth.BluetoothAdapter; // API 5
import android.bluetooth.BluetoothDevice; // API 5
import android.bluetooth.BluetoothManager; // API 18
import android.bluetooth.le.BluetoothLeScanner; // API 21
import android.bluetooth.le.ScanCallback; // API 21
import android.bluetooth.le.ScanResult; // API 21

import android.content.Context; // API 1
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
 * This class is only available since API 21.
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
	
	private BluetoothLeScanner bluetoothLeScanner = null;
	private MyScanCallback myScanCallback = null;
	
	
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
		
		String errorMsg = this.startScan();
		if (errorMsg != null) {
			try {
				this.tvError.setText (errorMsg); // TextView: API 1, returns void, IllegalArgumentException is thrown
			} catch (IllegalArgumentException e) { }
		}
		
	}
	
	
	protected void onEnd () {
		this.endScanIfScanning ();
		
		try {
			this.adapter.clear (); // ArrayAdapter: API 1, returns void, throws UnsupportedOperationException
		} catch (UnsupportedOperationException e) { }
	}
	
	
	private void endScanIfScanning () {
		if (this.bluetoothLeScanner != null) {
			if (this.myScanCallback != null)
				this.bluetoothLeScanner.stopScan (this.myScanCallback); // BluetoothLeScanner: API 21, returns void, nothing thrown
			this.bluetoothLeScanner = null;
		}
		
		if (this.myScanCallback != null)
			this.myScanCallback = null;
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
	
	private void onScanFailed (int ErrorCode) {
		this.endScanIfScanning ();
		
		try {
			this.adapter.clear (); // ArrayAdapter: API 1, returns void, throws UnsupportedOperationException
		} catch (UnsupportedOperationException e) { }
		
		try {
			this.tvError.setText ("scan failed, error = " + ErrorCode); // TextView: API 1, returns void, IllegalArgumentException is thrown
		} catch (IllegalArgumentException e) { }
	}
	
	// result must not be null
	private void onDeviceFound (ScanResult result) {
		BluetoothDevice device = result.getDevice(); // ScanResult: API 21, nothing thrown
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
	
	
	
	
	
	
	// on success, null is returned; on error, the error description is returned
	private String startScan () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		PackageManager packageManager = activity.getPackageManager(); // Context: API 1, nothing thrown
		if (packageManager == null)
			return "getPackageManager() failed";
		
		String featureBluetoothLe = PackageManager.FEATURE_BLUETOOTH_LE; // API 18
		if (!packageManager.hasSystemFeature (featureBluetoothLe)) // PackageManager: API 5, nothing thrown
			return "feature FEATURE_BLUETOOTH_LE not found";
		
		String bluetoothService = Context.BLUETOOTH_SERVICE; // API 18
		BluetoothManager bluetoothManager;
		try {
			bluetoothManager = (BluetoothManager) activity.getSystemService (bluetoothService); // Context: API 1, nothing thrown
		} catch (ClassCastException e) { return "getSystemService(BLUETOOTH_SERVICE) cannot be casted to BluetoothManager"; }
		if (bluetoothManager == null)
			return "getSystemService(BLUETOOTH_SERVICE) returned null";
		
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter(); // BluetoothManager: API 18, nothing thrown
		if (bluetoothAdapter == null)
			return "bluetoothManager.getAdapter() returned null";
		
		else if (!bluetoothAdapter.isEnabled()) // BluetoothAdapter: API 5, nothing thrown
			return "bluetooth adapter not enabled";
		
		this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner(); // BluetoothAdapter: API 21, nothing thrown
		if (this.bluetoothLeScanner == null)
			return "getBluetoothLeScanner() returned null";
		
		this.myScanCallback = new MyScanCallback();
		if (this.myScanCallback == null)
			return "MyScanCallback could not be created";
		
		try {
			this.bluetoothLeScanner.startScan (this.myScanCallback); // BluetoothLeScanner: API 21, returns void, throws IllegalArgumentException
		} catch (IllegalArgumentException e) {
			this.myScanCallback = null;
			return "bluetoothLeScanner.startScan() threw an exception";
		}
		
		return null;
	}
	
	
	
	
	
	private class MyScanCallback extends ScanCallback { // API 21 as ScanCallback
		
		public void onScanFailed (int ErrorCode) {
			ActivityMainLayoutDeviceSearch.this.onScanFailed (ErrorCode);
		}
		
		public void onScanResult (int CallbackType, ScanResult result) {
			if (result != null)
				ActivityMainLayoutDeviceSearch.this.onDeviceFound (result);
		}
		
		public void onBatchScanResults (List <ScanResult> listResults) {
			if (listResults != null) {
				for (ScanResult result : listResults) {
					if (result != null)
						ActivityMainLayoutDeviceSearch.this.onDeviceFound (result);
				}
			}
		}
		
	}
	
	
}
