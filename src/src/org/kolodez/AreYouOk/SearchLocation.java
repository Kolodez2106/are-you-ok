package org.kolodez.AreYouOk;

// VERIFIED

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import android.content.Context; // API 1

import android.location.Location; // API 1
import android.location.LocationListener; // API 1
import android.location.LocationManager; // API 1

import android.os.Bundle; // API 1


public class SearchLocation { // API 4, but does not work as intended below API 9
	
	private LocationManager manager;
	private List <LocationListener> listenerList;
	
	private SearchLocation (LocationManager manager, List <LocationListener> listenerList) {
		this.manager = manager;
		this.listenerList = listenerList;
	}
	
	
	public void close () {
		if (this.listenerList != null) {
			for (LocationListener listener : this.listenerList) {
				try {
					this.manager.removeUpdates (listener); // LocationManager: API 1, returns void, throws IllegalArgumentException (if listener is null)
				} catch (IllegalArgumentException e) { }
			}
		}
	}
	
	
	
	
	
	public static SearchLocation search (Context context, MyListener listener) { // returns null for API < 9
		
		if ((AppState.SDK < 9) || (listener == null))
			return null;
		
		String serviceLocation = Context.LOCATION_SERVICE; // API 1
		LocationManager locationManager = (LocationManager) context.getSystemService (serviceLocation); // Context: API 1, nothing thrown
		if (locationManager == null)
			return null;
		
		List<String> listProviders = locationManager.getProviders (false); // LocationManager: API 1, nothing thrown
		
		/*if ((listProviders == null) || listProviders.isEmpty()) {
			Log.d ("AreYouOk log", "SearchLocation.search(): (listProviders == null) || listProviders.isEmpty()");
			return null;
		}*/
		if ((listProviders == null) || listProviders.isEmpty())
			return null;
		
		
		
		List <LocationListener> listListeners = new ArrayList <LocationListener> ();
		
		for (String provider : listProviders) {
			LocationListener privateListener = new MyPrivateListener (provider, listener);
			listListeners.add (privateListener);
			
			try {
				locationManager.requestSingleUpdate (provider, privateListener, null); // LocationManager: API 9, DEPRECATED in API 30, returns void, throws IllegalArgumentException, SecurityException
			} catch (IllegalArgumentException e) { }
			catch (SecurityException e) { }
		}
		
		return new SearchLocation (locationManager, listListeners);
	}
	
	
	
	
	public static interface MyListener {
		public void locationObtained (LocationData locationData);
	}
	
	
	private static class MyPrivateListener implements LocationListener {
		
		private String provider;
		private MyListener listener;
		
		// listener must not be null
		private MyPrivateListener (String provider, MyListener listener) {
			this.provider = provider;
			this.listener = listener;
		}
		
		public void onLocationChanged (Location location) {
			if (location != null) {
				LocationData locationData = new LocationData (this.provider, location);
				this.listener.locationObtained (locationData);
			}
		}
		
		public void onProviderDisabled (String provider) { }
		public void onProviderEnabled (String provider) { }
		public void onStatusChanged (String provider, int status, Bundle extras) { }
	}
	
	
	
	public static class LocationData {
		
		private String provider;
		private double latitude; // in deg
		private double longitude; // in deg
		private double horizontalConfidenceRadius; // 68%; in meters; 99999999 if none available
		private String timestamp;
		
		public String getProvider () { return this.provider; }
		public double getLatitude () { return this.latitude; }
		public double getLongitude () { return this.longitude; }
		public double getHorizontalConfidenceRadius () { return this.horizontalConfidenceRadius; }
		public String getTime () { return this.timestamp; }
		
		
		// location must not be null
		public LocationData (String provider, Location location) {
			this.provider = provider;
			
			this.latitude = location.getLatitude(); // Location: API 1, nothing thrown
			this.longitude = location.getLongitude(); // Location: API 1, nothing thrown
			
			if (location.hasAccuracy()) // Location: API 1, nothing thrown
				this.horizontalConfidenceRadius = location.getAccuracy(); // Location: API 1, nothing thrown
			else
				this.horizontalConfidenceRadius = 99999999;
				
			Calendar time = new GregorianCalendar (1970, Calendar.JANUARY, 1, 0, 0, 0);
			
			TimeZone utc = TimeZone.getTimeZone("UTC"); // nothing thrown
			time.setTimeZone (utc); // nothing thrown
			
			long locationTime = location.getTime(); // Location: API 1, nothing thrown
			time.setTimeInMillis (locationTime); // nothing thrown
			
			this.timestamp = ServiceMain.printTime (time) + " UTC";
		}
		
	}
	
}
