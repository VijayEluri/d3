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
package org.d3.protocol.xml;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import org.d3.annotation.ActorPath;
import org.d3.protocol.InetProtocol;
import org.d3.protocol.Request;

@ActorPath("/protocols/xml/tcp")
@InetProtocol
public class XMLTCPProtocol extends XMLProtocol {
	private ServerSocketChannel channel;

	public XMLTCPProtocol(InetSocketAddress socketAddress) throws IOException {
		super(Integer.toString(socketAddress.getPort()), socketAddress);
		
		channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind( socketAddress );
	}

	public void writeRequest(Request request) {
		byte[] data = convert(request);
		URI target = request.getTargetURI();
		
		try {
			SocketChannel out = SocketChannel.open();
			out.connect(new InetSocketAddress(target.getHost(), target.getPort()));
			out.write(ByteBuffer.wrap(data));
			out.close();
		} catch(IOException e) {
			// TODO
			e.printStackTrace();
		}
	}
	
	public final SelectableChannel getChannel() {
		return channel;
	}
}
