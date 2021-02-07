package org.kolodez.AreYouOk;

// VERIFIED

public abstract class MultipleLayoutActivityLayout { // API 1
	
	private MultipleLayoutActivity activity;
	
	public MultipleLayoutActivity getActivity() { return this.activity; }
	
	
	/*
	 * activity must be the activity that contains this layout.
	 */
	public void bindToActivity (MultipleLayoutActivity activity) { this.activity = activity; }
	
	
	/*
	 * iThisLayout is the index of this layout in the array of all layouts.
	 * Implement this function as if this were the onCreate() function of an activity.
	 */
	protected abstract void onStart (int iThisLayout);
	
	/*
	 * Implement this function as if this were the onDestroy() function of an activity.
	 */
	protected abstract void onEnd ();
	
	
	public static class EmptyLayout extends MultipleLayoutActivityLayout {
		protected void onStart (int iThisLayout) { }
		protected void onEnd () { }
	}
	
}



