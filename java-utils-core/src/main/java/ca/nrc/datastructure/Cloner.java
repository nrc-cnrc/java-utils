package ca.nrc.datastructure;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Cloner {
	
	public static class ClonerException extends Exception {
		public ClonerException(Exception e) {super(e);}
	}

	public static <T extends Object> T clone(T orig) throws ClonerException {
		T copy = null;
		try {
			if (isKeySet(orig)) {
				// HashMap.KeySet needs special processing, because the
				// ObjectMapper class does not deal with it properly.
				copy = (T)cloneKeySet((Set)orig);
			} else {
				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(orig);
				copy = (T) mapper.readValue(json, orig.getClass());
			}
		} catch (IOException e) {
			throw new ClonerException(e);
		};

		return copy;
	}

	private static <T extends Object> boolean isKeySet(T orig) {
		String className = orig.getClass().getName();
		boolean answer = (className.matches(".*?HashMap.KeySet$"));
		return answer;
	}

	public static <T extends Object> Set<?>  cloneKeySet(Set<?> orig) {

		Class clazz = orig.getClass();
		Set regularSet = new HashSet<>();
		regularSet.addAll(orig);


		return regularSet;
	}

}
