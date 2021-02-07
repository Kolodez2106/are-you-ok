package org.kolodez.AreYouOk;

// VERIFIED

import android.view.View; // API 1

import android.widget.Button; // API 1
import android.widget.TextView; // API 1

/*
 * bindToActivity() must have been called before this layout is opened. (Assumption 1)
 */
public class ActivityMainLayoutHelp extends MultipleLayoutActivityLayout { // API 4
	
	private final static String textHelp = "Take notice of the important information in the end of this file!\r\n\r\nThis app is designed to automatically tell selected contacts if you may need help. To turn this functionality on and off, you must press \"start monitoring\" or \"stop monitoring\". When monitoring is turned on, the app sometimes triggers a check, which is combined with an audible alarm, in order to ask you whether you are ok. If you do not answer within the specified time period (changeable by the \"seconds for checking\" buttons), the app triggers the alarm.\r\n\r\nThe alarm sends an SMS that you may need help and why to all previously selected contacts (we call these contacts \"contact list\"). If the app can obtain your location, it also sends your location to the contact list. Moreover, all contacts from your contact list are called periodically, one every three minutes (assuming that no dialing blocks the phone app for three minutes or more). The calls have the intention to make sure that your contacts get notified about the SMS as soon as possible.\r\n\r\nWhen monitoring is turned on, it may be in two modes (pulse device connected or not connected).\r\n\r\nMode \"pulse device not connected\": The time when the next check is scheduled can be seen in the app user interface. You can move (\"snooze\") the time of the check to an arbitrary time in the future. A snooze is also considered a positive answer to the check (i.e. you seem to be ok), thus, the current check is finished and the audible alarm stops.\r\n\r\nMode \"pulse device connected\": If you connect a device via bluetooth that supports GATT and the HEART_RATE_MEASUREMENT characteristic, your current pulse is shown in the app user interface. If your pulse is outside of the expected range (changeable by the \"min pulse\" and \"max pulse\" buttons) or if the device is disconnected (except if you disconnect it yourself by pressing \"disconnect\"), a check is triggered. The check can be answered by the \"snooze\" button. Then, the check finishes. Whenever you press the \"snooze\" button, the pulse monitoring is \"snoozed\", i.e. before the specified time, a pulse outside of the expected range will not trigger a check.\r\n\r\nIMPORTANT: Before using the app, test the alarm function, e.g. by starting the monitoring, setting the \"time for checking\" to 5 s and snoozing for 0 min.\r\n\r\nIMPORTANT: This app has not been designed for phones with multiple sim cards.\r\n\r\nThis app works since API level 16 (Android 4.1 Jelly Bean), but connecting to a pulse device is supported only since API level 21 (Android 5.0 Lollipop).";
	
	
	private Button btnBack;
	
	private TextView tvHelp;
	
	
	
	protected void onStart (int i) {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
        activity.setContentView (R.layout.help); // Activity: API 1, returns void, nothing thrown
        
        this.tvHelp = (TextView) activity.findViewById (R.id.TextHelp); // Activity: API 1, may return null, nothing thrown
        this.btnBack = (Button) activity.findViewById (R.id.ButtonHelpBack); // Activity: API 1, may return null, nothing thrown
        
        if (
			(this.btnBack == null) ||
			(this.tvHelp == null)
		) {
			activity.changeLayout (ActivityMain.LAYOUT_MAIN);
			return;
		}
		
		
		ButtonBackListener listener = this.new ButtonBackListener ();
		this.btnBack.setOnClickListener (listener); // View: API 1, returns void, nothing thrown
		
        try {
			this.tvHelp.setText (textHelp); // TextView: API 1, returns void, IllegalArgumentException is thrown
		} catch (IllegalArgumentException e) { }
		
		if (AppState.SDK >= 11) // API 4
			this.tvHelp.setTextIsSelectable (true); // TextView: API 11, returns void, nothing thrown
        
	}
	
	protected void onEnd () { }
	
	
	
	
	private class ButtonBackListener implements View.OnClickListener {
		// needed since API 1:
		public void onClick (View v) {
			ActivityMain activity = (ActivityMain) ActivityMainLayoutHelp.this.getActivity (); // this does not return null due to Assumption 1
			
			activity.changeLayout (ActivityMain.LAYOUT_MAIN);
		}
	}
	
}
