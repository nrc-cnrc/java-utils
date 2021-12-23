package ca.nrc.json;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.nrc.introspection.Introspection;
import ca.nrc.introspection.IntrospectionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.log4j.Logger;

/**
 * Generates a DETERMISTIC Json PrettyPrint of any object.
 *
 * Note: This may seem redundant with Jackson or Gson but it's not.
 * 
 * - While it's possible configure Jackson so that it will print the 
 *   keys of Maps in sorted order, there does not seem to be a way to make 
 *   it print entryes of a Set in order
 * - As far as Gson is concerned, it does not seem to support sorting keys of
 *   Maps and elements of Sets.
 *   
 * This PrettyPrinter is far from being bullet proof (in particular, it does 
 * not handle circular references very well). As such it should NOT be used 
 * for the purposes of writing objects to a persistence store.
 * 
 * The PrettyPrinter is mostly used to compare objects during testing, by 
 * their PrettyPrints (see AssertObject class for examples of this use).
 *   
 * @author desilets
 *
 */

public class PrettyPrinter {

	public static enum OPTION {IGNORE_PRIVATE_PROPS};
	
	private Integer decimals = null;
	private boolean ignorePrivateProps = false;
	
	protected Set<Object> alreadySeen = new HashSet<Object>();
	
	private Set<String> fieldsToIgnore = new HashSet<String>();	
	
	public PrettyPrinter() {
	}
	
	public PrettyPrinter(Integer _decimals) {
		init__PrettyPrinter(_decimals, new OPTION[0]);
	}

	public PrettyPrinter(OPTION... _options) {
		init__PrettyPrinter((Integer)null, _options);
	}

	private void init__PrettyPrinter(Integer _decimals, OPTION[] _options) {
		this.decimals = _decimals;
		for (OPTION anOpt: _options) {
			if (anOpt == OPTION.IGNORE_PRIVATE_PROPS) {
				this.ignorePrivateProps = true;
			}
		}
	}

	public static String print(Object obj)  {
		return new PrettyPrinter().pprint(obj);
	}

	public String pprint(Object obj)  {
		String json = pprint(obj, new String[] {});
		return json;
	}

	public static String print(Object obj, Integer _decimals)  {
		return new PrettyPrinter(_decimals).pprint(obj);
	}

	public String pprint(Object obj, Integer _decimals)  {
		PrettyPrinter printer = new PrettyPrinter(_decimals);
		Set<String> fieldsToIgnoreSet = new HashSet<String>();
		String json = printer.prettyPrint(obj, fieldsToIgnoreSet);
		return json;		
	}

	public static String print(Object obj, Set<String> ignoreFields, Integer _decimals)  {
		return new PrettyPrinter(_decimals).pprint(obj, ignoreFields);
	}

	public String pprint(Object obj, Set<String> ignoreFields, Integer _decimals)  {
		PrettyPrinter printer = new PrettyPrinter(_decimals);
		String json = printer.prettyPrint(obj, ignoreFields);
		return json;		
	}

	public static String print(Object obj, String[] fieldsToIgnore)  {
		return new PrettyPrinter().pprint(obj, fieldsToIgnore);
	}

	public String pprint(Object obj, String[] fieldsToIgnore)  {
		Set<String> fieldsToIgnoreSet = new HashSet<String>();
		for (String aFieldName: fieldsToIgnore) fieldsToIgnoreSet.add(aFieldName);
		String json = prettyPrint(obj, fieldsToIgnoreSet);
		return json;
	}

	public static String print(Object obj, Set<String> fieldsToIgnore)  {
		return new PrettyPrinter().pprint(obj, fieldsToIgnore);
	}

	public String pprint(Object obj, Set<String> fieldsToIgnore)  {
		PrettyPrinter printer = new PrettyPrinter();
		String json = printer.prettyPrint(obj, fieldsToIgnore);
		return json;
	}
	public String formatAsSingleLine(String json) {
		json = json.replaceAll("\n\\s*","");
		return json;
	}

	private String prettyPrint(Object obj, Set<String> fieldsToIgnore)  {
		String json = prettyPrint(obj, fieldsToIgnore, 0);
		return json;
	}

