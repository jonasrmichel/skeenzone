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

package edu.utexas.skeenzone.messages;

import java.io.Serializable;
import java.net.InetAddress;

public abstract class Message implements Serializable, Comparable<Message> {

	public Message(long clockVal, InetAddress sender) {
		super();
		this.clockVal = clockVal;
		this.sender = sender;
	}

	private static final long serialVersionUID = 1L;
	private long clockVal;
	private InetAddress sender;

	public void setClockVal(long clockVal) {
		this.clockVal = clockVal;
	}

	public long getClockVal() {
		return clockVal;
	}

	public void setSender(InetAddress sender) {
		this.sender = sender;
	}

	public InetAddress getSender() {
		return sender;
	}

	@Override
	public int compareTo(Message other) {
		if (this.clockVal < other.clockVal)
			return -1;
		if (this.clockVal > other.clockVal)
			return 1;
		return this.sender.toString().compareTo(other.sender.toString());
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Message))
			return false;
		return this.getClass().equals(other.getClass())
				&& this.compareTo((Message) other) == 0;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = 31 * hash + ((Long) clockVal).hashCode();
		hash = 31 * hash + ((InetAddress) sender).hashCode();
		hash = 31 * hash + getSender().hashCode();
		hash = 31 * hash + super.hashCode();

		return hash;
	}

	/**
	 * Execute this message and construct a response
	 * 
	 * @param myClock
	 *            the clock value of the receiver
	 * @param myAddress
	 *            the address of the receiver
	 * @return the message to be sent to the sender
	 */
	public abstract Message execute(long myClock, InetAddress myAddress);
}
