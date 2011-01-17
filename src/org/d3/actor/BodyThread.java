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
package org.d3.actor;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.d3.Console;

public class BodyThread extends ActorThread {

	public static enum SpecialAction {
		STEP, STOP
	}

	private static class SpecialActionTask extends ScheduledTask {

		SpecialAction action;

		SpecialActionTask(long delay, TimeUnit unit, SpecialAction action) {
			super(delay, unit);
			this.action = action;
		}
	}

	private DelayQueue<Delayed> queue;

	public BodyThread(LocalActor owner) {
		super(owner, "request");
		this.queue = new DelayQueue<Delayed>();
	}

	public void run() {
		checkIsOwner();

		owner.register();
		runBody();
	}

	protected final void runBody() {
		checkIsOwner();

		boolean running = true;
		Object current;

		if (owner instanceof StepActor) {
			StepActor sa = (StepActor) owner;
			SpecialActionTask sat = new SpecialActionTask(
					sa.getStepDelay(TimeUnit.NANOSECONDS),
					TimeUnit.NANOSECONDS, SpecialAction.STEP);
			
			queue.add(sat);
		}

		while (running) {
			try {
				current = queue.take();
			} catch (InterruptedException e) {
				continue;
			}

			if (current == null)
				continue;

			if (current instanceof Call) {
				Call c = (Call) current;

				try {
					Object r = owner.call(c.getName(), c.getArgs());
					c.getFuture().init(r);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (current instanceof SpecialActionTask) {
				SpecialActionTask sat = (SpecialActionTask) current;

				switch (sat.action) {
				case STEP:
					if (owner instanceof StepActor) {
						StepActor sa = (StepActor) owner;
						sa.step();
						sat.delay = sa.getStepDelay(sat.unit);
						sat.reset();
						queue.add(sat);
					}

					break;
				case STOP:
					running = false;
					break;
				}
			}
		}
	}

	public final Future enqueue(String name, Object[] args) {
		Call c = new Call(owner, name, args);
		queue.add(c);

		return c.getFuture();
	}
}
