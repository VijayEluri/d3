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
package org.d3.protocol;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.d3.HostAddress;
import org.d3.actor.Agency;
import org.d3.protocol.request.ObjectCoder;
import org.d3.protocol.request.ObjectCoder.CodingMethod;
import org.d3.remote.RemotePort;

public class FutureRequest implements Serializable {
	private static final long serialVersionUID = -3651816320747757365L;

	private String id;
	private CodingMethod coding;
	private byte[] value;
	private URI target;

	public FutureRequest(String id, Object value, Transmitter transmitter,
			RemotePort remotePort) {
		this.id = id;
		this.coding = transmitter.getPreferredCodingMethod();
		this.value = ObjectCoder.encode(coding, (Serializable) value);

		try {
			HostAddress address = remotePort.getRemoteAgency().getRemoteHost()
					.getAddress();
			String host = address.getHost();

			this.target = new URI(String.format("%s://%s:%d", remotePort
					.getScheme(), host, remotePort.getPort()));
		} catch (URISyntaxException e) {
			Agency.getFaultManager().handle(e, null);
		}
	}

	public FutureRequest(String id, CodingMethod coding, byte[] value,
			URI target) {
		this.id = id;
		this.value = value;
		this.coding = coding;
		this.target = target;
	}

	public URI getTarget() {
		return target;
	}

	public String getFutureId() {
		return id;
	}

	public CodingMethod getCodingMethod() {
		return coding;
	}

	public byte[] getValue() {
		return value;
	}

	public Object getDecodedValue() {
		return ObjectCoder.decode(coding, value);
	}
}
