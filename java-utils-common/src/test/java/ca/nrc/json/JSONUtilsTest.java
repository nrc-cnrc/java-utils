package ca.nrc.json;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class JSONUtilsTest extends JSONUtils {

	public static class SuperPerson {
		public String name = null;
		public List<SuperPerson> ennemies = new ArrayList<SuperPerson>();
		public SuperPerson nemesis = null;
	}
	
	@Test
	public void test__JSONUtils__Synopsis() throws Exception {
		
		// Say you want to manipulate a JSONObject parsed from this string
		String json = 
				  "{\n"
		        + "  \"name\": \"Batman\", \n"
		        + "  \"ennemies\": [{\"name\": \"Joker\"}, {\"name\": \"Riddler\"}], \n"
		        + "  \"nemesis\": {\"name\": \"Joker\"}\n"
		        + "}"
		        ;
		JSONObject jObj = new JSONObject(json);
		
		// More specifically, you want to get the "ennemies" array of this object.
		//
		// You can do it using JSONUtils.asList() like this:
		//
		List<SuperPerson> ennemies = JSONUtils.asList(jObj.getJSONArray("ennemies"), SuperPerson.class);		
		
		// Or say, you want to access a sub-component of the JSON object that corresponds
		// to a known class, for example the "nemesis" field of the above structure
		SuperPerson nemesis = (SuperPerson) JSONUtils.asObject(jObj.getJSONObject("nemesis"), SuperPerson.class);
	}

}
