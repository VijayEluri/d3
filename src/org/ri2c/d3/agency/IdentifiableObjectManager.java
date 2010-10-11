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
package org.ri2c.d3.agency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.ri2c.d3.Console;
import org.ri2c.d3.IdentifiableObject;
import org.ri2c.d3.IdentifiableObject.IdentifiableType;

public class IdentifiableObjectManager
{
	private static class Pool
		extends ConcurrentHashMap<String,IdentifiableObject>
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -7397149209003613470L;
		
	}
	
	public static enum RegistrationStatus
	{
		accepted,
		refused,
		alreadyRegistered,
		error
	}
	
	private ConcurrentHashMap<IdentifiableType,Pool> 	pools;
	private ReentrantLock								registrationLock;
	
	public IdentifiableObjectManager()
	{
		pools = new ConcurrentHashMap<IdentifiableType,Pool>();
		
		for( IdentifiableType t: IdentifiableType.values() )
			pools.put(t,new Pool());
		
		registrationLock = new ReentrantLock();
	}
	
	public void alias( String id, IdentifiableType type, String alias )
	{
		registrationLock.lock();
		
		Pool pool = pools.get(type);
		alias(pool.get(id),alias);
		
		registrationLock.unlock();
	}
	
	public void alias( IdentifiableObject idObject, String alias )
	{
		if( idObject == null )
			return;
		
		registrationLock.lock();
		
		Pool pool = pools.get(idObject.getType());
		
		if( ! pool.containsKey(alias) && pool.containsKey(idObject.getId()) )
		{
			pool.put(alias,idObject);
			
			Console.info("%s/%s --> %s/%s",
					alias, idObject.getType(), idObject.getId(), idObject.getType() );
		}
		
		registrationLock.unlock();
	}
	
	public RegistrationStatus register( IdentifiableObject obj )
	{
		if( obj == null || obj.getId() == null || obj.getType() == null )
			return RegistrationStatus.error;
		
		registrationLock.lock();
		
		Pool p = pools.get(obj.getType());
		
		if( p == null )
		{
			registrationLock.unlock();
			return RegistrationStatus.error;
		}
		
		if( p.containsKey(obj.getId()) )
		{
			if( p.get(obj.getId()) == obj )
			{
				registrationLock.unlock();
				return RegistrationStatus.alreadyRegistered;
			}
			else
			{
				registrationLock.unlock();
				return RegistrationStatus.refused;
			}
		}
		
		p.put(obj.getId(),obj);
		
		Console.info("register %s", obj.getId() );
		
		registrationLock.unlock();
		return RegistrationStatus.accepted;
	}
	
	public void unregister( IdentifiableObject obj )
	{
		if( obj == null )
			return;
		
		registrationLock.lock();
			pools.get(obj.getType()).remove(obj.getId());
		registrationLock.unlock();
		
		Console.info("unregister %s", obj.getId() );
	}
	
	public IdentifiableObject get( IdentifiableType type, String id )
	{
		return pools.get(type).get(id);
	}
}
