/**
 * SkeenZone
 * http://code.google.com/p/skeenzone
 * 
 * Copyright 2011 Kyle Prete, Jonas Michel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utexas.skeenzone.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import edu.utexas.skeenzone.Controller;
import edu.utexas.skeenzone.SessionIdentifier;

public class ChatHoc extends Activity {
	/** dialog type fields */
	static final int DIALOG_WELCOME_ID = 0;
	static final int DIALOG_CREATE_ID = 1;
	static final int DIALOG_WHOCHAT_ID = 2;
	static final int DIALOG_WHOLOCAL_ID = 3;
	static final int DIALOG_INVITE_ID = 4;
	static final int DIALOG_MYCHATS_ID = 5;
	static final int DIALOG_LEAVE_ID = 6;
	static final int DIALOG_INCOMING_ID = 7;

	private String username_;
	private String incomingUsername_;
	private SessionIdentifier incomingSessionID;

	/** android UI fields */
	private TextView mTitle;
	private ListView mConversationView;
	private EditText mChatEditText;
	private Button mSendButton;

	private Controller controller_;

	/** called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set up the window layout
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// Set up the custom title,
		// app name on the left
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		// something custom on the right
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// before we proceed, initialize some user and chat info
		showDialog(DIALOG_WELCOME_ID);

		// Initialize the compose field with a listener for the return key
		mChatEditText = (EditText) findViewById(R.id.edit_text_message);
		mChatEditText.setOnEditorActionListener(mWriteListener);

		// Initialize the send button with a listener that for click events
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				// Send a message using content of the edit text widget
				TextView view = (TextView) findViewById(R.id.edit_text_message);
				String message = view.getText().toString().trim();
				view.setText("");

				controller_.handleMessageFromClient(message);
			}
		});

		mConversationView = (ListView) findViewById(R.id.blackboard_listview);
	}

	private Handler pirate = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			int type = b.getInt(Controller.UI_TYPE_KEY);

			switch (type) {
			case Controller.TYPE_MESSAGE:

				String message = b.getString(Controller.UI_MSG_KEY);
				SessionIdentifier sessionid = (SessionIdentifier) b
						.getSerializable(Controller.UI_SESSION_KEY);

				controller_.getSessionById(sessionid).appendMessage(message);
				break;

			case Controller.TYPE_INCOMING:
				incomingUsername_ = b.getString(Controller.UI_USERNAME_KEY);
				incomingSessionID = (SessionIdentifier) b
						.getSerializable(Controller.UI_SESSION_ID);
				showDialog(DIALOG_INCOMING_ID);
				break;
			}

		}
	};

	/** action listener for the EditText widget to listen for the return key */
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// If the action is a key-up event on the return key, send the
			// message
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString().trim();
				view.setText("");

				controller_.handleMessageFromClient(message);
			}
			return true;
		}
	};

	/** called when device's menu button is pressed in the app */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	/** menu item selection actions */
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {

		case R.id.menuitem_whochat:
			// display list of locally available users
			showDialog(DIALOG_WHOCHAT_ID);
			return true;

		case R.id.menuitem_wholocal:
			// display list of locally available users
			showDialog(DIALOG_WHOLOCAL_ID);
			return true;

		case R.id.menuitem_invite:
			// display invite user dialog
			showDialog(DIALOG_INVITE_ID);
			return true;

		case R.id.menuitem_mychats:
			// display switch menu list of active chats
			showDialog(DIALOG_MYCHATS_ID);
			return true;

		case R.id.menuitem_leave:
			// display are you sure prompt
			showDialog(DIALOG_LEAVE_ID);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/** dialog box ctors */
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater factory = LayoutInflater.from(this);

		switch (id) {
		case DIALOG_WELCOME_ID:
			// do the work to display the welcome dialog
			final View welcomeTextEntryView = factory.inflate(
					R.layout.welcome_dialog, null);
			builder.setIcon(R.drawable.ic_menu_tag).setTitle(R.string.welcome)
					.setView(welcomeTextEntryView).setPositiveButton(
							R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									username_ = getFieldVal(
											welcomeTextEntryView,
											R.id.username_edit);
									controller_ = new Controller(username_,
											ChatHoc.this, R.layout.message,
											pirate);
									new Thread(controller_).start();
									while (!controller_.isInitialized()) {
									}
									controller_.createNewSession();
									updateSessionView();
								}
							}).setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// "exit" app
									ChatHoc.this.finish();
								}
							});
			dialog = builder.create();

			break;
		case DIALOG_WHOCHAT_ID:
			// do the work to define who's chatting dialog
			ArrayAdapter<String> chattersarr = new ArrayAdapter<String>(this,
					R.layout.message);
			for (String s : controller_.currentSessionChatters()) {
				chattersarr.add(s);
			}
			final View whochatTextEntryView = factory.inflate(
					R.layout.who_dialog, null);
			builder.setIcon(R.drawable.ic_menu_happy)
					.setTitle(R.string.whochat).setView(whochatTextEntryView)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialog.cancel();
									// delete dialog to force recreation next
									// call
									removeDialog(DIALOG_WHOCHAT_ID);
								}
							});
			dialog = builder.create();
			ListView lv = (ListView) whochatTextEntryView
					.findViewById(R.id.chatusers_listview);
			lv.setAdapter(chattersarr);
			break;
		case DIALOG_CREATE_ID:
			// do the work to define create dialog
			builder
					.setIcon(R.drawable.ic_menu_wizard)
					.setTitle(R.string.create)
					.setMessage(
							"You are creating a new empty chat session. Continue?")
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									controller_.createNewSession();
									updateSessionView();
									// delete dialog to force recreation next
									// call
									removeDialog(DIALOG_CREATE_ID);
								}
							}).setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialog.cancel();
									// delete dialog to force recreation next
									// call
									removeDialog(DIALOG_CREATE_ID);
								}
							});
			dialog = builder.create();
			break;
		case DIALOG_WHOLOCAL_ID:
			// do the work to define who local dialog
			ArrayAdapter<String> otherguy = new ArrayAdapter<String>(this,
					R.layout.message);
			for (String s : controller_.allChatters()) {
				otherguy.add(s);
			}

			final View wholocalTextEntryView = factory.inflate(
					R.layout.who_dialog, null);
			builder.setIcon(R.drawable.ic_menu_globe).setTitle(
					R.string.wholocal).setView(wholocalTextEntryView)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialog.cancel();
									// delete dialog to force recreation next
									// call
									removeDialog(DIALOG_WHOLOCAL_ID);
								}
							});
			dialog = builder.create();
			ListView lv2 = (ListView) wholocalTextEntryView
					.findViewById(R.id.chatusers_listview);
			lv2.setAdapter(otherguy);
			break;
		case DIALOG_INVITE_ID:
			// do the work to define invite dialog
			final View inviteTextEntryView = factory.inflate(
					R.layout.invite_dialog, null);
			builder.setIcon(R.drawable.ic_menu_resize)
					.setTitle(R.string.invite).setView(inviteTextEntryView)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									controller_.connectTo(getFieldVal(
											inviteTextEntryView,
											R.id.ipaddress_edit),
											Controller.DEFAULT_PORT);
								}
							}).setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		case DIALOG_MYCHATS_ID:
			// do the work to define mychats dialog
			final List<SessionIdentifier> sessions = controller_.getSessions();
			List<String> fu = new ArrayList<String>();
			for (SessionIdentifier asd : sessions) {
				fu.add(asd.toString());
			}

			final CharSequence[] items = (String[]) fu.toArray(new String[0]);

			int temp;
			try {
				temp = sessions.indexOf(controller_.getCurrentSessionID());
			} catch (NullPointerException e) {
				temp = -1;
			}
			final int currsession = temp;

			builder.setTitle(R.string.mychats).setIcon(
					R.drawable.ic_menu_dialog).setSingleChoiceItems(items,
					currsession, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							controller_.switchSession(sessions.get(item));
							updateSessionView();
							dialog.cancel();
							// delete dialog to force recreation next
							// call
							removeDialog(DIALOG_MYCHATS_ID);
						}
					}).setPositiveButton(R.string.create,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							showDialog(DIALOG_CREATE_ID);
							// delete dialog to force recreation next
							// call
							removeDialog(DIALOG_MYCHATS_ID);
						}
					});

			dialog = builder.create();
			break;
		case DIALOG_LEAVE_ID:
			// do the work to define leave dialog
			builder.setMessage("Are you sure you want to leave this chat?")
					.setIcon(R.drawable.ic_menu_exit).setCancelable(false)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									controller_.leaveCurrentChat();
									showDialog(DIALOG_MYCHATS_ID);
								}
							}).setNegativeButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		case DIALOG_INCOMING_ID:
			String incomingTitle = this.getString(R.string.incoming) + " "
					+ "(" + incomingUsername_ + ")" + " " + incomingSessionID;
			// do the work to define create dialog
			final View incomingTextEntryView = factory.inflate(
					R.layout.create_dialog, null);
			builder.setIcon(R.drawable.ic_menu_magnet).setTitle(incomingTitle)
					.setView(incomingTextEntryView).setPositiveButton(
							R.string.accept,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									controller_
											.switchSession(incomingSessionID);
									updateSessionView();
									// delete dialog to force recreation next
									// call
									removeDialog(DIALOG_INCOMING_ID);
								}
							}).setNegativeButton(R.string.reject,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									controller_
											.destroySessionWithID(incomingSessionID);
									dialog.cancel();
									// delete dialog to force recreation next
									// call
									removeDialog(DIALOG_INCOMING_ID);
								}
							});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	private String getFieldVal(View d, int id) {
		EditText timmy = (EditText) d.findViewById(id);
		return timmy.getText().toString().trim();
	}

	private void updateSessionView() {
		mTitle.setText("(" + controller_.getFormattedAddress() + ") "
				+ controller_.getCurrentSessionID());
		mConversationView.setAdapter(controller_.getCurrentMessageLog());
		mConversationView.smoothScrollToPosition(controller_
				.getCurrentMessageLog().getCount());
	}

}
