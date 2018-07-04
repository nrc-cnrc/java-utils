package ca.nrc.reflection;

public class Reflection {
	
	public static <T> T downcastTo(Class<T> cls, Object obj)
	{
		return cls.cast(obj); // no warning
	}
}
