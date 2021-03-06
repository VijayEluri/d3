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
package org.d3.actor.body;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import org.d3.Console;
import org.d3.actor.Agency;
import org.d3.actor.LocalActor;
import org.d3.annotation.Callable;
import org.d3.annotation.Direct;
import org.d3.annotation.Local;
import org.d3.template.Template;

public class BodyMap {
	private static final ConcurrentHashMap<Class<? extends LocalActor>, BodyMap> maps = new ConcurrentHashMap<Class<? extends LocalActor>, BodyMap>();

	public static synchronized BodyMap getBodyMap(
			Class<? extends LocalActor> clazz) {
		if (!maps.containsKey(clazz))
			maps.put(clazz, new BodyMap(clazz));

		return maps.get(clazz);
	}

	public static interface CallRoutine {
		Object call(Object target, Object[] args);
	}

	public static class ReflectRoutine implements CallRoutine {
		protected Method method;

		public ReflectRoutine(Method m) {
			this.method = m;
		}

		public Object call(Object target, Object[] args) {
			try {
				return method.invoke(target, args);
			} catch (Exception e) {
				return e;
			}
		}
	}

	private static RoutineLoader loader = new RoutineLoader();

	protected final HashMap<String, CallRoutine> callables;

	protected final HashMap<String, Boolean> direct;
	protected final HashMap<String, Boolean> local;

	public BodyMap(Class<? extends LocalActor> clazz) {
		callables = new HashMap<String, CallRoutine>();
		direct = new HashMap<String, Boolean>();
		local = new HashMap<String, Boolean>();

		Class<?> cls = clazz;
		long m1, m2;

		m1 = System.currentTimeMillis();
		while (LocalActor.class.isAssignableFrom(cls) && cls != Object.class) {
			Method[] methods = cls.getMethods();

			if (methods != null) {
				for (Method m : methods) {
					if (m.getAnnotation(Callable.class) != null) {
						String name = m.getAnnotation(Callable.class).value();

						if (callables.containsKey(name)) {
							Console.warning("duplicate callable \"%s\"", name);
						} else {
							create(name, m);
						}

						direct.put(name, m.getAnnotation(Direct.class) != null);
						local.put(name, m.getAnnotation(Local.class) != null);
					}
				}
			}

			cls = cls.getSuperclass();
		}
		m2 = System.currentTimeMillis();

		Console.info("actor \"%s\" map build in %d ms", clazz.getSimpleName(),
				m2 - m1);
	}

	protected void create(String name, Method m) {
		CallRoutine cr = loader.getOrCreate(name, m);
		callables.put(name, cr);
	}

	public boolean has(String name) {
		return callables.containsKey(name);
	}

	public boolean isDirect(String name) {
		return direct.get(name);
	}

	public boolean isLocal(String name) {
		return local.get(name);
	}

	public Object invoke(LocalActor obj, String name, Object... args)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		return callables.get(name).call(obj, args);
	}

	private static final String ROUTINE_TEMPLATE = "public Object call(Object target, Object [] args) {"
			+ " return (({%class%}) target).{%name%}({%args%}); }";

	private static final String ROUTINE_VOID_TEMPLATE = "public Object call(Object target, Object [] args) {"
			+ " (({%class%}) target).{%name%}({%args%}); return Boolean.TRUE; }";

	private static class RoutineLoader extends ClassLoader {
		protected ClassPool classPool;
		protected Template template, templateVoid;
		protected HashMap<String, Class<?>> classes;

		RoutineLoader() {
			classPool = ClassPool.getDefault();
			template = new Template(ROUTINE_TEMPLATE);
			templateVoid = new Template(ROUTINE_VOID_TEMPLATE);
			classes = new HashMap<String, Class<?>>();
		}

		protected Class<?> findClass(String name) throws ClassNotFoundException {
			if (classes.containsKey(name))
				return classes.get(name);
			throw new ClassNotFoundException();
		}

		synchronized CallRoutine getOrCreate(String name, Method m) {
			String routineName = "Routine_" + m.getDeclaringClass().getName()
					+ "_" + name;
			routineName = routineName.replaceAll("[^\\w\\d_]", "_");

			try {
				Class<?> cls = findClass(routineName);
				return (CallRoutine) cls.newInstance();
			} catch (Exception e) {
			}

			try {
				HashMap<String, String> env = new HashMap<String, String>();
				env.put("class", m.getDeclaringClass().getName());
				env.put("name", m.getName());

				Class<?>[] params = m.getParameterTypes();
				StringBuilder args = new StringBuilder();

				if (params != null) {
					for (int i = 0; i < params.length; i++) {
						String sep = i > 0 ? ", " : "";
						String type = params[i].getName();

						args.append(String.format("%s(%s) args[%d]", sep, type,
								i));
					}
				}

				env.put("args", args.toString());

				String code;

				if (m.getReturnType().equals(Void.TYPE))
					code = templateVoid.toString(env);
				else
					code = template.toString(env);

				CtClass cc = classPool.makeClass(routineName);
				cc.addInterface(classPool.get(CallRoutine.class.getName()));
				CtMethod cm = CtMethod.make(code, cc);
				cc.addMethod(cm);

				byte[] data = cc.toBytecode();
				Class<?> cls = defineClass(routineName, data, 0, data.length);
				classes.put(routineName, cls);

				Console.info("create routine \"%s\"", routineName);

				try {
					return (CallRoutine) cls.newInstance();
				} catch (VerifyError e) {
					throw new Exception(e);
				}
			} catch (Exception e) {
				Agency.getFaultManager().handle(e, null);
				Console
						.warning("failed to compil routine, create a reflect routine");
				return new ReflectRoutine(m);
			}
		}
	}
}
