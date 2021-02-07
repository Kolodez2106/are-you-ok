package org.kolodez.AreYouOk;

// VERIFIED

public class AppMode {
	
	public static final int MONITOR = 0;
	public static final int ALARM = 1;
	
	public static Class getActivityClass (int mode) {
		
		if (mode == MONITOR)
			return ActivityMain.class;
		
		else
			return ActivityAlarm.class;
		
	}
	
}
