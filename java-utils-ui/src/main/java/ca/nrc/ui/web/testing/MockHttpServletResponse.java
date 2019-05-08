package ca.nrc.ui.web.testing;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import ca.nrc.testing.outputcapture.CapturedPrintWriter;

public class MockHttpServletResponse implements HttpServletResponse {

	private StringBuilder capturedOutput = new StringBuilder();
	private ServletOutputStream oStream = new MockServletOutputStream(capturedOutput);
	private CapturedPrintWriter capturedWriter = new CapturedPrintWriter(capturedOutput);
	
	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return oStream;
	}
	
	public String getOutput() throws IOException {
		String output = getOutputStream().toString();
		return output;
	}
	
	
	@Override
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return capturedWriter;
	}

	@Override
	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetBuffer() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentLength(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContentType(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addCookie(Cookie arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean containsHeader(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String encodeUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendError(int arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
	
//	@Override
//	public void setLocale(Locale loc) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setContentType(String type) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setContentLengthLong(long len) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setContentLength(int len) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setCharacterEncoding(String charset) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setBufferSize(int size) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void resetBuffer() {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void reset() {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public boolean isCommitted() {
//		// TODO Auto-generated method stub
//		return false;
//	}
//	
//	@Override
//	public PrintWriter getWriter() throws IOException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public ServletOutputStream getOutputStream() throws IOException {
//		return oStream;
//	}
//	
//	@Override
//	public Locale getLocale() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public String getContentType() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public String getCharacterEncoding() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public int getBufferSize() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//	
//	@Override
//	public void flushBuffer() throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setStatus(int sc, String sm) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setStatus(int sc) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setIntHeader(String name, int value) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setHeader(String name, String value) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void setDateHeader(String name, long date) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void sendRedirect(String location) throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void sendError(int sc, String msg) throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void sendError(int sc) throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public int getStatus() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//	
//	@Override
//	public Collection<String> getHeaders(String name) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public Collection<String> getHeaderNames() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public String getHeader(String name) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public String encodeUrl(String url) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public String encodeURL(String url) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public String encodeRedirectUrl(String url) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public String encodeRedirectURL(String url) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	@Override
//	public boolean containsHeader(String name) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//	
//	@Override
//	public void addIntHeader(String name, int value) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void addHeader(String name, String value) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void addDateHeader(String name, long date) {
//		// TODO Auto-generated method stub
//		
//	}
//	
//	@Override
//	public void addCookie(Cookie cookie) {
//		// TODO Auto-generated method stub
//		
//	}
//};
//
//return response;
//}	
	

}
