package ca.nrc.web;

import org.junit.Test;

import java.net.URL;

public class HttpTest {

	@Test
	public void test__Http__Synopsis() throws Exception {
		// Use Http to carry out various Http requests
		//

		// For example, here is how you carry out a GET request
		URL url = new URL("http://www.google.com/");
		HttpResponse resp = Http.get(url);

		// The HttpResponse result allows you to know the response code (ex: 200)
		int respCode = resp.code;
		// And to get the string returned by the response
		String body = resp.body;

		// You can invoke any of the Http method (GET, POST, PUT, DELETE, HEAD)
		//
		String jsonBody = "{}";
		resp = Http.post(url, jsonBody);
		resp = Http.put(url, jsonBody);
		resp = Http.delete(url);

	}
}
