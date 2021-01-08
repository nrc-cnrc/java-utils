package ca.nrc.web;

import okhttp3.Response;

public  class HttpResponse {
	public String body = "{}";
	public int code = 100;

	public HttpResponse(Response response) {
		try {
			this.body = response.body().string();
		} catch (Exception e) {
		}
		try {
			this.code = response.code();
		} catch (Exception e) {
		}
	}
}
