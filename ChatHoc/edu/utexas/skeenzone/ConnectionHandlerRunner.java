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
import java.net.Socket;
import java.net.SocketException;
import java.util.HashSet;

import edu.utexas.skeenzone.messages.HandshakeMessage;
import edu.utexas.skeenzone.messages.HeartBeatMessage;
import edu.utexas.skeenzone.messages.Message;

/**
 * Runnable class to handle incoming server-server messages.
 * 
 */
public class ConnectionHandlerRunner implements Runnable {
	/**
	 * 
	 */
	private Session session_;
	private Controller controller_;
	protected ServerConnection serverConn_ = null;
	private boolean needToDie_ = false;
	private boolean firstRun_ = true;
	boolean hasFailed_ = false;
	private final InetAddress remoteAddress_;
	boolean checkForSession_ = false;

	public void setSession(Session s) {
		this.session_ = s;
		if (this.session_.connections_.containsKey(remoteAddress_))
			needToDie_ = true;
		else
			this.session_.connections_.put(remoteAddress_, serverConn_);
	}

	// NOTE: We have no Session yet, NOT sharing IPS
	public ConnectionHandlerRunner(Controller ctr, Socket serverSocket) {
		this.controller_ = ctr;
		try {
			serverSocket.setSoTimeout(Controller.TIMEOUT_LONG);
		} catch (SocketException e) {
			// this shoud not happen
			assert (false);
		}

		checkForSession_ = true;

		serverConn_ = new ServerConnection(this, serverSocket);
		remoteAddress_ = serverConn_.getRemoteAddress();

		try {
			serverConn_.sendMessage(new HandshakeMessage(1, controller_
					.getAddress(), controller_.getMyUsername(),
					new HashSet<InetAddress>(), null));
		} catch (IOException e1) {
			System.err.println("message error occured");
			return;
		}
	}

	public ConnectionHandlerRunner(Session s, Controller ctr,
			Socket serverSocket, boolean shareIPS) {
		this.controller_ = ctr;
		try {
			serverSocket.setSoTimeout(Controller.TIMEOUT_LONG);
		} catch (SocketException e) {
			// this shoud not happen
			assert (false);
		}

		serverConn_ = new ServerConnection(this, serverSocket);
		remoteAddress_ = serverConn_.getRemoteAddress();
		setSession(s);

		try {
			if (shareIPS)
				serverConn_.sendMessage(new HandshakeMessage(this.session_
						.getMyClock(), this.session_.getMyAddress(),
						this.session_.getMyUsername(),
						this.session_.connections_.keySet(),
						this.session_.sessionID_));
			else
				serverConn_.sendMessage(new HandshakeMessage(this.session_
						.getMyClock(), this.session_.getMyAddress(),
						this.session_.getMyUsername(),
						new HashSet<InetAddress>(), this.session_.sessionID_));
		} catch (IOException e1) {
			System.err.println("message error occured");
			return;
		}
	}

	@Override
	public void run() {
		if (checkForSession_) {
			// Get the HandshakeMessage
			HandshakeMessage m;
			try {
				m = (HandshakeMessage) serverConn_.receiveMessage();
			} catch (IOException e) {
				return;
			}

			// Ask controller for corresponding Session
			setSession(controller_.getSessionByID(m.getSessionID(),
					m.getUsername()));
			
			session_.msgHandler_.handle(m);

		}

		this.session_.client_.deliver(this.session_, "Connection to "
				+ remoteAddress_ + " established.");
		// message handler loop
		while (true) {
			try {
				if (needToDie_)
					return;
				// Get a message
				Message m = serverConn_.receiveMessage();

				// Update the clock
				serverConn_.updateClock(m);

				// Handle the message
				this.session_.msgHandler_.handle(m);

				// reset failure switch
				hasFailed_ = false;
				if (firstRun_) {
					serverConn_.updateTimeout(Controller.TIMEOUT_SHORT);
					firstRun_ = false;
				}
			} catch (Exception e) {
				if (serverConn_.isDestroyed())
					return;
				// Server died?
				if (hasFailed_) {
					// server is dead, let this thread die
					this.session_.connections_.remove(remoteAddress_);
					this.session_.client_.deliver(this.session_,
							"Connection to " + remoteAddress_ + " lost.");
					this.session_.msgHandler_.announceDead(remoteAddress_);
					return;
				}
				// server timeout, ping them, are they alive?
				hasFailed_ = true;
				try {
					serverConn_.sendMessage(new HeartBeatMessage(this.session_
							.getMyClock(), this.session_.getMyAddress()));
				} catch (IOException e1) {
					// continue;
				}
			}
		}
	}

	public void updateClock(Message m) {
		if (session_ != null)
			session_.updateClock(m);
	}

}