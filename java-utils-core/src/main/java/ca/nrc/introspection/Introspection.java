package ca.nrc.introspection;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Introspection {
	
	public static <T> T downcastTo(Class<T> cls, Object obj)
	{
		return cls.cast(obj); // no warning
	}
	
	public static Map<String,Object> publicFields(Class clazz) 
			throws 
			IntrospectionException { 
		Object obj = null;
		try {
			obj = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			new IntrospectionException("No empty constructor for class "+clazz);
		}
		return publicFields(obj);
	}
	
	public static Map<String,Object> publicFields(Object obj) 
			throws 
//			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			IntrospectionException {
		Map<String,Object> fields = new HashMap<String,Object>();
		
		// Get public member variables
		Field[] memberVariables = obj.getClass().getFields();
		
		// Filter static fields
		for (int ii=0; ii < memberVariables.length; ii++) {
			Field aField = memberVariables[ii];
			String fieldName = aField.getName();
			Object fieldValue;
			try {
				fieldValue = aField.get(obj);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new IntrospectionException(
					"Could not access field "+fieldName+" of object "+obj);
			}
			fields.put(fieldName, fieldValue);
		}
		
		// Get fields that are private but have a public 'get' method
		for(PropertyDescriptor pd : 
		    Introspector.getBeanInfo(obj.getClass()).getPropertyDescriptors()){
			
			Method meth = pd.getReadMethod();
			if (meth == null) continue;
			String methName = meth.getName();
			if (methName.equals("getClass")) continue;
			JsonIgnore ann = meth.getAnnotation(JsonIgnore.class);
			Object methValue;
			try {
				methValue = meth.invoke(obj);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IntrospectionException(
					"Could not invoke method "+methName+" of object "+obj);
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

	public static Object getFieldValue(Object obj, String fldName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
		return getFieldValue(obj, fldName, true);
	}	
	
	public static Object getFieldValue(Object obj, String fldName, boolean failIfNotFound) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
		Map<String,Object> publicFields = publicFields(obj);
		Object value = null;
		if (! publicFields.containsKey(fldName) && failIfNotFound) {
			throw new IntrospectionException("Object of class "+obj.getClass().getName()+" does not have a public attribute with name "+fldName);
		}
		value = publicFields.get(fldName);
		
		return value;
	}
}
