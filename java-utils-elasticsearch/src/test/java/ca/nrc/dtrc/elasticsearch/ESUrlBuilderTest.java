package ca.nrc.dtrc.elasticsearch;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

import ca.nrc.testing.AssertHelpers;

public class ESUrlBuilderTest  {

	@Test
	public void test__search_WithScroll() throws Exception {
		URL gotURL = 
				new ESUrlBuilder("test-index", "localhost", 9090)
					.forEndPoint("search")
					.scroll()
					.build();
		;
		String expURL = "http://localhost:9090/test-index/search?scroll=1m";
		AssertHelpers.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__scroll() throws Exception {
		URL gotURL = 
				new ESUrlBuilder("test-index", "localhost", 9090)
					.forEndPoint("_search/scroll")
					.build();
		;
		String expURL = "http://localhost:9090/_search/scroll";
		AssertHelpers.assertStringEquals(expURL, gotURL.toString());
	}
	
	@Test
	public void test__delete_by_query__HappyPath() throws Exception {
		URL gotURL = 
				new ESUrlBuilder("test-index", "localhost", 9090)
					.forEndPoint("_delete_by_query")
					.build();
		;
		String expURL = "http://localhost:9090/test-index/_delete_by_query";
		AssertHelpers.assertStringEquals(expURL, gotURL.toString());
	}
}
