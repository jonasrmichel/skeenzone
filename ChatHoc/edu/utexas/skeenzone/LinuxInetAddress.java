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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

public class LinuxInetAddress {

	/**
	 * Returns an InetAddress representing the address of the localhost. Every
	 * attempt is made to find an address for this host that is not the loopback
	 * address. If no other address can be found, the loopback will be returned.
	 * 
	 * @return InetAddress - the address of localhost
	 * @throws UnknownHostException
	 *             - if there is a problem determing the address
	 */
	public static InetAddress getLocalHost() throws UnknownHostException {
		InetAddress localHost = InetAddress.getLocalHost();
		if (!localHost.isLoopbackAddress())
			return localHost;
		InetAddress[] addrs = getAllLocalUsingNetworkInterface();
		for (int i = 0; i < addrs.length; i++) {
			if (!addrs[i].isLoopbackAddress()
					// Hack to eliminate hamachi?
					&& !addrs[i].getHostAddress().startsWith("5.")
					&& !(addrs[i] instanceof Inet6Address))
				return addrs[i];
		}
		return localHost;
	}

	/**
	 * This method attempts to find all InetAddresses for this machine in a
	 * conventional way (via InetAddress). If only one address is found and it
	 * is the loopback, an attempt is made to determine the addresses for this
	 * machine using NetworkInterface.
	 * 
	 * @return InetAddress[] - all addresses assigned to the local machine
	 * @throws UnknownHostException
	 *             - if there is a problem determining addresses
	 */
	public static InetAddress[] getAllLocal() throws UnknownHostException {
		InetAddress[] iAddresses = InetAddress.getAllByName("127.0.0.1");
		if (iAddresses.length != 1)
			return iAddresses;
		if (!iAddresses[0].isLoopbackAddress())
			return iAddresses;
		return getAllLocalUsingNetworkInterface();

	}

	/**
	 * Utility method that delegates to the methods of NetworkInterface to
	 * determine addresses for this machine.
	 * 
	 * @return InetAddress[] - all addresses found from the NetworkInterfaces
	 * @throws UnknownHostException
	 *             - if there is a problem determining addresses
	 */
	private static InetAddress[] getAllLocalUsingNetworkInterface()
			throws UnknownHostException {
		ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
		Enumeration<NetworkInterface> e = null;
		try {
			e = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException ex) {
			throw new UnknownHostException("127.0.0.1");
		}
		while (e.hasMoreElements()) {
			NetworkInterface ni = (NetworkInterface) e.nextElement();
			for (Enumeration<InetAddress> e2 = ni.getInetAddresses(); e2
					.hasMoreElements();) {
				addresses.add(e2.nextElement());
			}
		}
		InetAddress[] iAddresses = new InetAddress[addresses.size()];
		for (int i = 0; i < iAddresses.length; i++) {
			iAddresses[i] = (InetAddress) addresses.get(i);
		}
		return iAddresses;
	}
}