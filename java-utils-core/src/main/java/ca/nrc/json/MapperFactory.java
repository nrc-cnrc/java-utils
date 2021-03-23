package ca.nrc.json;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang.ArrayUtils;

public class MapperFactory {

	public static enum MapperOptions {SORT_FIELD};

	public static ObjectMapper mapper(MapperOptions... options) {
		ObjectMapper mapper = new ObjectMapper();
		if (options != null &&
			ArrayUtils.contains(options, MapperOptions.SORT_FIELD)) {
			mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
			mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
		}

		return mapper;
	}

}
