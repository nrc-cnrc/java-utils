package ca.nrc.dtrc.elasticsearch;

import ca.nrc.dtrc.elasticsearch.cluster.ClusterAPI;
import ca.nrc.dtrc.elasticsearch.crud.CrudAPI;
import ca.nrc.dtrc.elasticsearch.engine.EngineAPI;
import ca.nrc.dtrc.elasticsearch.index.IndexAPI;
import ca.nrc.dtrc.elasticsearch.search.SearchAPI;
import ca.nrc.ui.commandline.UserIO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Factory for making objects needed to interact with ElasticSearch
 */
public abstract class ESFactory {

	public static enum ESOptions {
		CREATE_IF_NOT_EXISTS, UPDATES_WAIT_FOR_REFRESH, VERBOSE, APPEND
	};

	public abstract int version();
	public abstract StreamlinedClient client() throws ElasticSearchException;
	public abstract IndexAPI indexAPI() throws ElasticSearchException;
	public abstract CrudAPI crudAPI() throws ElasticSearchException;
	public abstract SearchAPI searchAPI() throws ElasticSearchException;
	public abstract ClusterAPI clusterAPI() throws ElasticSearchException;

	public String indexName = null;

	private String serverName = "localhost";
	public int port = 9200;

	/**
	 * Note: As of 2020-01, we have noticed that when several StreamlinedClient_v5
	 * 	are used concurrently in different threads, it ends up creating
	 *    documents whose JSON structure does not correspond to the structure
	 *    of a document.
	 *
	 *    If you find that to be the case, then configure your StreamlinedClients
	 *    with syncHttpCalls = true. Note that this may significantly slow down
	 *    the operation of the various StreamlinedClients.
	 *
	 *    This will cause the client to invoke the Http client throug the
	 *    synchronized method httpCall_sync below.
	 */
	public boolean synchedHttpCalls = true;


	@JsonIgnore
	UserIO userIO = new UserIO();

	protected ResponseMapper respMapper = new ResponseMapper((String)null);

	public boolean updatesWaitForRefresh = false;
	public boolean createIndexIfNotExist = false;
	public ErrorHandlingPolicy _errorPolicy = ErrorHandlingPolicy.STRICT;

	// Whenever the client issues a transaction that modifies the DB,
	// it will sleep by that much to give ESFactory time to update all the
	// nodes and shards.
	public double sleepSecs = 0.0;

	public List<ESObserver> observers = new ArrayList<ESObserver>();

	public ESFactory() {
	}

	public ESFactory(String _indexName) throws ElasticSearchException {
		init_ESFactory(_indexName);
	}

	private void init_ESFactory(String _indexName) throws ElasticSearchException {
		init_ESFactory(_indexName, (Integer)null, new ESOptions[0]);
	}

	private void init_ESFactory(String _indexName, Integer _port) throws ElasticSearchException {
		init_ESFactory(_indexName, _port, new ESOptions[0]);
	}

	private void init_ESFactory(String _indexName, Integer _port,
		ESOptions... options) throws ElasticSearchException {
		if (_indexName == null) {
			throw new ElasticSearchException("Index name must not be null");
		}
		indexName = _indexName;
		if (_port != null) {
			port = _port;
		}

		new ESVersionChecker(serverName, port)
			.isRunningVersion(version());

		if (ArrayUtils.contains(options, ESOptions.UPDATES_WAIT_FOR_REFRESH)) {
			updatesWaitForRefresh = true;
		}

		if (ArrayUtils.contains(options, ESOptions.CREATE_IF_NOT_EXISTS)) {
			createIndexIfNotExist = true;
		}
	}

	public ESFactory setIndexName(String name) {
		indexName = name;
		return this;
	}

	public ESFactory setServer(String _server) {
		serverName = _server;
		return this;
	}

	public ESFactory setPort(int _port) {
		port = _port;
		return this;
	}

	public ESFactory setSleepSecs(double secs) {
		sleepSecs = secs;
		return this;
	}

	public ESFactory setErrorPolicy(ErrorHandlingPolicy policy) {
		if (policy != null) {
			_errorPolicy = policy;
			respMapper.onBadRecord = policy;
		}
		return this;
	}

	public ErrorHandlingPolicy getErrorPolicy() {
		return _errorPolicy;
	}

	public ESFactory setSynchedHttpCalls(boolean synched) {
		synchedHttpCalls = synched;
		return this;
	}

	@JsonIgnore
	List<ESObserver> getObservers() {
		return observers;
	}

	public void attachObservers(Collection<ESObserver> _observers) {
		for (ESObserver anObserver: _observers) {
			attachObserver(anObserver);
		}
	}

	public void attachObserver(ESObserver _obs) {
		_obs.setObservedIndex(this.indexName);
		observers.add(_obs);
	}

	public void detachObservers() {
		observers = new ArrayList<ESObserver>();
	}

	public ESUrlBuilder urlBuilder() {
		return new ESUrlBuilder(this.indexName, "localhost", port);
	}

	public ESFactory setUserIO(UserIO _userIO) {
		this.userIO = _userIO;
		return this;
	}

	public UserIO getUserIO() {
		return this.userIO;
	}

	public void echo(String message, UserIO.Verbosity level) {
		if (this.userIO != null) userIO.echo(message, level);
	}

	public EngineAPI engineAPI() throws ElasticSearchException {
		return new EngineAPI(this);
	}


	public StreamlinedClient client(double sleepSecs) throws ElasticSearchException {
		StreamlinedClient _client = client();
		_client.setSleepSecs(sleepSecs);
		return _client;
	}

	public Transport transport() {
		Transport transp = new Transport(indexName, observers);
		transp.synchedHttpCalls = synchedHttpCalls;
		return transp;
	}

	public void sleep() {
		try {
			sleep(this.sleepSecs);
		} catch (InterruptedException e) {
			System.exit(0);
		}
	}

	public void sleep(double secs) throws InterruptedException {
		int millis = (int) (1000 * secs);
		Thread.sleep(millis);
	}

}
