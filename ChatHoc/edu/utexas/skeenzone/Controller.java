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

package edu.utexas.skeenzone;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ListAdapter;

public class Controller implements ChatClient, Runnable {
	public static final String UI_SESSION_KEY = "session";
	public static final String UI_MSG_KEY = "msg";
	public static final String UI_USERNAME_KEY = "username";
	public static final String UI_TYPE_KEY = "type";
	public static final String UI_SESSION_ID = "sessionID";
	public static final int TYPE_MESSAGE = 0;
	public static final int TYPE_INCOMING = 1;
	Map<SessionIdentifier, Session> sessions_;
	Session currentSession_;
	Context myContext_;
	int myViewID_;
	private InetSocketAddress myAddress_;
	private Map<InetAddress, String> usernameMap_;
	private String username_;
	static final String DISCONN_CMD = "/d ";
	static final Object QUIT_CMD = "/quit";
	static final String CONN_CMD = "/c ";
	static final int TIMEOUT_SHORT = 10000;
	static final int TIMEOUT_LONG = 100000;
	public static final int DEFAULT_PORT = 5199;
	Handler uiThread_;
	private Discoverer columbus_;

	/** emulator debug settings */
	private static final boolean EMULATOR = false;
	private static final String FAKE_IPADDRESS = "128.62.154.188";

	@Override
	public void deliver(Session destination, String message) {
		Message m = Message.obtain(uiThread_);
		Bundle b = new Bundle();
		b.putInt(UI_TYPE_KEY, TYPE_MESSAGE);
		b.putString(UI_MSG_KEY, message);
		b.putSerializable(UI_SESSION_KEY, destination.getID());
		m.setData(b);
		uiThread_.sendMessage(m);

	}

	public Controller(String user, Context c, int viewID, Handler pirate) {
		myContext_ = c;
		myViewID_ = viewID;
		sessions_ = new HashMap<SessionIdentifier, Session>();
		usernameMap_ = new HashMap<InetAddress, String>();
		username_ = user;
		uiThread_ = pirate;
	}

	public Set<String> currentSessionChatters() {
		Set<String> vals = new HashSet<String>();
		for (InetAddress ia : currentSession_.currentConnections()) {
			vals.add(formatAddress(ia) + " " + usernameMap_.get(ia));
		}
		return vals;
	}

	public Set<String> allChatters() {
		Set<String> vals = new HashSet<String>();
		for (Session s : sessions_.values()) {
			for (InetAddress ia : s.currentConnections()) {
				vals.add(formatAddress(ia) + " " + usernameMap_.get(ia));
			}
		}
		vals.addAll(columbus_.getDiscovered());
		return vals;
	}

	public void switchSession(SessionIdentifier sessionID) {
		currentSession_ = sessions_.get(sessionID);
	}

	public Session createNewSession() {
		currentSession_ = new Session(username_, this, this, myContext_,
				myViewID_);
		sessions_.put(currentSession_.getSessionID(), currentSession_);
		return currentSession_;
	}

	public void connectTo(String host, int port) {
		currentSession_.connectTo(host, port);
	}

	public List<SessionIdentifier> getSessions() {
		return new ArrayList<SessionIdentifier>(sessions_.keySet());
	}

	public Session getSessionById(SessionIdentifier name) {
		return sessions_.get(name);
	}

	/**
	 * Creates a single ServerSocket listener for client requests (public),
	 * establishes a connection to all other active servers (private), synchs
	 * its local copy of seat table, and begins listening for client requests /
	 * server messages.
	 */
	@Override
	public void run() {
		ServerSocket serverListener = null;
		try {
			serverListener = new ServerSocket(Controller.DEFAULT_PORT);
			serverListener.setSoTimeout(0);
			myAddress_ = new InetSocketAddress(LinuxInetAddress.getLocalHost(),
					Controller.DEFAULT_PORT);
			usernameMap_.put(getAddress(), username_);
			// TODO: eliminate mapping when losing server connection?
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		columbus_ = new Discoverer(username_, DEFAULT_PORT);

		// listen for server-server messages
		while (true) {
			try {
				Socket anotherServer = serverListener.accept();
				new Thread(new ConnectionHandlerRunner(this, anotherServer))
						.start();
			} catch (IOException ex) {
				System.err.println(ex);
			}
		}
	}

	public void announceConnection(SessionIdentifier sessionID, String username) {
		Message m = Message.obtain(uiThread_);
		Bundle b = new Bundle();
		b.putInt(UI_TYPE_KEY, TYPE_INCOMING);
		b.putString(UI_USERNAME_KEY, username);
		b.putSerializable(UI_SESSION_ID, sessionID);
		m.setData(b);
		uiThread_.sendMessage(m);
	}

	public String formatAddress(InetAddress adx) {
		String formatted = adx.getHostAddress().toString();
		formatted = formatted.replace("/", "");
		return formatted;
	}

	public String getFormattedAddress() {
		return formatAddress(getAddress());
	}

	public InetAddress getAddress() {
		if (EMULATOR)
			try {
				return InetAddress.getByName(FAKE_IPADDRESS);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return myAddress_.getAddress();
	}

	public boolean isInitialized() {
		return myAddress_ != null;
	}

	public String getMyUsername() {
		return username_;
	}

	public void setUser(InetAddress sender, String username) {
		usernameMap_.put(sender, username);
	}

	public String getUser(InetAddress s) {
		return usernameMap_.get(s);
	}

	public void handleMessageFromClient(String line) {
		if (line.startsWith(Controller.CONN_CMD)) {
			String[] arr = line.split("\\s+");
			if (arr.length == 2 || arr.length == 3) {
				assert Controller.CONN_CMD.equals(arr[0] + " ");
				String host = arr[1];
				int port = Controller.DEFAULT_PORT;
				try {
					port = Integer.parseInt(arr[2]);
				} catch (Exception ex) {
					// Using default
				}
				connectTo(host, port);
				return;
			}
			this.deliver(currentSession_,
					"Connect error - provide only two arguments to /c ip port");
			return;
		} else if (line.equals(Controller.QUIT_CMD)) {
			System.exit(0);
		} else if (line.startsWith(Controller.DISCONN_CMD)) {
			String[] arr = line.split("\\s+");
			if (arr.length == 2) {
				assert Controller.DISCONN_CMD.equals(arr[0] + " ");
				String host = arr[1];
				try {
					InetAddress temp = InetAddress.getByName(host);
					currentSession_.disconnect(temp);
				} catch (UnknownHostException e) {
					this.deliver(currentSession_, "Malformed host: " + host);
				} catch (IOException e) {
					// Try to ignore destruction error.
				}
				return;
			}
		}
		currentSession_.handleFromClient(line);
	}

	public ListAdapter getCurrentMessageLog() {
		return currentSession_.getMessageLog();
	}

	public void leaveCurrentChat() {
		sessions_.remove(currentSession_.getID());
		currentSession_.destroy();
		currentSession_ = null;
	}

	public SessionIdentifier getCurrentSessionID() {
		return currentSession_.getID();
	}

	public Session getSessionByID(SessionIdentifier sessionID, String username) {
		Session ret = sessions_.get(sessionID);
		if (ret == null) {
			ret = new Session(username_, this, this, myContext_, myViewID_,
					sessionID);
			sessions_.put(sessionID, ret);
			announceConnection(sessionID, username);
		}
		return ret;
	}

	public void destroySessionWithID(SessionIdentifier incomingSessionID) {
		Session temp = sessions_.remove(incomingSessionID);
		temp.destroy();
	}

}
