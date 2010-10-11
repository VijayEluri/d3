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
package org.ri2c.d3.protocol.xml.udp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;

import javax.management.modelmbean.XMLParseException;

import org.ri2c.d3.Agency;
import org.ri2c.d3.agency.IpTables;
import org.ri2c.d3.protocol.udp.UDPInterface;
import org.ri2c.d3.protocol.xml.XMLInterface;
import org.ri2c.d3.protocol.xml.XMLRequest;
import org.ri2c.d3.protocol.xml.XMLStanza;
import org.ri2c.d3.protocol.xml.XMLStanzaBuilder;
import org.ri2c.d3.protocol.xml.XMLStanzaFactory;
import org.ri2c.d3.request.RequestListener;

public class XMLUDPInterface
	extends UDPInterface implements XMLInterface
{
	public static final int XML_UDP_PORT = 6002;
	
	private class InnerXMLStanzaFactory
		implements XMLStanzaFactory
	{
		public XMLStanza newXMLStanza(String name)
		{
			return new XMLRequest(name);
		}
	}
	
	private Charset 			cs;
	private XMLStanzaFactory 	factory;
	private RequestListener		xprb;
	private IpTables			iptables;
	
	public XMLUDPInterface()
		throws SocketException
	{
		super();
		cs = Charset.forName( Agency.getArg("l2d.system.cs.default") );
		factory = new InnerXMLStanzaFactory();
	}

	public void init( RequestListener bridge, IpTables iptables )
	{
		try {
			super.init(Agency.getArg("l2d.protocol.xml.udp.interface"),XML_UDP_PORT);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		this.iptables = iptables;
		this.xprb     = bridge;
		runService();
	}
	
	public void sendXMLRequest( String remoteId, XMLRequest request )
	{
		InetAddress inet = iptables.getAddress(remoteId);
		
		if( inet != null )
		{
			try
			{
				sendUDPRequest(inet,XML_UDP_PORT, (XMLUDPRequest) request);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else System.err.printf("[xml] unknown remote agency %s%n", remoteId);
	}
	
	public XMLRequest getXMLRequest( String name )
	{
		return new XMLUDPRequest(name);
	}
	
	public void dataReceived( InetAddress from, byte[] data, int length )
	{
		String content 	= new String( data, 0, length, cs );
		String sourceId = iptables.getId(from);
		
		if( sourceId == null )
		{
			System.out.printf("[xml] unknown source: %s%n", from);
			return;
		}
		
		try
		{
			XMLRequest request = (XMLRequest) XMLStanzaBuilder.string2stanza(factory,content);
			request.setSource(sourceId);
			
			xprb.requestReceived(request);
		}
		catch (XMLParseException e)
		{
			e.printStackTrace();
		}
	}
}
