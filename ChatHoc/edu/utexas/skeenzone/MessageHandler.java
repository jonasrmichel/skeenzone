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

import java.net.InetAddress;

import edu.utexas.skeenzone.messages.HandshakeMessage;
import edu.utexas.skeenzone.messages.Message;

public abstract class MessageHandler {

	protected Session mySession_;

	public MessageHandler(Session server) {
		mySession_ = server;
	}

	/**
	 * Try to execute the message for a reply and send that reply to the sender
	 * of m. This method is intended to be overridden in subclasses.
	 * 
	 * @param m
	 *            the message received (to be handled)
	 */
	public void handle(Message m) {
		if (m instanceof HandshakeMessage) {
			HandshakeMessage h = (HandshakeMessage) m;
			mySession_.setUser(h.getSender(), h.getUsername());
			for (InetAddress i : h.getIPsToConnect()) {
				if (!i.equals(mySession_.getMyAddress()))
					mySession_.joinInSession(i.getHostAddress(),
							Controller.DEFAULT_PORT);
			}
		}
		Message reply = m.execute(mySession_.getMyClock(),
				mySession_.getMyAddress());
		if (reply != null)
			mySession_.send(m.getSender(), reply);
	}

	/**
	 * This method does stuff when a server dies. For base class, nothing is
	 * needed.
	 */
	public abstract void announceDead(InetAddress sender);

	/**
	 * Handle input from the client
	 * 
	 * @param line
	 *            the String command passed from the client.
	 */
	public abstract void handleFromClient(String line);
}
