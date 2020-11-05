package me.johnnywoof.ao.utils;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NMSUtils{
	
	private static Map<Class<?>, Method>	handleMethods	= new HashMap<>();
	private static String					version			= getVersion();
	
	public static String getVersion(){
		if(version != null)
			return version;
		String name = Bukkit.getServer().getClass().getPackage().getName();
		return name.substring(name.lastIndexOf('.') + 1) + ".";
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
	
	public static Class<?> getNMSClassWithException(String className) throws Exception{
		return Class.forName("net.minecraft.server." + getVersion() + className);
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
			return getNMSClassWithException(className);
		}catch(Exception e){
		}
		return null;
	}
	
	public static Class<?> getNMSClass(String className, String embedded){
		try{
			return getNMSClassWithException(className);
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
	
	private static Class<?>	c	= getOBCClass("block.CraftBlock");
	private static Method	m	= getMethod(c, "getNMSBlock");
	
	public static Object getBlockHandleWithException(Object obj) throws Exception{
		return m.invoke(obj);
	}
	
	public static Object getBlockHandle(Object obj){
		try{
			return m.invoke(obj);
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static Object getBlockHandleSilent(Object obj){
		try{
			return m.invoke(obj);
		}catch(Exception e){
			return null;
		}
	}

	private static Field setAccesible(Field field) throws Exception{
		field.setAccessible(true);
		int modifiers = field.getModifiers();
		try {
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
		} catch (NoSuchFieldException e) {
			if ("modifiers".equals(e.getMessage()) || (e.getCause() != null && e.getCause().getMessage() != null &&  e.getCause().getMessage().equals("modifiers"))) {
				// https://github.com/ViaVersion/ViaVersion/blob/e07c994ddc50e00b53b728d08ab044e66c35c30f/bungee/src/main/java/us/myles/ViaVersion/bungee/platform/BungeeViaInjector.java
				// Java 12 compatibility *this is fine*
				Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
				getDeclaredFields0.setAccessible(true);
				Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
				for (Field classField : fields) {
					if ("modifiers".equals(classField.getName())) {
						classField.setAccessible(true);
						classField.set(field, modifiers & ~Modifier.FINAL);
						break;
					}
				}
			} else {
				throw e;
			}
		}
		return field;
	}
	
	public static List<Field> getFields(Class<?> clazz) throws Exception{
		List<Field> f = new ArrayList<Field>();
		for(Field field : clazz.getDeclaredFields()){
			field = setAccesible(field);
			f.add(field);
		}
		return f;
	}
	
	public static List<Field> getFieldsIncludingUpper(Class<?> clazz) throws Exception{
		List<Field> f = new ArrayList<Field>();
		do{
			for(Field field : clazz.getDeclaredFields()){
				field = setAccesible(field);
				f.add(field);
			}
		}while((clazz = clazz.getSuperclass()) != null);
		return f;
	}
	
	public static Field getFieldWithException(Class<?> clazz, String name) throws Exception{
		for(Field field : clazz.getDeclaredFields())
			if(field.getName().equals(name)){
				field = setAccesible(field);
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
				field = setAccesible(field);
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
				field = setAccesible(field);
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
				field = setAccesible(field);
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
		field = setAccesible(field);
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
			e.printStackTrace();
		}
		return null;
	}
	
}
