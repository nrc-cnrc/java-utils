package ca.nrc.introspection;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Introspection {

	private static Map<Pair<Class,Boolean>, Set<String>> cachedFieldNames =
		new HashMap<Pair<Class,Boolean>, Set<String>>();

	private static Map<Pair<Class,Boolean>, Field[]> cachedFields =
		new HashMap<Pair<Class,Boolean>, Field[]>();

	public static <T> T downcastTo(Class<T> cls, Object obj)
	{
		return cls.cast(obj); // no warning
	}

	public static Set<String> fieldNames(Class clazz) throws IntrospectionException {
		return fieldNames(clazz, (Boolean)null);
	}

	public static Set<String> fieldNames(Class clazz, Boolean publicFieldsOnly) throws  IntrospectionException {
		Logger tLogger = Logger.getLogger("ca.nrc.introspection.Introspection.fields");
		Set<String> names = uncacheFieldNames(clazz, publicFieldsOnly);
		if (names == null) {
			Object obj = prototype4class(clazz);
			Map<String, Object> fieldVals = fieldValues(obj, publicFieldsOnly);
			names = fieldVals.keySet();
			cacheFieldNames(clazz, publicFieldsOnly, names);
		}
		return names;
	}

	private static synchronized void cacheFieldNames(Class clazz, Boolean publicFieldsOnly,
		Set<String> names) {
		cachedFieldNames.put(Pair.of(clazz,publicFieldsOnly), names);
	}

	private static synchronized Set<String> uncacheFieldNames(Class clazz, Boolean publicFieldsOnly) {
		Set<String> names = cachedFieldNames.get(Pair.of(clazz,publicFieldsOnly));
		return names;
	}

	public static Map<String,Object> fieldValues(Class clazz)
		throws IntrospectionException {
		return fieldValues(prototype4class(clazz), (Boolean)null);
	}

	public static Map<String,Object> fieldValues(Object obj)
		throws IntrospectionException {
		return fieldValues(obj, (Boolean)null);
	}

	public static Map<String,Object> fieldValues(Class clazz, Boolean publicFieldsOnly)
		throws IntrospectionException {
		return fieldValues(prototype4class(clazz), publicFieldsOnly);
	}

	public static Map<String,Object> fieldValues(Object obj, Boolean publicFieldsOnly)
		throws IntrospectionException {
		
		Logger tLogger = Logger.getLogger("ca.nrc.introspection.Introspection.fieldValues");

		if (publicFieldsOnly == null) {
			publicFieldsOnly = true;
		}
		Class clazz = obj.getClass();
		tLogger.trace("invoked on object of class: "+clazz);
		
		Map<String,Object> fields = new HashMap<String,Object>();
		
		// Get public member variables
		Field[] memberVariables = uncacheFields(clazz, publicFieldsOnly);
		if (memberVariables == null) {
			if (publicFieldsOnly) {
				memberVariables = obj.getClass().getFields();
			} else {
				memberVariables = obj.getClass().getDeclaredFields();
			}
			cacheFields(clazz, publicFieldsOnly, memberVariables);
		}
		
		// Filter static and JSONIgnored fields
		for (Field aField: memberVariables) {
			if (aField.isAnnotationPresent(JsonIgnore.class)) {
				continue;
			}
			if(Modifier.isStatic(aField.getModifiers())) {
				continue;
			}
			String fieldName = aField.getName();
			Object fieldValue;
			try {
				aField.setAccessible(true);
				fieldValue = aField.get(obj);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new IntrospectionException(
					"Could not access field "+fieldName+" of object "+obj);
			}
			fields.put(fieldName, fieldValue);
		}
		
		// Get fields that are private but have a public 'get' method
		PropertyDescriptor[] propDescriptors;
		try {
			propDescriptors = Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors();
		} catch (java.beans.IntrospectionException e1) {
			throw new IntrospectionException(
				"Could not obtain property descriptors for class "+obj.getClass(), e1);
		}	
		for(PropertyDescriptor pd : propDescriptors) {
			Method meth = pd.getReadMethod();
			if (meth == null) continue;
			String methName = meth.getName();
			if (methName.equals("getClass")) continue;
			JsonIgnore ann = meth.getAnnotation(JsonIgnore.class);
			Object methValue;
			tLogger.trace("Getting value of public method named "+methName);
			try {
				methValue = meth.invoke(obj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IntrospectionException(
					"Could not invoke method "+methName+" of object "+obj, e);
			}
			if (ann == null) {
				String fieldName = methName.substring(4, methName.length());
				String firstChar = methName.substring(3,4).toLowerCase();
				fieldName = firstChar + fieldName;
				fields.put(fieldName, methValue);
			}
		}
		
		return fields;
	}

	private static synchronized void cacheFields(Class clazz, Boolean publicFieldsOnly,
		Field[] fields) {
		cachedFields.put(Pair.of(clazz,publicFieldsOnly), fields);
	}

	private static synchronized Field[] uncacheFields(Class clazz, Boolean publicFieldsOnly) {
		Field[] fields = cachedFields.get(Pair.of(clazz,publicFieldsOnly));
		return fields;
	}


	public static Object getFieldValue(Object obj, String fldName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
		return getFieldValue(obj, fldName, true);
	}	
	
	public static Object getFieldValue(Object obj, String fldName, boolean failIfNotFound) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
		Map<String,Object> publicFields = fieldValues(obj);
		Object value = null;
		if (! publicFields.containsKey(fldName) && failIfNotFound) {
			throw new IntrospectionException("Object of class "+obj.getClass().getName()+" does not have a public attribute with name "+fldName);
		}
		value = publicFields.get(fldName);
		
		return value;
	}

	public static <T extends Object> T prototype4class(Class<T> clazz) throws IntrospectionException {
		T proto = null;
		try {
			Constructor<?> ctor = clazz.getConstructor();
			proto = (T) ctor.newInstance();
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IntrospectionException(e);
		}
		return proto;
	}
}
