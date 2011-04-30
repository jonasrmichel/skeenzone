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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import edu.utexas.skeenzone.messages.Message;

public class ServerConnection {
	private long theirClock_ = 0;
	private Socket clientSocket = null;
	private ConnectionHandlerRunner connRunner_;

	private ObjectInputStream din = null;
	private ObjectOutputStream pout = null;

	public ServerConnection(ConnectionHandlerRunner ms, Socket s) {
		clientSocket = s;
		connRunner_ = ms;
	}

	protected ObjectInputStream getDin() throws IOException {
		if (din == null) {
			din = new ObjectInputStream(clientSocket.getInputStream());
		}
		return din;
	}

	protected ObjectOutputStream getPout() throws IOException {
		if (pout == null) {
			pout = new ObjectOutputStream(clientSocket.getOutputStream());
		}
		return pout;
	}
	
	public void updateTimeout(int timeout) {
		try {
			clientSocket.setSoTimeout(timeout);
		} catch (SocketException e) {
			// don't care
		}
	}

	/**
	 * 
	 * @return the remote address for this connection
	 */
	public InetAddress getRemoteAddress() {
		return clientSocket.getInetAddress();
	}

	/**
	 * 
	 * @return the local address for this connection
	 */
	public InetAddress getLocalAddres() {
		return clientSocket.getLocalAddress();
	}

	/**
	 * This method initializes the connection if necessary, then sends a
	 * Message.
	 * 
	 * @param m
	 *            the Message to be sent
	 * @throws IOException
	 *             if the connected server is down
	 */
	public void sendMessage(Message m) throws IOException {
		ObjectOutputStream tempout = getPout();
		tempout.writeObject(m);
		tempout.flush();
	}

	/**
	 * This method initializes the connection if necessary, then receives a
	 * Message.
	 * 
	 * @return the next Message sent from the server
	 * @throws IOException
	 *             if the connected server goes down or has no message to send,
	 *             this exception is thrown
	 */
	public Message receiveMessage() throws IOException {
		try {
			return (Message) getDin().readObject();
		} catch (ClassNotFoundException e) {
			assert false;
		}
		return null;
	}

	/**
	 * It is expected that this method is called after a Message is successfully
	 * received.
	 * 
	 * @param m
	 *            the message just received on this connection.
	 */
	public void updateClock(Message m) {
		connRunner_.updateClock(m);
		theirClock_ = Math.max(theirClock_, m.getClockVal());
	}

	/**
	 * 
	 * @return the most recent clock value seen from the server on the other end
	 *         of this connection.
	 */
	public long getTheirClock() {
		return theirClock_;
	}
	
	public void destroy() throws IOException {
		if (clientSocket != null)
			clientSocket.close();
		clientSocket = null;
	}
	
	public boolean isDestroyed() {
		return clientSocket == null;
	}
}
