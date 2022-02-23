package me.johnnywoof.ao.utils;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NMSUtils {
	
	private static Method	varHandleSet		= null;
	private static Object	modifiersVarHandle	= null;
	private static Field	modifiersField		= null;
	
	{
		try{
			Class.forName("java.lang.invoke.VarHandle");
			Class<?> methodHandles = getClass("java.lang.invoke.MethodHandles");
			Class<?> lookup = getInnerClass(methodHandles, "Lookup");
			Method privateLookupIn = getMethod(methodHandles, "privateLookupIn", Class.class, lookup);
			Method lookupMethod = getMethod(methodHandles, "lookup");
			Object lookupObject = privateLookupIn.invoke(null, Field.class, lookupMethod.invoke(null));
			Method findVarHandle = getMethod(lookup, "findVarHandle", Class.class, String.class, Class.class);
			modifiersVarHandle = findVarHandle.invoke(lookupObject, Field.class, "modifiers", int.class);
		}catch(Exception e){
		}
	}
	{
		try{
			Class<?> classVarHandle = Class.forName("java.lang.invoke.VarHandle");
			varHandleSet = getMethod(classVarHandle, "set", Object[].class);
		}catch(Exception e){
		}
	}
	
	{
		try{
			modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
		}catch(Exception ignored){
		}
	}
	
	private static Map<Class<?>, Method>	handleMethods	= new HashMap<>();
	private static String					version			= getVersion();
	
	public static String getVersion(){
		try {
			if (version != null)
				return version;
			String name = Bukkit.getServer().getClass().getPackage().getName();
			return name.substring(name.lastIndexOf('.') + 1) + ".";
		}catch (Exception | Error e){
			return null;
		}
	}
	
	public static Class<?> getClassWithException(String name) throws Exception{
		return Class.forName(name);
	}
	
	public static Class<?> getClass(String name){
		try{
			return getClassWithException(name);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Class<?> getClassSilent(String name){
		try{
			return getClassWithException(name);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Class<?> getPossibleClass(String... strings){
		for(String s : strings){
			try{
				return getClassWithException(s);
			}catch(Exception e){
			}
		}
		return null;
	}
	
	public static Class<?> getNMSVersionClassWithException(String className) throws Exception{
		return Class.forName("net.minecraft.server." + getVersion() + className);
	}

	public static Class<?> getNMSNormalClassWithException(String className) throws Exception{
		return Class.forName("net.minecraft.server." + className);
	}

	public static Class<?> getNMSClassWithException(String className) throws Exception{
		try{
			return getNMSVersionClassWithException(className);
		}catch(Exception e){
		}
		return getNMSNormalClassWithException(className);
	}
	
	public static Class<?> getNMSClass(String className){
		try{
			return getNMSClassWithException(className);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Class<?> getNMSClassSilent(String className){
		try{
			return getNMSVersionClassWithException(className);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Class<?> getNMSClass(String className, String embedded){
		try{
			return getNMSVersionClassWithException(className);
		}catch(Exception e){
			return getInnerClassSilent(getNMSClassSilent(embedded), className);
		}
	}
	
	public static Class<?> getNMSClassSilent(String className, String embedded){
		try{
			return getNMSClassWithException(className);
		}catch(Exception e){
			return getInnerClassSilent(getNMSClassSilent(embedded), className);
		}
	}
	
	public static Class<?> getNMSInnerClassSilent(String className, String embedded){
		try{
			return getInnerClassSilent(getNMSClassSilent(embedded), className);
		}catch(Exception e){
			return null;
		}
	}
	
	public static Class<?> getOBCClassWithException(String className) throws Exception{
		return Class.forName("org.bukkit.craftbukkit." + getVersion() + className);
	}
	
	public static Class<?> getOBCClass(String className){
		try{
			return getOBCClassWithException(className);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Class<?> getOBCClassSilent(String className){
		try{
			return getOBCClassWithException(className);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Object getHandle(Object obj){
		try{
			return getHandleWithException(obj);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static Object getHandleWithException(Object obj) throws Exception{
		return handleMethods.computeIfAbsent(obj.getClass(), (c) -> getMethod(c, "getHandle")).invoke(obj);
	}
	
	public static Object getHandleSilent(Object obj){
		try{
			return getHandleWithException(obj);
		}catch(Exception e){
			return null;
		}
	}
	
	public static Field setAccessible(Field field) throws Exception{
		return setAccessible(field, false);
	}
	
	public static Field setAccessible(Field field, boolean readOnly) throws Exception{
		return setAccessible(field, readOnly, false);
	}
	
	private static Field setAccessible(Field field, boolean readOnly, boolean privileged) throws Exception{
		try{
			field.setAccessible(true);
		}catch(Exception e){
			if(!privileged){
				return AccessController.doPrivileged((PrivilegedAction<Field>)() -> {
					try{
						return setAccessible(field, readOnly, true);
					}catch(Exception e1){
						e1.printStackTrace();
					}
					return field;
				});
			}
		}
		if(readOnly){ return field; }
		removeFinal(field, privileged);
		return field;
	}
	
	private static void removeFinal(Field field, boolean privileged) throws ReflectiveOperationException{
		int modifiers = field.getModifiers();
		if(Modifier.isFinal(modifiers)){
			try{
				removeFinalSimple(field);
			}catch(Exception e1){
				try{
					removeFinalVarHandle(field);
				}catch(Exception e2){
					try{
						removeFinalNativeDeclaredFields(field);
					}catch(Exception e3){
						if(!privileged){
							AccessController.doPrivileged((PrivilegedAction<Field>)() -> {
								try{
									setAccessible(field, false, true);
								}catch(Exception e){
								}
								return null;
							});
							return;
						}
					}
				}
			}
		}
	}
	
	private static void removeFinalSimple(Field field) throws ReflectiveOperationException{
		int modifiers = field.getModifiers();
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
	}
	
	private static void removeFinalVarHandle(Field field) throws ReflectiveOperationException{
		int modifiers = field.getModifiers();
		int newModifiers = modifiers & ~Modifier.FINAL;
		if(modifiersVarHandle != null){
			varHandleSet.invoke(modifiersVarHandle, field, newModifiers);
		}else{
			modifiersField.setInt(field, newModifiers);
		}
	}
	
	private static void removeFinalNativeDeclaredFields(Field field) throws ReflectiveOperationException{
		removeFinalNativeDeclaredFields(field, false);
	}
	
	private static void removeFinalNativeDeclaredFields(Field field, boolean secondTry) throws ReflectiveOperationException{
		int modifiers = field.getModifiers();
		// https://github.com/ViaVersion/ViaVersion/blob/e07c994ddc50e00b53b728d08ab044e66c35c30f/bungee/src/main/java/us/myles/ViaVersion/bungee/platform/BungeeViaInjector.java
		// Java 12 compatibility *this is fine*
		Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
		getDeclaredFields0.setAccessible(true);
		Field[] fields = (Field[])getDeclaredFields0.invoke(Field.class, false);
		for(Field classField : fields){
			if("modifiers".equals(classField.getName())){
				classField.setAccessible(true);
				classField.set(field, modifiers & ~Modifier.FINAL);
				break;
			}
		}
	}
	
	public static List<Field> getDirectFields(Class<?> clazz){
		List<Field> f = new ArrayList<Field>();
		for(Field field : clazz.getDeclaredFields()){
			try{
				field = setAccessible(field);
			}catch(Exception e){
				e.printStackTrace();
			}
			if(!Modifier.isStatic(field.getModifiers())){
				f.add(field);
			}
		}
		return f;
	}
	
	public static List<Field> getFields(Class<?> clazz) throws Exception{
		List<Field> f = new ArrayList<Field>();
		for(Field field : clazz.getDeclaredFields()){
			field = setAccessible(field);
			f.add(field);
		}
		return f;
	}
	
	public static List<Field> getFieldsIncludingUpper(Class<?> clazz) throws Exception{
		List<Field> f = new ArrayList<Field>();
		do{
			for(Field field : clazz.getDeclaredFields()){
				field = setAccessible(field);
				f.add(field);
			}
		}while((clazz = clazz.getSuperclass()) != null);
		return f;
	}
	
	public static Field getFieldWithException(Class<?> clazz, String name) throws Exception{
		for(Field field : clazz.getDeclaredFields())
			if(field.getName().equals(name)){
				field = setAccessible(field);
				return field;
			}
		throw new Exception("Field Not Found");
	}
	
	public static Field getField(Class<?> clazz, String name){
		try{
			return getFieldWithException(clazz, name);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Field getFieldSilent(Class<?> clazz, String name){
		try{
			return getFieldWithException(clazz, name);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Object getFieldValueWithException(Field f, Object args) throws Exception{
		return f.get(args);
	}
	
	public static Object getFieldValueSilent(Field f, Object args){
		try{
			return getFieldValueWithException(f, args);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Object getFieldValue(Field f, Object args){
		try{
			return getFieldValueWithException(f, args);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Field getPossibleField(Class<?> clazz, String... strings){
		for(String s : strings){
			try{
				return getFieldWithException(clazz, s);
			}catch(Exception e){
			}
		}
		return null;
	}
	
	public static Field getFieldOfTypeWithException(Class<?> clazz, Class<?> type, String name) throws Exception{
		for(Field field : clazz.getDeclaredFields())
			if(field.getName().equals(name) && field.getType().equals(type)){
				field = setAccessible(field);
				return field;
			}
		throw new Exception("Field Not Found");
	}
	
	public static Field getFieldOfType(Class<?> clazz, Class<?> type, String name){
		try{
			return getFieldOfTypeWithException(clazz, type, name);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Field getFirstFieldOfTypeWithException(Class<?> clazz, Class<?> type) throws Exception{
		for(Field field : clazz.getDeclaredFields()){
			if(field.getType().equals(type)){
				field = setAccessible(field);
				return field;
			}
		}
		throw new Exception("Field Not Found");
	}
	
	public static Field getFirstFieldOfTypeSilent(Class<?> clazz, Class<?> type){
		try{
			return getFirstFieldOfTypeWithException(clazz, type);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Field getFirstFieldOfType(Class<?> clazz, Class<?> type){
		try{
			return getFirstFieldOfTypeWithException(clazz, type);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<Field> getFieldsOfTypeWithException(Class<?> clazz, Class<?> type) throws Exception{
		List<Field> list = new ArrayList<Field>();
		Field[] arrayOfField;
		int j = (arrayOfField = clazz.getDeclaredFields()).length;
		for(int i = 0; i < j; i++){
			Field field = arrayOfField[i];
			if(type.isAssignableFrom(field.getType())){
				field = setAccessible(field);
				list.add(field);
			}
		}
		if(list.size() > 0){ return list; }
		throw new Exception("Fields Not Found");
	}
	
	public static List<Field> getFieldsOfType(Class<?> clazz, Class<?> type){
		try{
			return getFieldsOfTypeWithException(clazz, type);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Field getLastFieldOfTypeWithException(Class<?> clazz, Class<?> type) throws Exception{
		Field field = null;
		for(Field f : clazz.getDeclaredFields())
			if(f.getType().equals(type)){
				field = f;
			}
		if(field == null){ throw new Exception("Field Not Found"); }
		field = setAccessible(field);
		return field;
	}
	
	public static Field getLastFieldOfType(Class<?> clazz, Class<?> type){
		try{
			return getLastFieldOfTypeWithException(clazz, type);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Method getMethod(Class<?> clazz, String name, Class<?>... args){
		for(Method m : clazz.getDeclaredMethods())
			if(m.getName().equals(name) && (args.length == 0 && m.getParameterTypes().length == 0 || ClassListEqual(args, m.getParameterTypes()))){
				m.setAccessible(true);
				return m;
			}
		for(Method m : clazz.getMethods())
			if(m.getName().equals(name) && (args.length == 0 && m.getParameterTypes().length == 0 || ClassListEqual(args, m.getParameterTypes()))){
				m.setAccessible(true);
				return m;
			}
		return null;
	}
	
	public static Method getMethod(Class<?> clazz, String names[], Class<?>... args){
		for(String name : names){
			for(Method m : clazz.getDeclaredMethods())
				if(m.getName().equals(name) && (args.length == 0 && m.getParameterTypes().length == 0 || ClassListEqual(args, m.getParameterTypes()))){
					m.setAccessible(true);
					return m;
				}
			for(Method m : clazz.getMethods())
				if(m.getName().equals(name) && (args.length == 0 && m.getParameterTypes().length == 0 || ClassListEqual(args, m.getParameterTypes()))){
					m.setAccessible(true);
					return m;
				}
		}
		return null;
	}
	
	public static Method getMethodSilent(Class<?> clazz, String name, Class<?>... args){
		try{
			return getMethod(clazz, name, args);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Method getMethodWithReturnTypeAndArgs(Class<?> clazz, Class<?> ret, Class<?>... args){
		for(Method m : clazz.getDeclaredMethods())
			if(m.getReturnType().equals(ret) && (args.length == 0 && m.getParameterTypes().length == 0 || ClassListEqual(args, m.getParameterTypes()))){
				m.setAccessible(true);
				return m;
			}
		for(Method m : clazz.getMethods())
			if(m.getReturnType().equals(ret) && (args.length == 0 && m.getParameterTypes().length == 0 || ClassListEqual(args, m.getParameterTypes()))){
				m.setAccessible(true);
				return m;
			}
		return null;
	}
	
	public static Method getMethodReturnWithException(Class<?> clazz, Class<?> arg) throws Exception{
		for(Method m : clazz.getDeclaredMethods())
			if(arg == m.getReturnType()){
				m.setAccessible(true);
				return m;
			}
		for(Method m : clazz.getMethods())
			if(arg == m.getReturnType()){
				m.setAccessible(true);
				return m;
			}
		throw new Exception("Method Not Found");
	}
	
	public static Method getMethodReturn(Class<?> clazz, Class<?> arg){
		try{
			return getMethodReturnWithException(clazz, arg);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Method getMethodReturnSilent(Class<?> clazz, Class<?> arg){
		try{
			return getMethodReturnWithException(clazz, arg);
		}catch(Exception e){
		}
		return null;
	}
	
	public static List<Method> getAllMethods(Class<?> clazz){
		List<Method> list = new ArrayList<>();
		try{
			for(Method m : clazz.getDeclaredMethods()){
				m.setAccessible(true);
				list.add(m);
			}
			for(Method m : clazz.getMethods()){
				m.setAccessible(true);
				list.add(m);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return list;
	}
	
	public static boolean ClassListEqual(Class<?>[] l1, Class<?>[] l2){
		if(l1.length != l2.length)
			return false;
		for(int i = 0; i < l1.length; i++)
			if(l1[i] != l2[i])
				return false;
		return true;
	}
	
	public static Class<?> getInnerClassWithException(Class<?> c, String className) throws Exception{
		for(Class<?> cl : c.getDeclaredClasses())
			if(cl.getSimpleName().equals(className))
				return cl;
		return null;
	}
	
	public static Class<?> getInnerClass(Class<?> c, String className){
		try{
			return getInnerClassWithException(c, className);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Class<?> getInnerClassSilent(Class<?> c, String className){
		try{
			return getInnerClassWithException(c, className);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Constructor<?> getConstructorWithException(Class<?> clazz, Class<?>... args) throws Exception{
		for(Constructor<?> c : clazz.getDeclaredConstructors())
			if(args.length == 0 && c.getParameterTypes().length == 0 || ClassListEqual(args, c.getParameterTypes())){
				c.setAccessible(true);
				return c;
			}
		for(Constructor<?> c : clazz.getConstructors())
			if(args.length == 0 && c.getParameterTypes().length == 0 || ClassListEqual(args, c.getParameterTypes())){
				c.setAccessible(true);
				return c;
			}
		throw new Exception("Constructor Not Found");
	}
	
	public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... args){
		try{
			return getConstructorWithException(clazz, args);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Constructor<?> getConstructorSilent(Class<?> clazz, Class<?>... args){
		try{
			return getConstructorWithException(clazz, args);
		}catch(Exception e){
		}
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Enum<?> getEnum(final String value, final Class enumClass){
		return Enum.valueOf(enumClass, value);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <E extends Enum<E>> E getEnum(int i, final Class enumClass){
		return (E)enumClass.getEnumConstants()[i];
	}
	
	public static Object newInstance(Constructor<?> con, Object... objects){
		try{
			return con.newInstance(objects);
		}catch(Exception e){
		}
		return null;
	}
	
}
