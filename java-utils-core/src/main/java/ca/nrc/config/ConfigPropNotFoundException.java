package ca.nrc.config;

public class ConfigPropNotFoundException extends ConfigException {
    public ConfigPropNotFoundException(String mess) {
        super(mess);
    }

    public ConfigPropNotFoundException(String mess, Exception e) {
        super(mess, e);
    }

    public ConfigPropNotFoundException(Exception e) { super(e); }
}
