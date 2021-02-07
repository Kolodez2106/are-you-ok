package org.kolodez.AreYouOk;

// VERIFIED

import android.media.AudioManager; // API 1
import android.media.ToneGenerator; // API 1

public class PeriodicBeep implements Runnable { // API 1
	
	public final static int MAX_VOLUME = ToneGenerator.MAX_VOLUME; // API 1
	
	/**
	 * Creates and starts a thread that plays a periodic tone.
	 * 
	 * Use:
	 * periodicBeep = PeriodicBeep.create (...); // start tone
	 * ... // do something
	 * periodicBeep.end() // end tone
	 */
	public static PeriodicBeep create (int millisecondsHalfPeriod, int millisecondsUntilTargetVolume, int volumeTarget) {
		
		PeriodicBeep result = new PeriodicBeep (millisecondsHalfPeriod, millisecondsUntilTargetVolume, volumeTarget);
		
		Thread thread;
		try {
			thread = new Thread (result); // throws SecurityException
		} catch (SecurityException e) { return null; }
		
		try {
			thread.start(); // throws IllegalThreadStateException
		} catch (IllegalThreadStateException e) { return null; }
		
		return result;
	}
	
	
	
	
	private boolean bContinue;
	
	private int millisecondsHalfPeriod;
	
	private VolumeCalculator volumeCalculator; // not null
	
	private PeriodicBeep (int millisecondsHalfPeriod, int millisecondsUntilTargetVolume, int volumeTarget) {
		this.bContinue = true;
		this.millisecondsHalfPeriod = millisecondsHalfPeriod;
		this.volumeCalculator = new VolumeCalculator (millisecondsHalfPeriod, millisecondsUntilTargetVolume, volumeTarget);
	}
	
	
	
	
	
	
	public boolean isOngoing () {
		return this.bContinue;
	}
	
	public void end () {
		this.bContinue = false;
	}
	
	public void run() {
		
		for (int iBeep = 0; this.bContinue; iBeep += 1) {
			
			if (iBeep < 0)
				iBeep = Integer.MAX_VALUE;
			
			int streamType = AudioManager.STREAM_ALARM; // API 1
			ToneGenerator toneGenerator = new ToneGenerator (streamType, this.volumeCalculator.calculate (iBeep)); // API 1, nothing thrown
			
			int toneType = ToneGenerator.TONE_DTMF_A; // API 1
			
			toneGenerator.startTone (toneType); // ToneGenerator: API 1, returns boolean with unclear meaning, nothing thrown
			
			try {
				Thread.sleep (this.millisecondsHalfPeriod); // returns void, throws InterruptedException, IllegalArgumentException
			} catch (InterruptedException e) { }
			catch (IllegalArgumentException e) { }
			
			toneGenerator.stopTone (); // ToneGenerator: API 1, returns void, nothing thrown
			toneGenerator.release(); // ToneGenerator: API 1, returns void, nothing thrown
			
			try {
				Thread.sleep (this.millisecondsHalfPeriod); // returns void, throws InterruptedException, IllegalArgumentException
			} catch (InterruptedException e) { }
			catch (IllegalArgumentException e) { }
		}
	}
	
	
	
	
	
	private static class VolumeCalculator {
		private int volumeTarget;
		private int iBeepFullVolume;
		
		// millisecondsHalfPeriod > 0
		private VolumeCalculator (int millisecondsHalfPeriod, int millisecondsUntilTargetVolume, int volumeTarget) {
			this.volumeTarget = volumeTarget;
			
			if (millisecondsUntilTargetVolume > 0)
				this.iBeepFullVolume = (int) ((double) millisecondsUntilTargetVolume / (2 * (double) millisecondsHalfPeriod));
			else
				this.iBeepFullVolume = 0;
		}
		
		private int calculate (int iBeep) {
			if (iBeep >= this.iBeepFullVolume)
				return this.volumeTarget;
			else
				return (int) (this.volumeTarget * Math.sin ((double) (iBeep+1) / (double) (this.iBeepFullVolume + 1) * Math.PI / 2));
		}
	}
	
}