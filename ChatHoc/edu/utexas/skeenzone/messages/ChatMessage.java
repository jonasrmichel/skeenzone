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

import java.net.InetAddress;

public class ChatMessage extends Message {
	private String message_ = "";
	private boolean deliverable_ = false;

	private static final long serialVersionUID = -6046912624788260565L;

	public ChatMessage(long clockVal, InetAddress sender, String message) {
		this(clockVal, sender, message, false);
	}

	public ChatMessage(long clockVal, InetAddress sender, String message,
			boolean deliverable) {
		super(clockVal, sender);
		message_ = message;
		deliverable_ = deliverable;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ChatMessage))
			return false;
		ChatMessage other = (ChatMessage) o;
		return this.getClockVal() == other.getClockVal()
				&& this.deliverable_ == other.deliverable_
				&& this.getSender().equals(other.getSender())
				&& message_.equals(other.message_);
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = 31 * hash + ((Long) getClockVal()).hashCode();
		hash = 31 * hash + ((Boolean) deliverable_).hashCode();
		hash = 31 * hash + getSender().hashCode();
		hash = 31 * hash + message_.hashCode();
		return hash;
	}

	@Override
	public Message execute(long myClock, InetAddress myAddress) {
		return new ProposedDeliveryTimeMessage(myClock, myAddress, this);
	}

	public void setMessage(String message) {
		this.message_ = message;
	}

	public String getMessage() {
		return message_;
	}

	public void setDeliverable(boolean deliverable) {
		this.deliverable_ = deliverable;
	}

	public boolean isDeliverable() {
		return deliverable_;
	}
}
