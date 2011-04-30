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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import edu.utexas.skeenzone.messages.ChatMessage;
import edu.utexas.skeenzone.messages.FinalDeliveryTimeMessage;
import edu.utexas.skeenzone.messages.Message;
import edu.utexas.skeenzone.messages.ProposedDeliveryTimeMessage;

public class ChatMessageHandler extends MessageHandler {
	Map<Message, Long> tempTime_ = new HashMap<Message, Long>();
	Map<Message, List<InetAddress>> acksToReceive_ = new HashMap<Message, List<InetAddress>>();
	PriorityQueue<ChatMessage> messageQueue_ = new PriorityQueue<ChatMessage>();

	public ChatMessageHandler(Session server) {
		super(server);
	}

	@Override
	public void handle(Message m) {
		if (m instanceof ChatMessage) {
			synchronized (messageQueue_) {
				messageQueue_.add((ChatMessage) m);
			}
			super.handle(m);
		} else if (m instanceof ProposedDeliveryTimeMessage) {
			ProposedDeliveryTimeMessage p = (ProposedDeliveryTimeMessage) m;
			Message reference = p.getReference();
			long clockToSend = 0;
			synchronized (messageQueue_) {
				long tempClock = Math.max(p.getClockVal(),
						tempTime_.remove(reference));
				List<InetAddress> left = acksToReceive_.remove(reference);
				boolean succeed = left.remove(m.getSender());
				assert succeed;
				if (left.size() == 0) {
					clockToSend = tempClock;
				} else {
					tempTime_.put(reference, tempClock);
					acksToReceive_.put(reference, left);
				}
			}
			if (clockToSend != 0)
				sendFinalTime(clockToSend, (ChatMessage) reference);
		} else if (m instanceof FinalDeliveryTimeMessage) {
			FinalDeliveryTimeMessage f = (FinalDeliveryTimeMessage) m;
			synchronized (messageQueue_) {
				if (messageQueue_.remove((ChatMessage) f.getReference()))
					messageQueue_.add((ChatMessage) f.getNewMessage());
				// else, message not meant for us.
			}
		} else {
			super.handle(m);
		}
		checkDeliverable();
	}

	private void checkDeliverable() {
		synchronized (messageQueue_) {
			while (!messageQueue_.isEmpty()
					&& messageQueue_.peek().isDeliverable()) {
				mySession_.deliverToClient(messageQueue_.poll());
			}
		}
	}

	/**
	 * Send a final timestamp message to all, based on the max timestamp of all
	 * receivers and the original ChatMessage.
	 * 
	 * @param tempClock
	 *            the final timestamp to be sent
	 * @param reference
	 *            the original ChatMessage
	 */
	private void sendFinalTime(long tempClock, ChatMessage reference) {
		ChatMessage newMessage = new ChatMessage(tempClock,
				reference.getSender(), reference.getMessage(), true);
		synchronized (messageQueue_) {
			messageQueue_.remove(reference);
			messageQueue_.add(newMessage);
		}
		mySession_.broadcast(new FinalDeliveryTimeMessage(
				mySession_.getMyClock(), mySession_.getMyAddress(), reference,
				newMessage));
	}

	@Override
	public void announceDead(InetAddress sender) {
		synchronized (messageQueue_) {
			// Use this guy to keep consistency of the keySet
			List<Message> toRemove = new LinkedList<Message>();
			for (Message c : acksToReceive_.keySet()) {
				// All messages in this structure were sent by us. Check if
				// we're waiting for dead sender.
				List<InetAddress> left = acksToReceive_.get(c);
				if (left.contains(sender)) {
					// We're still waiting for a response from this dead server
					left.remove(sender);
					if (left.size() > 0) {
						// We're still waiting for other responses
						acksToReceive_.put(c, left);
					} else {
						// We have all our responses.
						assert left.size() == 0;
						toRemove.add(c);
						long clock = tempTime_.remove(c);
						sendFinalTime(clock, (ChatMessage) c);
					}
				}
			}
			for (Message c : toRemove) {
				acksToReceive_.remove(c);
			}
			// Get rid of all undeliverable messages from sender
			List<ChatMessage> temp = new LinkedList<ChatMessage>();
			while (!messageQueue_.isEmpty()) {
				ChatMessage c = messageQueue_.poll();
				if (!c.getSender().equals(sender) || c.isDeliverable())
					temp.add(c);
				// if it is from dead guy and undeliverable, throw it away
			}
			messageQueue_.addAll(temp);
		}
	}

	@Override
	public void handleFromClient(String line) {
		ChatMessage m = new ChatMessage(mySession_.getMyClock(),
				mySession_.getMyAddress(), line);
		List<InetAddress> broadcast = mySession_.broadcast(m);
		synchronized (messageQueue_) {
			tempTime_.put(m, m.getClockVal());
			messageQueue_.add(m);
			acksToReceive_.put(m, broadcast);
		}
		// In case we are sending this while talking to no one.
		if (broadcast.size() == 0) {
			tempTime_.remove(m);
			acksToReceive_.remove(m);
			m.setDeliverable(true);
			checkDeliverable();
		}
	}

}
