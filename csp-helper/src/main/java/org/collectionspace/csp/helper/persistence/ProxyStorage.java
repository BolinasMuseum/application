package org.collectionspace.csp.helper.persistence;

/* Completely transparent proxy for storage. Useful as a temporary fix, to work round corner cases, and to pre-reserve
 * an abstraction.
 * 
 */

import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.json.JSONObject;

public abstract class ProxyStorage implements Storage {
	private Storage proxed;
	
	protected ProxyStorage() {}
	protected ProxyStorage(Storage p) { setTarget(p); }
	protected void setTarget(Storage p) { proxed=p; }
	
	public String autocreateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		return proxed.autocreateJSON(filePath,jsonObject);
	}

	public void createJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		proxed.createJSON(filePath,jsonObject);
	}

	public void deleteJSON(String filePath) throws ExistException,
			UnimplementedException, UnderlyingStorageException {
		proxed.deleteJSON(filePath);
	}

	public JSONObject getPathsJSON(String rootPath,JSONObject restriction)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return proxed.getPathsJSON(rootPath,restriction);
	}

	public String[] getPaths(String rootPath,JSONObject restriction)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return proxed.getPaths(rootPath,restriction);
	}

	public JSONObject retrieveJSON(String filePath)
		throws ExistException, UnimplementedException, UnderlyingStorageException {
		return proxed.retrieveJSON(filePath);
	}

	public void updateJSON(String filePath, JSONObject jsonObject)
			throws ExistException, UnimplementedException, UnderlyingStorageException {
		proxed.updateJSON(filePath,jsonObject);
	}

}
