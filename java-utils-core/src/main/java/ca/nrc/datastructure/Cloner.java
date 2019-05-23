package ca.nrc.datastructure;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Cloner {
	
	public static class ClonerException extends Exception {
		public ClonerException(Exception e) {super(e);}
	}

	public  static <T extends Object> T clone(T orig) throws ClonerException {
		T copy = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(orig);
			copy = (T) mapper.readValue(json, orig.getClass());
		} catch (IOException e) {
			throw new ClonerException(e);
		};

		return copy;
	}

}
