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

public class FinalDeliveryTimeMessage extends Message {

	private static final long serialVersionUID = -5500329275728840654L;
	private Message reference_;
	private Message newMessage_;

	public FinalDeliveryTimeMessage(long clockVal, InetAddress sender,
			Message oldM, Message newM) {
		super(clockVal, sender);
		reference_ = oldM;
		newMessage_ = newM;
	}

	@Override
	public Message execute(long myClock, InetAddress myAddress) {
		return null;
	}

	public Message getReference() {
		return reference_;
	}

	public Message getNewMessage() {
		return newMessage_;
	}

}
