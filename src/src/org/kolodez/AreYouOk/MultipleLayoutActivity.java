package org.kolodez.AreYouOk;

// VERIFIED

import android.app.Activity; // API 1

public abstract class MultipleLayoutActivity extends Activity {
	
	protected abstract MultipleLayoutActivityLayout[] getLayouts();
	
	
	
	private int iActiveLayout = -1;
	
	protected int getActiveLayout () { return iActiveLayout; }
	
	
	protected void start (int iLayout) {
		if (this.iActiveLayout == -1) {
			this.iActiveLayout = iLayout;
			this.getLayouts() [this.iActiveLayout].onStart (this.iActiveLayout);
		}
	}
	
	protected void changeLayout (int iNewLayout) {
		if (this.iActiveLayout != -1) {
			this.getLayouts() [this.iActiveLayout].onEnd ();
			this.iActiveLayout = iNewLayout;
			this.getLayouts() [this.iActiveLayout].onStart (this.iActiveLayout);
		}
	}
	
	
	
	
	protected void onDestroy () {
		if (this.iActiveLayout != -1) {
			this.getLayouts() [this.iActiveLayout].onEnd ();
		}
		
		super.onDestroy(); // Activity: API 1, returns void, nothing thrown
	}
	
}



