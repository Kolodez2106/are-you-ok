package org.kolodez.AreYouOk;

// VERIFIED

import java.util.IllegalFormatException;

import android.app.Activity; // API 1

import android.content.ActivityNotFoundException; // API 1
import android.content.Context; // API 1
import android.content.ContentResolver; // API 1
import android.content.Intent; // API 1

import android.database.Cursor; // API 1

import android.net.Uri; // API 1

import android.provider.ContactsContract; // API 5

import android.view.InflateException; // API 1
import android.view.LayoutInflater; // API 1
import android.view.View; // API 1
import android.view.ViewGroup; // API 1

import android.widget.ArrayAdapter; // API 1
import android.widget.Button; // API 1
import android.widget.ListView; // API 1
import android.widget.TextView; // API 1



/*
 * This class is only available since API 5.
 * bindToActivity() must have been called before this layout is opened. (Assumption 1)
 */
public class ActivityMainLayoutContactList extends MultipleLayoutActivityLayout {
	
	private ListView listView;
	private MyArrayAdapter adapter;
	
	private final static int RESULT_PICK_CONTACT = 1;
	
	private Button[] button = new Button[2];
	
	private static int[] buttonIdArray = {
		R.id.ButtonAddContact,
		R.id.ButtonCloseContactList
	};
	
	
	protected void onStart (int i) {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
        activity.setContentView (R.layout.contact_list); // Activity: API 1, returns void, nothing thrown
        
        for (int iButton = 0; iButton < this.button.length; iButton++) {
			button [iButton] = (Button) activity.findViewById (this.buttonIdArray [iButton]); // Activity: API 1, may return null, nothing thrown
			if (this.button [iButton] == null) {
				activity.changeLayout (ActivityMain.LAYOUT_MAIN);
				return;
			}
			
			ButtonListener listener = this.new ButtonListener (buttonIdArray [iButton]);
			this.button [iButton].setOnClickListener (listener); // View: API 1, returns void, nothing thrown
		}
		
		this.listView = (ListView) activity.findViewById (R.id.ListViewContacts); // Activity: API 1, may return null, nothing thrown
		if (this.listView == null) {
			activity.changeLayout (ActivityMain.LAYOUT_MAIN);
			return;
		}
		
		
		this.adapter = new MyArrayAdapter (activity);
		this.listView.setAdapter (this.adapter); // ListView: API 1, returns void, nothing thrown
	}
	
	
	protected void onEnd () {
		AppState.writeContactsToSharedPreferences ();
	}
	
	
	private void closeContactList () {
		ActivityMain activity = (ActivityMain) this.getActivity (); // this does not return null due to Assumption 1
		
		activity.changeLayout (ActivityMain.LAYOUT_MAIN);
	}
	
	
	private class MyArrayAdapter extends ArrayAdapter <ContactListEntry> {
		
		public MyArrayAdapter (Context context) {
			super (context, android.R.layout.simple_list_item_1, AppState.contactList); // ArrayAdapter: API 1, nothing thrown
		}
		
		public View getView (int position, View convertView, ViewGroup parent) {
			
			ActivityMain activity = (ActivityMain) ActivityMainLayoutContactList.this.getActivity (); // this does not return null due to Assumption 1
			
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
			
			ContactListEntry entry = this.getItem (position); // ArrayAdapter: API 1, nothing thrown
			String newText;
			
			if (entry == null)
				newText = "null";
			else {
				String name = entry.name;
				String number = entry.number;
				
				if (name == null)
					name = "null";
				if (number == null)
					number = "null";
				
				try {
					newText = String.format("%s (%s)", name, number); // throws IllegalFormatException, NullPointerException
				} catch (IllegalFormatException e) { newText = "null"; }
				catch (NullPointerException e) { newText = "null"; }
			}
			
			try {
				text.setText (newText); // TextView: API 1, returns void, IllegalArgumentException is thrown
			} catch (IllegalArgumentException e) { }
			
			Button deleteButton = (Button) result.findViewById (R.id.ListItemButton); // View: API 1, may return null, nothing thrown
			if (deleteButton != null) {
				
				ListButtonListener listener = this.new ListButtonListener (entry);
				deleteButton.setOnClickListener (listener); // View: API 1, returns void, nothing thrown
				
				try {
					deleteButton.setText ("delete"); // TextView: API 1, returns void, IllegalArgumentException is thrown
				} catch (IllegalArgumentException e) { }
			}
			
			
			return result;
		}
		
