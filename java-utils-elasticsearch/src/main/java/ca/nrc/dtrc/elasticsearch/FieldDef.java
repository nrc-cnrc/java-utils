package ca.nrc.dtrc.elasticsearch;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class FieldDef {
	
	public static enum Types {text, integer, keyword, binary, date, ip, object, nested};
	

	public Types type = Types.text;
	
	// This one only applies when type = Type.text
	// If set to null, it means the text is not analyzed (i.e. no stemming, nor stopword removal)
	private String __analyzerLang = null;
	
	public String getAnalyzer() {
		String analyzer = null;
		if (type == Types.text) {
			if (__analyzerLang == null) {
//				analyzer = "english";
				analyzer = null;
			} else if (__analyzerLang.matches("[nN]one")) {
				analyzer = null;
			} else {
				analyzer = __analyzerLang;
			}
		}
		return analyzer;
	}

	public FieldDef setAnalyzer(String lang) {
		__analyzerLang = getLangFullName(lang);
		return this;
	}
	

	public FieldDef() {
		initialize(null, null);
	}

	public FieldDef(Types _type) {
		initialize(_type, null);
	}
	
	public void initialize(Types _type, String _analyzerLang) {

		_analyzerLang = getLangFullName(_analyzerLang);
		if (type == Types.text && _analyzerLang == null) {
			// By default, text fields are analyzed with the "english" analyzer
			// meaning that stopwords are removed and words are stemmed
			_analyzerLang = "english";
		}
		
		this.__analyzerLang = _analyzerLang;		
	}
		
	public FieldDef setType(Types _type) {
		this.type = _type;
		if (type == Types.text && __analyzerLang == null) {
			// By default, text fields are analyzed with the "english" analyzer
			// meaning that stopwords are removed and words are stemmed
			__analyzerLang = "english";
		}
		return this;
	}
	
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", type.toString());
		String analyzer = getAnalyzer();
		if (analyzer != null) {
			json.put("analyzer", analyzer);
		}

		return json;
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

}
