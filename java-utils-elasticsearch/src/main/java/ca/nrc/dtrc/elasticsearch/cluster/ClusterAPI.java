package ca.nrc.dtrc.elasticsearch.cluster;

import ca.nrc.dtrc.elasticsearch.DocClusterSet;
import ca.nrc.dtrc.elasticsearch.ESFactory;
import ca.nrc.dtrc.elasticsearch.ES_API;
import ca.nrc.dtrc.elasticsearch.ElasticSearchException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URL;

/** API for creating/reading/updating/deleting ESFactory documents
 */
public abstract class ClusterAPI extends ES_API {
	public ClusterAPI(ESFactory _esFactory) throws ElasticSearchException {
		super(_esFactory);
	}

	public DocClusterSet clusterDocuments(String query, String docTypeName,
		String[] useFields, String algName, Integer maxDocs) throws ElasticSearchException {
		esFactory.engineAPI().ensurePluginInstalled("elasticsearch-carrot2");

		URL url = urlBuilder().forDocType(docTypeName)
		.forEndPoint("_search_with_clusters").build();

		String jsonQuery;
		try {
			jsonQuery = clusterDocumentJsonBody(query, docTypeName, useFields, algName, maxDocs);
		} catch (JsonProcessingException e) {
			throw new ElasticSearchException(e);
		}
		String jsonResponse = transport().post(url, jsonQuery);

		DocClusterSet clusters = parseClusterResponse(jsonResponse, docTypeName);

		return clusters;
	}

	public String clusterDocumentJsonBody(String freeformQuery, String docTypeName, String[] useFields, String algName, Integer maxDocs) throws ElasticSearchException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

		ObjectNode root = nodeFactory.objectNode();
		try {
			ObjectNode searchRequest = nodeFactory.objectNode();
			root.set("search_request", searchRequest);
			{
				ArrayNode source = nodeFactory.arrayNode();
				searchRequest.set("_source", source);
				{
					for (int ii = 0; ii < useFields.length; ii++)
						source.add(useFields[ii]);
				}

				ObjectNode query = nodeFactory.objectNode();
				searchRequest.set("query", query);
				{
					ObjectNode queryString = nodeFactory.objectNode();
					query.set("query_string", queryString);
					{
						queryString.put("query", freeformQuery);
					}
				}
				searchRequest.put("size", maxDocs);
			}
			root.put("query_hint", "");

			root.put("algorithm", algName);


			ObjectNode fieldMapping = nodeFactory.objectNode();
			root.set("field_mapping", fieldMapping);
			{
				ArrayNode content = nodeFactory.arrayNode();
				for (int ii = 0; ii < useFields.length; ii++)
					content.add("_source." + useFields[ii]);
				fieldMapping.set("content", content);
			}
		} catch (Exception exc) {
			throw new ElasticSearchException(exc);
		}

		String jsonBody = mapper.writeValueAsString(root);

		return jsonBody;
	}

	private DocClusterSet parseClusterResponse(String jsonClusterResponse, String docTypeName) throws ElasticSearchException {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode jsonRespNode;
		DocClusterSet clusters = new DocClusterSet(indexName(), docTypeName);
		try {
			jsonRespNode = (ObjectNode) mapper.readTree(jsonClusterResponse);
			ArrayNode clustersNode = (ArrayNode) jsonRespNode.get("clusters");
			for (int ii = 0; ii < clustersNode.size(); ii++) {
				ObjectNode aClusterNode = (ObjectNode) clustersNode.get(ii);
				String clusterName = aClusterNode.get("label").asText();
				ArrayNode documentIDsNode = (ArrayNode) aClusterNode.get("documents");
				for (int jj = 0; jj < documentIDsNode.size(); jj++) {
					String docID = documentIDsNode.get(jj).asText();
					clusters.addToCluster(clusterName, docID);
				}
			}

		} catch (IOException e) {
			throw new ElasticSearchException(e);
		}

		return clusters;
	}
}
