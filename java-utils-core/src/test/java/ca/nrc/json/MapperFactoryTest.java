package ca.nrc.json;

import ca.nrc.testing.AssertString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MapperFactoryTest {

	//////////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////////

	@Test
	public void test__MapperFactory__Synopsis() {
		// Use this class to generate a pre-configured mapper
		//
		// This gives you a mapper without any special configuration
		ObjectMapper mapper = MapperFactory.mapper();

		// This mapper will sort fields and map keys alphabetically during
		// serialization
		mapper = MapperFactory.mapper(MapperFactory.MapperOptions.SORT_FIELD);
	}

	//////////////////////////////////
	// DOCUMENTATION TESTS
	//////////////////////////////////

	@Test
	public void test__MapperFactory__SORT_FIELDS() throws Exception {
		ObjectMapper mapper =
			MapperFactory.mapper(MapperFactory.MapperOptions.SORT_FIELD);

		Map<String,Object> obj = new HashMap<String,Object>();
		for (int ii=0; ii < 5; ii++) {
			obj.put("field_"+ii, 1);
		}
		String gotJson = mapper.writeValueAsString(obj);
		String expJson = "{\"field_0\":1,\"field_1\":1,\"field_2\":1,\"field_3\":1,\"field_4\":1}";
		AssertString.assertStringEquals(
			"JSON not as expected",
			expJson, gotJson
		);
	}

}
