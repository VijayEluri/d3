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
package org.d3;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;

import org.d3.actor.LocalActor;
import org.d3.annotation.ActorPath;
import org.d3.protocol.ProtocolThread;

@ActorPath("/protocols")
public abstract class Protocol extends LocalActor {
	public static final int REQUEST_MAX_SIZE = 1024000;

	protected final SocketAddress socketAddress;
	private final ProtocolThread protocolThread;
	protected final String scheme;

	protected Protocol(String scheme, String id, SocketAddress socketAddress) {
		super(id);
		this.socketAddress = socketAddress;
		this.protocolThread = new ProtocolThread(this);
		this.scheme = scheme;
	}

	public void init() {
		super.init();
		protocolThread.start();
	}

	public final IdentifiableType getType() {
		return IdentifiableType.protocol;
	}

	public final void checkProtocolThreadAccess() {
		protocolThread.checkIsOwner();
	}
	
	protected void dispatch(Request r)
			throws ActorNotFoundException {
		Actor target = Agency.getLocalAgency()
				.getIdentifiableObject(r.getTargetURI());

		target.handle(r);
	}

	public final int getPort() {
		if (socketAddress instanceof InetSocketAddress)
			return ((InetSocketAddress) socketAddress).getPort();

		return -1;
	}

	public final String getScheme() {
		return scheme;
	}
	
	public abstract SelectableChannel getChannel();

	public abstract void readRequest(ByteBuffer buffer)
			throws InvalidRequestFormatException;

	/**
	 * Send a request to an identifiable object.
	 * 
	 * @param target
	 * @param r
	 */
	public abstract void sendRequest(Request r);
}
