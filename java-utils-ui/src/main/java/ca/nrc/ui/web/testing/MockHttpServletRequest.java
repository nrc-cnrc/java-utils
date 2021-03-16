package ca.nrc.ui.web.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.iterators.IteratorEnumeration;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

//import org.apache.commons.collections.iterators.IteratorEnumeration;

public class MockHttpServletRequest implements HttpServletRequest {

	private Map<String,List<Object>> attributes = new HashMap<String,List<Object>>();
	private Map<String,List<String>> parameters = new HashMap<String,List<String>>();
	private String uri;

	@Override
	public void setAttribute(String name, Object value) {
		List<Object> valuesList = this.attributes.get(name);
		if (valuesList == null) {
			valuesList = new ArrayList<Object>();
			this.attributes.put(name, valuesList);
		}
		valuesList.add(value);
	}

	@Override
	public Object getAttribute(String name) {
		List<Object> valuesList = attributes.get(name);
		return valuesList.get(0);
	}

	@Override
	public Enumeration getAttributeNames() {
		Enumeration enumeration = new IteratorEnumeration(attributes.keySet().iterator());
		return enumeration;
	}
	
	@Override
	public String getParameter(String name) {
		String value = null;
		List<String> valuesList = parameters.get(name);
		if (valuesList != null && valuesList.size() > 0) {
			value = valuesList.get(0);
		}
		return value;
	}

	@Override
	public Map getParameterMap() {
		return parameters;
	}
	
	/* 
	 * NOTE: This method is NOT part of the standard HttpServletRequest 
	 * interface. The standard interface does NOT provide a way to set a 
	 * parameter.
	 * 
	 * But for testing purposes, we need to be able to set parameters in 
	 * the tests.
	 */
	public MockHttpServletRequest setParameter(String name, String value) {
		List<String> valuesList = this.parameters.get(name);
		if (valuesList == null) {
			valuesList = new ArrayList<String>();
			this.parameters.put(name, valuesList);
		}
		valuesList.add(value);
		return this;
	}

	@Override
	public Enumeration getParameterNames() {
		Enumeration enumeration = new IteratorEnumeration(parameters.keySet().iterator());
		return enumeration;
	}

	@Override
	public String[] getParameterValues(String name) {
		List<String> valuesList = parameters.get(name);
		String[] valuesArr = valuesList.toArray(new String[valuesList.size()]);
		return valuesArr;
	}	
	
	@Override
	public BufferedReader getReader() throws IOException {
		String jsonBody = new ObjectMapper().writeValueAsString(this.parameters);
		BufferedReader reader = new BufferedReader(new StringReader(jsonBody));
		return reader;
	}
	
	public void setReaderContent(String json) throws JsonParseException, JsonMappingException, IOException {
		this.parameters = new ObjectMapper().readValue(json, this.parameters.getClass());
	}
	

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getContentLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocalPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration getLocales() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRealPath(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteAddr() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteHost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getScheme() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServerName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getServerPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getAuthType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContextPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getDateHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIntHeader(String arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getMethod() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathTranslated() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQueryString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRemoteUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestURI() {
		return this.uri;
	}

	public MockHttpServletRequest setURI(String _uri) {
		this.uri = _uri;
		return this;
	}
	

	@Override
	public StringBuffer getRequestURL() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServletPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpSession getSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}
}