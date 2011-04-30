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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class Discoverer implements Runnable {
	Thread discoverer_;
	private String name_ = "TestService";
	private String type_ = "_skeeny._tcp.local.";
	private int port_ = 5199;
	private JmDNS jmdns_ = null;
	private ServiceListener listener_ = null;
	private ServiceInfo serviceInfo_;

	private Set<String> discovered_ = new HashSet<String>();

	public Discoverer(String name, int port) {
		name_ = name;
		port_ = port;
		System.out.println("Starting " + name_ + " port:" + port_);
		discoverer_ = new Thread(this, name_);
		discoverer_.start();
	}
	
	public Set<String> getDiscovered() {
		return Collections.unmodifiableSet(discovered_);
	}

	@Override
	public void run() {
		try {
			Runtime.getRuntime().addShutdownHook(new ShutdownHook());

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			jmdns_ = JmDNS.create(LinuxInetAddress.getLocalHost(), name_);

			// listen for services
			jmdns_.addServiceListener(type_, listener_ = new ServiceListener() {

				@Override
				public void serviceResolved(ServiceEvent e) {
					System.out.println("Service resolved: "
							+ e.getInfo().getQualifiedName() + " "
							+ e.getInfo().getHostAddresses()[0] + ":"
							+ e.getInfo().getPort());

					discovered_.add(e.getInfo().getHostAddresses()[0] + " "
							+ e.getInfo().getName());
				}

				@Override
				public void serviceRemoved(ServiceEvent e) {
					System.out.println("Service removed: " + e.getName());
					// Let pings handle it
				}

				@Override
				public void serviceAdded(ServiceEvent e) {
					// Required to force serviceResolved to be called
					// again (after the first search)
					jmdns_.requestServiceInfo(e.getType(), e.getName(), 1);
				}
			});
			serviceInfo_ = ServiceInfo.create(type_, name_, port_,
					"test service");
			jmdns_.registerService(serviceInfo_);
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}

	}

	public void close() {
		System.out.println("Stopping " + name_);
		if (jmdns_ != null) {
			if (listener_ != null) {
				jmdns_.removeServiceListener(type_, listener_);
				listener_ = null;
			}
			jmdns_.unregisterAllServices();
			try {
				jmdns_.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			jmdns_ = null;
		}
	}

	class ShutdownHook extends Thread {
		public void run() {
			close();
		}
	}

}
