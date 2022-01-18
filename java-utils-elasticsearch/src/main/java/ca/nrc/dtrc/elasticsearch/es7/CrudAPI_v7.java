package ca.nrc.dtrc.elasticsearch.es7;

import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;

import java.net.URL;

public class CrudAPI_v7 extends CrudAPI {

	public CrudAPI_v7(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	@Override
	protected URL url4putDocument(String type, String id) throws ElasticSearchException {
		URL url =
		urlBuilder()
			.forDocType(type)
			.includeTypeInUrl(false)
			.forDocID(id)
			.refresh(esFactory.updatesWaitForRefresh)
			.build();
		return url;
	}

	@Override
	protected URL url4updateDocument(String esDocType, String docID) throws ElasticSearchException {
		URL url =
			urlBuilder()
				.forDocType(esDocType)
				.forDocID(docID)
				.forEndPoint("_update")
				.includeTypeInUrl(false)
				.refresh(esFactory.updatesWaitForRefresh)
				.build();
		return url;
	}

	@Override
	protected URL url4doc(String esDocType, String docID) throws ElasticSearchException  {
		URL url = urlBuilder().forDocType(esDocType).forDocID(docID)
			.includeTypeInUrl(false).build();
		return url;
	}
}
