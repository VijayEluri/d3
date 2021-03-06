/*
 * This file is part of d3 <http://d3-project.org>.
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
 * Copyright 2010 - 2011 Guilhelm Savin
 */
package org.d3.tools;

import java.util.concurrent.atomic.AtomicReference;

public class AtomicState<K extends Enum<K>> {
	AtomicReference<K> reference;

	public AtomicState(Class<? extends K> cls, K init) {
		reference = new AtomicReference<K>(init);
	}

	public void set(K k) {
		reference.set(k);
		
		synchronized (reference) {
			reference.notifyAll();
		}
	}

	public K get() {
		return reference.get();
	}

	public K waitForState(K s) throws InterruptedException {
		K k = reference.get();

		while (k.ordinal() < s.ordinal()) {
			synchronized (reference) {
					reference.wait(1000);
			}
			
			k = reference.get();
		}

		return k;
	}
}
