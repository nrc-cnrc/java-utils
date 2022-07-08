package ca.nrc.testing.outputcapture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapturedPrintWriter extends PrintWriter {
    private StringBuilder outputCapture = new StringBuilder();

    public CapturedPrintWriter(StringBuilder _outputCapture) {
    	super(new ByteArrayOutputStream());
    	this.outputCapture = _outputCapture;
    }

    public CapturedPrintWriter(Writer out) {
        super(out);
    }

    public CapturedPrintWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public CapturedPrintWriter(OutputStream out) {
        super(out);
    }

    public CapturedPrintWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public CapturedPrintWriter(String fileName) throws FileNotFoundException {
        super(fileName);
    }

    public CapturedPrintWriter(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(fileName, csn);
    }

    public CapturedPrintWriter(File file) throws FileNotFoundException {
        super(file);
    }

    public CapturedPrintWriter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(file, csn);
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        super.write(cbuf, off,len);
        outputCapture.append(cbuf, off, len);
    }

    @Override
    public void write(String s, int off, int len) {
        super.write(s, off,len);
        outputCapture.append(s, off, len);
    }
    
    @Override
    public void println(String s) {
    	super.println(s);
    	outputCapture.append(s+"\n");
    }

    @Override
    public void print(String s) {
    	super.print(s);
    	outputCapture.append(s);
    }
    
    public String getCaputredOutput() {
        return outputCapture.toString();
    }

}
