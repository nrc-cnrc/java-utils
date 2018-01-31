package ca.nrc.ui.web.testing;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

public class MockServletOutputStream extends ServletOutputStream {

	StringBuilder outputCapture = new StringBuilder();		
	
	public MockServletOutputStream(StringBuilder _outputCapture) {
		this.outputCapture = _outputCapture;
	}

	@Override
	public void write(int b) throws IOException {
		byte[] charByteArray = new byte[] {(byte)b};
		String charString = new String(charByteArray, "UTF-8");
		outputCapture.append(charString);
	}
	
	
	@Override
	public void write(byte b[], int off, int len) throws IOException {
		outputCapture.append(new String(b, "UTF-8"));
	}	
	
	@Override
	public String toString() {
		String value = outputCapture.toString();
		return value;
	}	
	
}
