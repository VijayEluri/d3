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

import java.io.PrintStream;

import org.d3.actor.ActorThread;

public class Console {
	public static final String RESET = "\033[0m";
	public static final String ROUGE = "\033[31m";
	public static final String VERT = "\033[32m";
	public static final String MARRON = "\033[33m";

	public static final String SAVE_POSITION = "\033[s";
	public static final String RESTORE_POSITION = "\033[u";

	public static final String CLEAR_LINE = "\033[K";

	public static final int LOG_LEVEL_ERROR = 0;
	public static final int LOG_LEVEL_WARNING = 1;
	public static final int LOG_LEVEL_INFO_1 = 2;
	public static final int LOG_LEVEL_INFO_2 = 3;
	public static final int LOG_LEVEL_INFO_3 = 4;

	public static enum LogLevel {
		ERROR, WARNING, INFO
	}

	protected static PrintStream out = System.out;
	protected static int logLevel = 2;
	protected static boolean enableColor = true;
	protected static boolean printExceptionStack = true;

	public static void init(Args args) {
		if (args.has("level")) {
			if (args.get("level").matches("^\\d+$"))
				logLevel = args.getInteger("level");
			else {
				LogLevel lv = LogLevel.valueOf(args.get("level").toUpperCase());
				logLevel = lv.ordinal();
			}
		}

		if (args.has("coloring"))
			enableColor = args.getBoolean("coloring");

		if (args.has("exception.print_stack"))
			printExceptionStack = args.getBoolean("exception.print_stack");
	}

	public static void setLogLevel(int level) {
		logLevel = level;
	}

	public static void setColoring(boolean on) {
		enableColor = on;
	}

	public static synchronized void log(int level, String message,
			Object... args) {
		if (level <= logLevel)
			out.printf(message, args);
	}

	public static void error(String message, Object... args) {
		log(LOG_LEVEL_ERROR, "%s%s%s%s%n", enableColor ? ROUGE : "",
				getHeader(), String.format(message, args), enableColor ? RESET
						: "");
	}

	public static void warning(String message, Object... args) {
		log(LOG_LEVEL_WARNING, "%s%s%s%s%n", enableColor ? MARRON : "",
				getHeader(), String.format(message, args), enableColor ? RESET
						: "");
	}

	public static void info(String message, Object... args) {
		log(LOG_LEVEL_INFO_1, "%s%s%s%s%n", enableColor ? VERT : "",
				getHeader(), String.format(message, args), enableColor ? RESET
						: "");
	}

	public static void exception(Throwable e) {
		if (e instanceof RuntimeException)
			warning("%s: %s", e.getClass().getName(), e.getMessage());
		else
			error("%s: %s", e.getClass().getName(), e.getMessage());

		if (printExceptionStack)
			e.printStackTrace();
	}

	protected static String getHeader() {
		Actor actor = ActorThread.getCurrentActor();

		if (actor != null)
			return String.format("[%s] ", actor.getFullPath());
		else
			return "";
	}
}
