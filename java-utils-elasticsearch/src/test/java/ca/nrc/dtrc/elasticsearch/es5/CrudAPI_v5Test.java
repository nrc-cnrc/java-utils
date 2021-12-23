package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ESTestHelpers;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPITest;
import org.junit.jupiter.api.BeforeAll;

public class CrudAPI_v5Test extends CrudAPITest {
	@BeforeAll
	public static void beforeAll() throws Exception {
		new ESTestHelpers(5).skipTestsUnlessESIsRunning(9207);
		return;
	}


	@Override
	protected ESFactory makeES(String indexName) throws Exception {
		return new ES5Factory(indexName);
	}

	@Override
	protected int esVersion() {
		return 5;
	}

	@Override
	protected String expUrl4Doc(String type, String docID) {
		return "http://localhost:9205/es-test/"+type+"/"+type+":"+docID;
	}

	@Override
	protected String expUrl4updateDoc(String type, String docID) {
		return "http://localhost:9205/es-test/sometype/sometype:somedoc/_update?refresh=wait_for";
	}
}
