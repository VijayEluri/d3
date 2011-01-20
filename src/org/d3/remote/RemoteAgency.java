/*
 * This file is part of d3.
 * 
 * d3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * d3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with d3.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2010 Guilhelm Savin
 */
package org.d3.remote;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.d3.Console;
import org.d3.RegistrationException;
import org.d3.actor.Agency;
import org.d3.actor.RemoteActor;
import org.d3.annotation.ActorPath;
import org.d3.protocol.Protocols;

@ActorPath("/agencies")
public class RemoteAgency {
	protected final ConcurrentHashMap<Integer, RemotePort> ports;
	protected final RemoteHost remoteHost;
	protected final String id;
	protected long lastPresenceDate;
	protected String digest;
	protected final URI uri;
	
	public RemoteAgency(RemoteHost remoteHost, String agencyId) {
		this(remoteHost, agencyId, null, "");
	}

	public RemoteAgency(RemoteHost remoteHost, String agencyId,
			String protocols, String digest) {
		this.ports = new ConcurrentHashMap<Integer, RemotePort>();
		this.lastPresenceDate = System.currentTimeMillis();
		this.remoteHost = remoteHost;
		this.id = agencyId;

		if (protocols != null)
			updateProtocols(protocols);

		if (digest != null)
			updateDigest(digest);
		
		InetAddress address = remoteHost.getAddress();
		String host = address.getHostAddress();
		
		if(address instanceof Inet6Address)
			host = String.format("[%s]", host);
		
		String uri = String.format("//%s/%s/%s", host, agencyId, agencyId);
		
		try {
			this.uri = new URI(uri);
		} catch(URISyntaxException e) {
			throw new RegistrationException(e);
		}
	}

	public RemoteHost getRemoteHost() {
		return remoteHost;
	}

	public String getId() {
		return id;
	}

	public void udpatePresence(long date) {
		this.lastPresenceDate = date;
	}

	public void updateDigest(String digest) {
		this.digest = digest;
	}

	public void updateProtocols(String protocols) {
		Matcher m = Protocols.PROTOCOL_EXPORT_PATTERN.matcher(protocols);

		LinkedList<String> schemes = new LinkedList<String>();
		LinkedList<Integer> ports = new LinkedList<Integer>();

		while (m.find()) {
			String scheme = m.group(Protocols.PROTOCOL_EXPORT_PATTERN_SCHEME);
			int port = Integer.parseInt(m
					.group(Protocols.PROTOCOL_EXPORT_PATTERN_PORT));

			schemes.addLast(scheme);
			ports.addLast(port);
		}

		LinkedList<Integer> portsToRemove = new LinkedList<Integer>();

		for (Integer i : this.ports.keySet())
			if (!ports.contains(i))
				portsToRemove.add(i);

		for (int i = 0; i < portsToRemove.size(); i++)
			this.ports.remove(portsToRemove.get(i));

		portsToRemove.clear();

		for (int i = 0; i < ports.size(); i++) {
			if (!this.ports.containsKey(ports.get(i))) {
				try {
					registerPort(ports.get(i), schemes.get(i));
				} catch (RemotePortException rpe) {
					Console.exception(rpe);
				}
			}
		}

		ports.clear();
		schemes.clear();
	}

	public String getDigest() {
		return digest;
	}

	public void registerPort(int port, String scheme)
			throws RemotePortException {
		if (ports.containsKey(port))
			throw new RemotePortException();

		RemotePort rp = new RemotePort(this, scheme, port);
		ports.put(port, rp);
		Console.warning("register %s %s:%d on %s",
				rp.isTransmitter() ? "transmitter" : "protocol", scheme, port,
				id);
	}

	public void unregisterPort(int port) {
		ports.remove(port);
	}

	public RemotePort getRandomRemotePort()
			throws NoRemotePortAvailableException {
		Iterator<RemotePort> it = ports.values().iterator();

		if (it.hasNext())
			return it.next();

		throw new NoRemotePortAvailableException();
	}

	public RemotePort getRandomRemotePortTransmittable()
			throws NoRemotePortAvailableException {
		Iterator<RemotePort> it = ports.values().iterator();

		while (it.hasNext()) {
			RemotePort rp = it.next();

			if (rp.isTransmitter())
				return rp;
		}

		throw new NoRemotePortAvailableException();
	}
	
	public RemoteActor asRemoteActor() {
		try {
			return Agency.getLocalAgency().getRemoteActors().get(uri);
		} catch( Exception e) {
			Console.exception(e);
			return null;
		}
	}
}
