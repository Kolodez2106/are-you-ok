package org.kolodez.AreYouOk;

// VERIFIED

import android.view.View; // API 1

import android.widget.Button; // API 1
import android.widget.TextView; // API 1

/*
 * bindToActivity() must have been called before this layout is opened. (Assumption 1)
 */
public class ActivityMainLayoutLocationTest extends MultipleLayoutActivityLayout { // API 4
	
	private Button[] button = new Button[2];
	
	private static int[] buttonIdArray = {
		R.id.ButtonLocationTestBack,
		R.id.ButtonLocationTestTest
	};
	
	private TextView tvResult;
	
	private int round = 0;
	private SearchLocation search = null; // API 4
	
	
	
	protected void onStart (int i) {
        ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
        
        activity.setContentView (R.layout.location_test); // Activity: API 1, returns void, nothing thrown
        
        this.tvResult = (TextView) activity.findViewById (R.id.TextLocationTestResult); // Activity: API 1, may return null, nothing thrown
        if (this.tvResult == null) {
			activity.changeLayout (ActivityMain.LAYOUT_MAIN);
			return;
		}
        
        if (AppState.SDK >= 11) // API 4
			this.tvResult.setTextIsSelectable (true); // TextView: API 11, returns void, nothing thrown
        
        
        for (int iButton = 0; iButton < this.button.length; iButton++) {
			this.button [iButton] = (Button) activity.findViewById (buttonIdArray [iButton]); // Activity: API 1, may return null, nothing thrown
			if (this.button [iButton] == null) {
				activity.changeLayout (ActivityMain.LAYOUT_MAIN);
				return;
			}
			
			ButtonListener listener = this.new ButtonListener (buttonIdArray [iButton]);
			this.button [iButton].setOnClickListener (listener); // View: API 1, returns void, nothing thrown
		}
		
	}
	
	
	protected void onEnd () {
		this.endSearch ();
	}
	
	private void endSearch() {
		this.round += 1;
		
		if (this.search != null) {
			this.search.close();
			this.search = null;
		}
	}
	
	
	
	private void back () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		activity.changeLayout (ActivityMain.LAYOUT_MAIN);
	}
	
	
	private void test () {
		
		this.endSearch();
		
		try {
			this.tvResult.setText (""); // TextView: API 1, returns void, IllegalArgumentException is thrown
		} catch (IllegalArgumentException e) { }
		
		
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		this.search = SearchLocation.search (activity, this.new LocationListener (this.round));
		if (this.search == null) {
			String errorMsg;
			if (AppState.SDK < 9)
				errorMsg = "This app does not support location determination before SDK 9 (Android 2.3)";
			else
				errorMsg = "Error";
			
			try {
				this.tvResult.setText (errorMsg); // TextView: API 1, returns void, IllegalArgumentException is thrown
			} catch (IllegalArgumentException e) { }
		}
	}
	
	
	
	
	
	
	
	public class LocationListener implements SearchLocation.MyListener {
		
		private int round;
		
		private LocationListener (int round) { this.round = round; }
		
		public void locationObtained (SearchLocation.LocationData locationData) {
			
			if (this.round == ActivityMainLayoutLocationTest.this.round) {
				
				String newText = String.format ("- \"%s\" (%s): %f, %f (68%% confidence radius: %.0f m)\r\n", locationData.getProvider(), locationData.getTime(), locationData.getLatitude(), locationData.getLongitude(), locationData.getHorizontalConfidenceRadius());
				
				CharSequence oldTextCharSeq = ActivityMainLayoutLocationTest.this.tvResult.getText (); // TextView: API 1, nothing thrown
				if (oldTextCharSeq == null)
					return;
				
				String oldText = oldTextCharSeq.toString(); // does not return null, nothing thrown
				
				try {
					ActivityMainLayoutLocationTest.this.tvResult.setText (oldText + newText); // TextView: API 1, returns void, IllegalArgumentException is thrown
				} catch (IllegalArgumentException e) { }
			}
		}
	}
	
	
	
	
	private class ButtonListener implements View.OnClickListener {
		private int id;
		
		public ButtonListener (int id) { this.id = id; }
		
		public void onClick (View v) {
			if (this.id == R.id.ButtonLocationTestBack)
				ActivityMainLayoutLocationTest.this.back();
			
			else if (this.id == R.id.ButtonLocationTestTest)
				ActivityMainLayoutLocationTest.this.test();
		}
	}
	
	
	
}
