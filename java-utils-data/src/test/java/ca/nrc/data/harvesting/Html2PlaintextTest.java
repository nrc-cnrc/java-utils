package ca.nrc.data.harvesting;

import static org.junit.Assert.*;

import org.junit.Test;

import ca.nrc.testing.AssertString;

public class Html2PlaintextTest {
	
	Html2Plaintext converter = new Html2Plaintext();

	@Test
	public void test__toPlainText__HappyPath() {
		String html = 
			"<body>\n"+
			"<script>some js crap</script>\n"+
			"<script>some more JS crap</script>\n"+
			"<h1>Hello world</h1>"+
			"<div>Take me to your leader</div>"+
			"<div>Right now</div>"+			
			"</body>"
			;
		String gotText = converter.toPlaintext(html);
		String expText = 
			"\n"+
			"Hello world\n"+
			"\n"+
			"Take me to your leader\n"+
			"Right now"
				;
		AssertString.assertStringEquals(
			"HTML not properly rendered to plaintext",
			expText, gotText);
	}

}
