package ca.nrc.dtrc.elasticsearch;

import java.util.HashMap;
import java.util.Map;

public class FieldDef {
	
	public static enum Types {text, integer, keyword, binary, date, ip, object, nested};
	

	Types type = Types.text;
	
	// This one only applies when type = Type.text
	// If set to null, it means the text is not analyzed (i.e. no stemming, nor stopword removal)
	String analyzerLang = "en";

	public FieldDef() {
		initialize(null, null);
	}

	public FieldDef(Types _type) {
		initialize(_type, null);
	}
	
	public void initialize(Types _type, String _analyzerLang) {

		if (_type == null) {
			_type = Types.text;
		}
		this.type = _type;
		
		_analyzerLang = getLangFullName(_analyzerLang);
		
		this.analyzerLang = _analyzerLang;
	}
	
	public FieldDef setAnalyzer(String lang) {
		this.analyzerLang = lang;
		return this;
	}
	
	public Map<String,Object> toMap() {
		
		Map<String,Object> fieldMap = new HashMap<String,Object>();
		fieldMap.put("type", type.toString());
		if (analyzerLang != null) {
			fieldMap.put("analyzer", analyzerLang);
		}
		
		return fieldMap;
	}
	
	private String getLangFullName(String lang) {
		if (lang != null) {
			if (lang.equals("en")) {
				lang = "english";
			} else if (lang.equals("fr")) {
				lang = "french";
			}
		}		
		return lang;
	}

	public FieldDef setType(Types _type) {
		this.type = _type;
		return this;
	}

}
