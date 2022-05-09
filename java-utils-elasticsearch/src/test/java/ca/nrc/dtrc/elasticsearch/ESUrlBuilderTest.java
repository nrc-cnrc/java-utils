package ca.nrc.dtrc.elasticsearch;

import java.net.URL;

import ca.nrc.testing.AssertString;
import org.junit.Test;

public class ESUrlBuilderTest  {

	@Test
	public void test__putDocument__es5() throws Exception {
		URL gotURL = new ESUrlBuilder("test-index", "localhost", 9090)
			.forDocType("sometype")
			.forDocID("somedoc")
			.refresh(false)
			.build();
		;
		String expURL = "http://localhost:9090/test-index/sometype/sometype:somedoc";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__putDocument__es7() throws Exception {
		URL gotURL = new ESUrlBuilder("test-index", "localhost", 9090)
			.forDocType("sometype")
			.forDocID("somedoc")
			.includeTypeInUrl(false)
			.refresh(false)
			.build();
			;
		String expURL = "http://localhost:9090/test-index/_doc/sometype:somedoc";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__search_WithScroll() throws Exception {
		URL gotURL = 
				new ESUrlBuilder("test-index", "localhost", 9090)
					.forEndPoint("search")
					.scroll()
					.build();
		;
		String expURL = "http://localhost:9090/test-index/search?scroll=1m";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__scroll() throws Exception {
		URL gotURL = 
				new ESUrlBuilder("test-index", "localhost", 9090)
					.forEndPoint("_search/scroll")
					.build();
		;
		String expURL = "http://localhost:9090/_search/scroll";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}
	
	@Test
	public void test__delete_by_query__HappyPath() throws Exception {
		URL gotURL = 
				new ESUrlBuilder("test-index", "localhost", 9090)
					.forEndPoint("_delete_by_query")
					.build();
		;
		String expURL = "http://localhost:9090/test-index/_delete_by_query";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__settings_endpoint() throws Exception {
		URL gotURL = 
				new ESUrlBuilder("test-index", "localhost", 9090)
					.forEndPoint("_settings")
					.build();
		;
		String expURL = "http://localhost:9090/test-index/_settings";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__update__WithRefresh() throws Exception {
		URL gotURL =
				new ESUrlBuilder("test-index", "localhost", 9090)
					.refresh(true)
					.forDocType("sometype")
					.forDocID("somedoc")
					.forEndPoint("_update")
					.build();
		;
		String expURL = "http://localhost:9090/test-index/sometype/sometype:somedoc/_update?refresh=wait_for";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__update__NoRefresh() throws Exception {
		URL gotURL =
				new ESUrlBuilder("test-index", "localhost", 9090)
						.forDocType("sometype")
						.forDocID("somedoc")
						.forEndPoint("_update")
						.build();
		;
		String expURL = "http://localhost:9090/test-index/sometype/sometype:somedoc/_update";
		AssertString.assertStringEquals(expURL, gotURL.toString());
	}

	@Test
	public void test__installedPlugins() throws Exception {

		URL gotURL =
				new ESUrlBuilder("test-index", "localhost", 9090)
					.cat("plugins").build();
		;
		String expURL = "http://localhost:9090/_cat/plugins?format=json";
		AssertString.assertStringEquals(expURL, gotURL.toString());

	}
}