	private String prettyPrint(Object obj, Set<String> fieldsToIgnore, int indentLevel)  {
		String json = "";

		boolean loopFound = checkForLoops(obj);
		if (loopFound) {
			return indentation(indentLevel) + "<OBJECT ALREADY SEEN. Not printing again to avoid infinite recursion>";
		}

		Number num = null;
		
		if (obj == null) {
			json = indentation(indentLevel) + "null";
		} else if (obj instanceof String) {
			json = PrettyPrintString((String)obj, indentLevel);
		} else if (obj instanceof Set<?>) {
			json = prettyPrintSet((Set<?>)obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof Map<?,?>) {
			json = prettyPrintMap((Map<?,?>)obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof List<?>) {
			json = prettyPrintList((List<Object>)obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof String) {
		    json = prettyPrintString((String)obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof Boolean) {
			json = prettyPrintBoolean((Boolean)obj, indentLevel);
		} else if ((num = isNumber(obj)) != null) {
			json = prettyPrintNumber(num, indentLevel);
		} else if (obj instanceof int[]) {
			json = prettyPrintIntArray((int[])obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof long[]) {
			json = prettyPrintLongArray((long[])obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof double[]) {
			json = prettyPrintDoubleArray((double[])obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof float[]) {
			json = prettyPrintFloatArray((float[])obj, fieldsToIgnore, indentLevel);
		} else if (obj instanceof Object[] || obj instanceof double[] || obj instanceof int[] || obj instanceof long[]) {
			json = prettyPrintArray((Object[])obj, fieldsToIgnore, indentLevel);			
		} else if (obj instanceof JsonNode) {
			json = prettyPrintJsonNode((JsonNode)obj, fieldsToIgnore, indentLevel);
		} else {
			json = prettyPrintObject(obj, fieldsToIgnore, indentLevel);
		}
		
		if (!loopFound) {
			removeFromAlreadySeen(obj);
		}
		
		return json;
	}

	private String prettyPrintLongArray(long[] arr, Set<String> fieldsToIgnore, int indentLevel)  {
		Long[] objArr = new Long[arr.length];
		for (int ii=0; ii< arr.length; ii++) objArr[ii] = new Long(arr[ii]);
		String json = prettyPrintArray(objArr, fieldsToIgnore, indentLevel);
		return json;
	}

	private String prettyPrintIntArray(int[] arr, Set<String> fieldsToIgnore, int indentLevel)  {
		Integer[] objArr = new Integer[arr.length];
		for (int ii=0; ii< arr.length; ii++) objArr[ii] = new Integer(arr[ii]);
		String json = prettyPrintArray(objArr, fieldsToIgnore, indentLevel);
		return json;
	}

	private String prettyPrintFloatArray(float[] arr, Set<String> fieldsToIgnore, int indentLevel)  {
		Float[] objArr = new Float[arr.length];
		for (int ii=0; ii< arr.length; ii++) objArr[ii] = new Float(arr[ii]);
		String json = prettyPrintArray(objArr, fieldsToIgnore, indentLevel);
		return json;
	}

	private String prettyPrintDoubleArray(double[] arr, Set<String> fieldsToIgnore, int indentLevel)  {
		Double[] objArr = new Double[arr.length];
		for (int ii=0; ii< arr.length; ii++) objArr[ii] = new Double(arr[ii]);
		String json = prettyPrintArray(objArr, fieldsToIgnore, indentLevel);
		return json;
	}

	private String prettyPrintNumber(Number num, int indentLevel) {
		String numStr = null;
		if (decimals != null && (num instanceof Double || num instanceof Float)) {
			// Round the number to the given tolerance level.
			numStr = String.format("%."+this.decimals.toString()+"f", num);
		} else {
			numStr = num.toString();
		}
		
		String baseIndentation = indentation(indentLevel);
		String json = baseIndentation +numStr;

		return json;
	}

	private Number isNumber(Object obj) {
		Number num = null;
		if (obj instanceof Number) {
			num = (Number)obj;
		}
		if (num == null) {
			// Try parsing the objec as various raw number formats (ex: int, float, etc...)
			String str = obj.toString();
			try {
				num = Integer.parseInt(str);
			} catch (Exception e) {}
			
			if (num == null) {
				try {
					num = Long.parseLong(str);
				} catch (Exception e) {}
			}

			if (num == null) {
				try {
					num = Double.parseDouble(str);
				} catch (Exception e) {}
			}
			
			if (num == null) {
				try {
					num = Float.parseFloat(str);
				} catch (Exception e) {}
				
			}	
		}
		
		return num;
	}

	private Class getNumberClass(Object obj) {
		// TODO Auto-generated method stub
		return null;
	}

	private String PrettyPrintString(String aString, int indentLevel) {
		String json = indentation(indentLevel);

		aString = aString.replace("\\", "\\\\");
		aString = aString.replaceAll("\"", "\\\\\"");

		json += "\"" + aString + "\"";
		return json;
	}

	private String prettyPrintBoolean(Boolean bool, int indentLevel) {
		String json = indentation(indentLevel);
		if (bool) {
			json += "true";
		} else {
			json += "false";
		}
		return json;
	}

	private String prettyPrintArray(Object[] arr, Set<String> fieldsToIgnore2, int indentLevel)  {
		List<Object> arrAsList = new ArrayList<Object>();
		for (int ii=0; ii < arr.length; ii++) {
			arrAsList.add(arr[ii]);
		}
		String json = prettyPrintList(arrAsList, fieldsToIgnore2, indentLevel);

		return json;
	}

	private String prettyPrintJsonNode(JsonNode node, Set<String> fieldsToIgnore, int indentLevel)  {
		String baseIndentation = indentation(indentLevel);

		String json = null;
		if (node.isNull()) {
			json = baseIndentation + "null";
		} else  if (node.isTextual()) {
			json = baseIndentation + "\"" + node.asText() + "\"";		
		} else if (node.isBoolean()) {
			json = baseIndentation + node.asBoolean();
		} else if (node.isInt()) {
			json = baseIndentation + node.asInt();
		} else if (node.isFloat() || node.isDouble()) {
			json = baseIndentation + node.asDouble();
		} else if (node.isArray()) {
			json = prettyPrintJsonArray((ArrayNode)node, indentLevel);
		} else if (node.isObject()) {
			json = prettyPrintJsonObject((ObjectNode)node, fieldsToIgnore, indentLevel);
		} else {
			throw new RuntimeException("Could not convert Json node: "+node);
		}
		
		return json;
	}

	private String prettyPrintJsonObject(ObjectNode oNode, Set<String> fieldsToIgnore, int indentLevel)  {
		String baseIndentation = indentation(indentLevel);
		
		List<String> fieldNames = new ArrayList<String>();
		Iterator<String> iter = oNode.fieldNames();
		while (iter.hasNext()) {
			fieldNames.add(iter.next());	
		}
		Collections.sort(fieldNames);

		String json = baseIndentation + "{";
		boolean first = true;		
		for (String aFieldName: fieldNames) {
			if (!first) {
				json = json + ",";
			}			
			first = false;							
			json = json + "\n" + indentation(indentLevel+1) + "\"" + aFieldName + "\":\n";
			JsonNode aFieldValue = oNode.get(aFieldName);
			json = json + prettyPrint(aFieldValue, fieldsToIgnore, indentLevel+2);
		}
		json = json + "\n" + baseIndentation + "}";
		
		return json;
	}

	private String prettyPrintJsonArray(ArrayNode aNode, int indentLevel)  {
		String baseIndentation = indentation(indentLevel);
		String json = baseIndentation + "[\n";
		
		for (int ii=0; ii < aNode.size(); ii++) {
			JsonNode elt = aNode.get(ii);
			json = json + prettyPrint(elt, fieldsToIgnore, indentLevel+1);
			if (ii < aNode.size()-1) {
				json = json + ",";
			}
			json = json + "\n";
		}
		json = json + baseIndentation + "]";
		return json;	}

	private String prettyPrintObject(Object obj, Set<String> fieldsToIgnore, int indentLevel)  {
		Logger logger = Logger.getLogger("ca.nrc.json.PrettyPrinter.prettyPrintObject");
		String className = null;
		if (logger.isTraceEnabled()) {
			className = (obj==null?null:obj.getClass().getSimpleName());
		}
		logger.trace("Printing object of class: "+className);
		String baseIndentation = indentation(indentLevel);;
		if (fieldsToIgnore == null) {
			fieldsToIgnore = new HashSet<String>();
		}

		String json = null;
		alreadySeen.add(obj);
		try {
			Map<String,Object> fieldValues = Introspection.fieldValues(obj);
			List<String> fieldNames = sortFieldNames(fieldValues);
			json = baseIndentation + "{";
			boolean first = true;
			for (String aFieldName: fieldNames) {
				logger.trace(className+": Looking at aFieldName="+aFieldName);
				if (aFieldName.matches("this\\$\\d+")) {
					// Reference to an inner class method or something...
					// won't deal with those.
					continue;
				}
				if (fieldsToIgnore.contains(aFieldName)) {
					// This is a field to be ignored
					continue;
				}
				if (!first) {
					json = json + ",";
				}
				first = false;
				json = json + "\n" + indentation(indentLevel+1) + "\"" + aFieldName + "\":\n";

				Object aFieldValue = fieldValues.get(aFieldName);
				json = json + prettyPrint(aFieldValue, fieldsToIgnore, indentLevel+2);
			}
			json = json + "\n" + baseIndentation + "}";

		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		logger.trace("Returning json="+json);


		return json;
	}

	private List<String> sortFieldNames(Map<String, Object> fieldValues) {
		List<String> names = new ArrayList<String>();
		names.addAll(fieldValues.keySet());
		names.sort((String n1, String n2) -> n1.compareToIgnoreCase(n2));
		return names;
	}


	private String prettyPrintString(String str, Set<String> fieldsToIgnore, int indentLevel) {
		String baseIndentation = indentation(indentLevel);
		String json = baseIndentation + "\""+str+"\"";
		return json;
	}

	private String prettyPrintList(List<Object> list, Set<String> fieldsToIgnore, int indentLevel)  {
		String baseIndentation = indentation(indentLevel);
		String json = baseIndentation + "[\n";
		int counter = 1;
		for (Object elt: list) {
			json = json + prettyPrint(elt, fieldsToIgnore, indentLevel+1);
			if (counter < list.size()) {
				json = json + ",";
			}
			json = json + "\n";
			counter++;
		}
		json = json + baseIndentation + "]";
		return json;
	}

	private String prettyPrintMap(Map<?,?> map, Set<String> fieldsToIgnore, int indentLevel)  {
		String indentation = indentation(indentLevel);
		String json = null;
		
		String baseIndentation = indentation(indentLevel);
		String subIndentation = indentation(indentLevel+1);
		
		// Check if the keys are of known type of Comparables
		Iterator iterator = map.keySet().iterator();
		if (!iterator.hasNext()) {
			json = baseIndentation + "{\n" + baseIndentation + "}";
		} else {
			Object firstKey = iterator.next();
			Map<String, Object> blah;
			if (firstKey != null) {
				if (firstKey instanceof String) {
					json = prettyPrintMapOfComparables((Map<String,Object>)map, fieldsToIgnore, indentLevel);
				} else if (firstKey instanceof Integer) {
					json = prettyPrintMapOfComparables((Map<Integer,Object>)map, fieldsToIgnore, indentLevel);
				} else if (firstKey instanceof Double) {
					json = prettyPrintMapOfComparables((Map<Double,Object>)map, fieldsToIgnore, indentLevel);
				}
			} 
			
			if (json == null) {
				// Keys were not of a known Comparable type
				// Just print them in whatever order the KeySet produces them.
				json = baseIndentation + "{\n";
				
				boolean first = true;
				for (Object aKey: map.keySet()) {
					if (!first) json = json + ",\n";
					String aKeyStr = "null";
					if (aKey != null) aKeyStr = aKey.toString();
					if (fieldsToIgnore.contains(aKeyStr)) {
						continue;
					}

					json = json + subIndentation + "\"" + aKeyStr + "\":\n";
					json = json + prettyPrint(map.get(aKey), fieldsToIgnore, indentLevel+2);
					first = false;
				}
				
				json = json + "\n" + baseIndentation + "}";
			}
		}
		
		return json;
	}
	
	protected <T extends Comparable<? super T>> String prettyPrintMapOfComparables(Map<T, Object> map, Set<String> fieldsToIgnore, int indentLevel)  {
		class CustomComp<T extends Comparable<? super T>> implements Comparator<T> {
			@Override
			public int compare(T o1, T o2) {
				int cmp = o1.compareTo(o2);
				return cmp;
			}
		}
		
		List<T> keysSorted = new ArrayList<T>();
		keysSorted.addAll(map.keySet());
		Collections.sort(keysSorted, new CustomComp<T>());
		
		String baseIndentation = indentation(indentLevel);
		String subIndentation = indentation(indentLevel+1);
		String json = baseIndentation + "{\n";
		
		boolean first = true;
		for (T aKey: keysSorted) {
			if (aKey != null && fieldsToIgnore.contains(aKey.toString())) {
				continue;
			}
			if (!first) json = json + ",\n";
			json = json + subIndentation + "\"" + aKey.toString() + "\":\n";
			json = json + prettyPrint(map.get(aKey), fieldsToIgnore, indentLevel+2);
			first = false;
		}
		
		json = json + "\n" + baseIndentation + "}";
		
		return json;
	}	
	
	
	protected <T extends Comparable<? super T>> List<T> sortedKeys(Map<T, Object> map) {
		class CustomComp<T extends Comparable<? super T>> implements Comparator<T> {
			@Override
			public int compare(T o1, T o2) {
				int cmp = o1.compareTo(o2);
				return cmp;
			}
		}
		
		List<T> keysSorted = new ArrayList<T>();
		keysSorted.addAll(map.keySet());
		Collections.sort(keysSorted, new CustomComp<T>());
		
		return keysSorted;
	}	
	
	
	protected String prettyPrintSet(Set<?> set, Set<String> fieldsToIgnore, int indentLevel)  {
		String json = null;
		if (set.size() > 0) {
			Object firstElement = set.iterator().next();
			if (firstElement instanceof Comparable) {
				json = prettyPrintSetOfComparables((Set<Comparable>) set, fieldsToIgnore, indentLevel);
			} else {
				// Elements of the set do not belong to a known class of Comparables
				// Just print them in whatever order the Set class orders them
				List<Object> list = new ArrayList<Object>();
				list.addAll(set);
				json = prettyPrintList(list, fieldsToIgnore, indentLevel);
			}
		} else {
			// Empty set
			json = indentation(indentLevel) + "[\n" + indentation(indentLevel) + "]";
		}
		
		return json;		
	}
	
	protected <T extends Comparable<? super T>> String prettyPrintSetOfComparables(Set<T> set, Set<String> fieldsToIgnore, int indentLevel)  {
		class CustomComp<T extends Comparable<? super T>> implements Comparator<T> {
			@Override
			public int compare(T o1, T o2) {
				int cmp = o1.compareTo(o2);
				return cmp;
			}
		}
		
		List<T> setSorted = new ArrayList<T>();
		setSorted.addAll(set);
		Collections.sort(setSorted, new CustomComp<T>());
		
		String json = prettyPrintList((List)setSorted, fieldsToIgnore, indentLevel);

		return json;
	}	
	
	private String indentation(int indentLevel) {
		String indentationString = "";
		for (int iiIndent = 0; iiIndent < indentLevel; iiIndent++) {indentationString = indentationString + "  ";}
		return indentationString;
	}

	protected boolean elementsAreComparable(Collection<?> coll) {
		boolean comparable = false;
		if (coll.size() > 0) {
			Object firstElement = coll.iterator().next();
			Class type = firstElement.getClass();
			if (Comparable.class.isAssignableFrom(type)) {
				comparable = true;
			}
		}
		return comparable;
	}

	protected boolean checkForLoops(Object obj) {
		boolean loopFound = false;
		
		if (alreadySeen.contains(obj)) {
			loopFound = true;
		}
		alreadySeen.add(obj);
		
		return loopFound;
	}
	
	public void removeFromAlreadySeen(Object obj) {
		if (!alreadySeen.contains(obj)) {
			throw new RuntimeException("Tried to remove an unseen object from set of seen.");
		} else {
			alreadySeen.remove(obj);
		}
	}

	public  List<Field> getAllFields(Object obj) {
		Class type = obj.getClass();
		List<Field> fields = getAllFields(new ArrayList<Field>(), type);
		return fields;
	}

	
	public List<Field> getAllFields(List<Field> fields, Class<?> type) {
		Field[] typeFields = null;
		if (this.ignorePrivateProps) {
			typeFields = type.getFields();
		} else {
			typeFields = type.getDeclaredFields();
		}
		fields.addAll(Arrays.asList(typeFields));

		if (type.getSuperclass() != null) {
		  getAllFields(fields, type.getSuperclass());
		}

		return fields;
	}
}
