package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPITest;
import org.junit.jupiter.api.BeforeAll;

public class CrudAPI_v7Test extends CrudAPITest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(7).skipTestsUnlessESIsRunning(9207);
		return;
	}

	@Override
	protected ESFactory makeES(String indexName) throws Exception {
		return new ES7Factory(indexName);
	}

	@Override
	protected int esVersion() {
		return 7;
	}

	@Override
	protected String expUrl4Doc(String type, String docID) {
		return "http://localhost:9207/es-test/_doc/"+type+":"+docID;
	}

	@Override
	protected String expUrl4updateDoc(String type, String docID) {
		return "http://localhost:9207/es-test/_doc/sometype:somedoc/_update?refresh=wait_for";
	}
}
