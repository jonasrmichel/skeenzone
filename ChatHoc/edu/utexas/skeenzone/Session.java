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

package edu.utexas.skeenzone;/**

 *  https://www.assembla.com/code/distcomp/git/nodes
 *  (private server)
 * 
 * @author Kyle Prete and Jonas Michel
 * @date March 15, 2011
 *
 * TODO: Fill this in
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import edu.utexas.skeenzone.messages.ChatMessage;
import edu.utexas.skeenzone.messages.Message;

public class Session {
	MessageHandler msgHandler_;
	ChatClient client_;
	long myClock_;
	Map<InetAddress, ServerConnection> connections_;
	ArrayAdapter<String> messageLog_;
	Controller controller_;
	SessionIdentifier sessionID_;

	/**
	 * ctor
	 * 
	 * @param sessionName
	 */
	public Session(String user, ChatClient cc, Controller c, Context con,
			int viewID) {
		this(user, cc, c, con, viewID, SessionIdentifier.createID());
	}

	public Session(String user, ChatClient cc, Controller c, Context con,
			int viewID, SessionIdentifier id) {
		myClock_ = 1;
		msgHandler_ = new ChatMessageHandler(this);
		client_ = cc;
		connections_ = new HashMap<InetAddress, ServerConnection>();
		messageLog_ = new ArrayAdapter<String>(con, viewID);
		controller_ = c;
		sessionID_ = id;
	}

	public void appendMessage(String m) {
		messageLog_.add(m);
	}

	public void send(InetAddress inetAddress, Message msg) {
		try {
			connections_.get(inetAddress).sendMessage(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// Dead server; ignore send.
			e.printStackTrace();
		}
	}

	public Set<InetAddress> currentConnections() {
		return connections_.keySet();
	}

	public long getMyClock() {
		return myClock_;
	}

	public long getLowestClock() {
		long min = getMyClock();
		for (ServerConnection sc : connections_.values()) {
			min = Math.min(min, sc.getTheirClock());
		}

		return min;
	}

	public InetAddress getMyAddress() {
		return controller_.getAddress();
	}

	public SessionIdentifier getSessionID() {
		return sessionID_;
	}

	public synchronized void updateClock(Message m) {
		myClock_ = Math.max(getMyClock(), m.getClockVal()) + 1;
	}

	public String getMyUsername() {
		return controller_.getMyUsername();
	}

	public List<InetAddress> broadcast(Message msg) {
		List<InetAddress> receivers = new ArrayList<InetAddress>();
		for (ServerConnection sc : connections_.values()) {
			try {
				sc.sendMessage(msg);
				receivers.add(sc.getRemoteAddress());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return receivers;
	}

	public void deliverToClient(ChatMessage poll) {
		client_.deliver(this, controller_.getUser(poll.getSender()) + ":"
				+ poll.getMessage());
	}

	public void connectTo(String host, int port) {
		try {
			Socket server = new Socket(host, port);
			new Thread(new ConnectionHandlerRunner(this, this.controller_,
					server, true)).start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void joinInSession(String host, int port) {
		try {
			Socket server = new Socket(host, port);
			new Thread(new ConnectionHandlerRunner(this, this.controller_,
					server, false)).start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setUser(InetAddress sender, String username) {
		controller_.setUser(sender, username);
	}

	public void handleFromClient(String line) {
		msgHandler_.handleFromClient(line);
	}

	public void disconnect(InetAddress temp) throws IOException {
		connections_.get(temp).destroy();
	}

	public ListAdapter getMessageLog() {
		return messageLog_;
	}

	public void destroy() {
		for (ServerConnection sc : connections_.values()) {
			try {
				sc.destroy();
			} catch (IOException a) {
				// IGNORE ME
			}
		}
	}

	public SessionIdentifier getID() {
		return sessionID_;
	}
}
