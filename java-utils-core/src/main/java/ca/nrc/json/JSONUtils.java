package ca.nrc.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {
	public static enum STRUCT {List, Object};
	
	public static Map<String,Object> json2ObjectMap(String json) throws JsonParseException, JsonMappingException, IOException {
		Map<String,Object> objMap = new HashMap<String,Object>();
		objMap = new ObjectMapper().readValue(json, objMap.getClass());
		return objMap;
	}

	public static Map<String, Object> object2ObjectMap(Object obj) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(obj);
		Map<String, Object> map = (Map) mapper.readValue(json, Map.class);
		
		return map;
	}

	
	@SuppressWarnings("unchecked")
	public static <T> List<T> asList(JSONArray jsonArray, Class<T> type) throws JsonParseException, JsonMappingException, IOException {
		String jsonString = jsonArray.toString();
		
		List<T> result = new ArrayList<T>();
		result = new ObjectMapper().readValue(jsonString, result.getClass());
		
		return result;
	}

	public static Object asObject(JSONObject jsonObj) throws JsonParseException, JsonMappingException, IOException {
		return asObject(jsonObj, Object.class);
	}

	
	public static Object asObject(JSONObject jsonObj, Class type) throws JsonParseException, JsonMappingException, IOException {
		String jsonString = jsonObj.toString();
		Object obj  = new ObjectMapper().readValue(jsonString, type);
		
		return obj;
	}

	public static Object map2Object(Map<String,Object> map, Class type) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(map);
		Object obj = mapper.readValue(json, type);
		return obj;
	}

}