		private class ListButtonListener implements View.OnClickListener {
			private ContactListEntry entry;
			public ListButtonListener (ContactListEntry entry) { this.entry = entry; }
			public void onClick (View v) {
				try {
					MyArrayAdapter.this.remove (this.entry); // ArrayAdapter: API 1, returns void, throws UnsupportedOperationException
				} catch (UnsupportedOperationException e) { }
				
				MyArrayAdapter.this.notifyDataSetChanged(); // ArrayAdapter: API 1, returns void, nothing thrown
			}
		}
		
	}
	
	
	
	
	private class ButtonListener implements View.OnClickListener {
		private int id;
		
		public ButtonListener (int id) { this.id = id; }
		
		public void onClick (View v) {
			if (this.id == R.id.ButtonAddContact)
				ActivityMainLayoutContactList.this.addContact();
			
			else if (this.id == R.id.ButtonCloseContactList)
				ActivityMainLayoutContactList.this.closeContactList();
		}
	}
	
	private void addContact () {
		
		String actionPick = Intent.ACTION_PICK; // API 1
		Uri contentUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI; // API 5
		Intent intent = new Intent (actionPick, contentUri); // API 1, nothing thrown
		
		String contentType = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE; // API 5
		intent.setType (contentType); // Intent: API 1, returns same Intent, nothing thrown
		
		Activity activity = this.getActivity (); // this does not return null due to Assumption 1
		try {
			activity.startActivityForResult (intent, RESULT_PICK_CONTACT); // Activity: API 1, returns void, throws ActivityNotFoundException
		} catch (ActivityNotFoundException e) { }
	}
	
	
	
	
	public void onActivityResult (int requestCode, int resultCode, Intent data) {
		int resultCanceled = Activity.RESULT_CANCELED; // API 1
		if ((requestCode == RESULT_PICK_CONTACT) && (resultCode != resultCanceled)) {
			
			Activity activity = this.getActivity (); // this does not return null due to Assumption 1
			
			ContentResolver contentResolver = activity.getContentResolver (); // ContextWrapper: API 1, nothing thrown
			if (contentResolver == null)
				return;
			
			Uri uri = data.getData(); // Intent: API 1, nothing thrown
			if (uri == null)
				return;
			
			final String[] CONTACT_PROJECTION = {
				ContactsContract.CommonDataKinds.Phone.NUMBER, // API 5
				ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME // ContactsContract.ContactsColumns: API 5
			};
			
			Cursor cursor = contentResolver.query (uri, CONTACT_PROJECTION, null, null, null); // ContentResolver: API 1, nothing thrown
			if (
				(cursor == null) ||
				! cursor.moveToFirst() // API 1, nothing thrown
			)
				return;
			
			
			int iNumber, iName;
			try {
				iNumber = cursor.getColumnIndexOrThrow (CONTACT_PROJECTION [0]); // Cursor: API 1, throws IllegalArgumentException
				
				iName = cursor.getColumnIndexOrThrow (CONTACT_PROJECTION [1]); // Cursor: API 1, throws IllegalArgumentException
			} catch (IllegalArgumentException e) { return; }
			
			String number, name;
			try {
				number = cursor.getString (iNumber); // Cursor: API 1, may throw an implementation-defined exception
				name = cursor.getString (iName); // Cursor: API 1, may throw an implementation-defined exception
			} catch (Exception e) { return; }
			
			if ((number == null) || (name == null))
				return;
			
			ContactListEntry newItem = new ContactListEntry (name, number);
			
			if (this.adapter != null) {
				try {
					this.adapter.add (newItem); // ArrayAdapter: API 1, returns void, throws UnsupportedOperationException
				} catch (UnsupportedOperationException e) { }
			}
		}
	}
	
	
}
