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
package org.ri2c.d3.atlas;

public interface AtlasConstants
{
	public static final String ATLAS_REQUEST_GET_ENTITY_LIST =
		"entity:getlist";
	
	public static final String ATLAS_REQUEST_ENTITY_LIST =
		"entity:list";
	
	public static final String ATLAS_REQUEST_FUTURE =
		"future";
	
	public static final String ATLAS_REQUEST_ENTITY_CALL =
		"entity:call";
	
	public static final String ATLAS_REQUEST_ENTITY_MIGRATION =
		"entity:migration";
	
	public static enum MigrationPhase
	{
		request,
		response,
		canceled,
		receive
	}
	
	public static enum MigrationResponse
	{
		accepted,
		error,
		rejected
	}
}
