package ca.nrc.dtrc.elasticsearch.es5;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;

import java.net.URL;

public class CrudAPI_v5 extends CrudAPI {

	@Override
	protected URL url4putDocument(String type, String id) throws ElasticSearchException {
		URL url =
			urlBuilder()
				.forDocType(type)
				.forDocID(id)
				.refresh(esFactory.updatesWaitForRefresh)
				.build();
		return url;
	}

	@Override
	protected URL url4doc(String esDocType, String docID) throws ElasticSearchException  {
		URL url = urlBuilder().forDocType(esDocType).forDocID(docID).build();
		return url;
	}

	@Override
	protected URL url4updateDocument(String esDocType, String docID) throws ElasticSearchException {
		URL url =
			urlBuilder()
				.forDocType(esDocType)
				.forDocID(docID)
				.forEndPoint("_update")
				.refresh(esFactory.updatesWaitForRefresh)
				.build();
		return url;
	}

	public CrudAPI_v5(ESFactory _esAPI) throws ElasticSearchException {
		super(_esAPI);
	}
}
