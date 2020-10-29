package ca.nrc.config;

public class InvalidPropJsonException extends ConfigException {
    public InvalidPropJsonException(String propName, String propJson, Class clazz) {
        super("Value of property '"+propName+"' was not the JSON serialization of a valid instance of class '"+clazz.getSimpleName()+"'");
    }
}
