package org.collectionspace.chain.csp.persistence.services.relation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedMultipartDocument;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedURL;
import org.collectionspace.chain.csp.persistence.services.connection.ServicesConnection;
import org.collectionspace.chain.csp.persistence.services.vocab.ConfiguredVocabStorage;
import org.collectionspace.chain.util.xtmpl.InvalidXTmplException;
import org.collectionspace.csp.api.core.CSPRequestCache;
import org.collectionspace.csp.api.core.CSPRequestCredentials;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.helper.persistence.ContextualisedStorage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* /relate/main/      POST ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst} ::: id
 * /relate/main/<id>  PUT ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst} :::
 * /relate/main/<id>  DELETE ::: :::
 * /relate/main/<id>  GET ::: ::: {'src': src-type/src, 'type': type, 'dst': dst-type/dst}
 * 
 */

// XXX some hacks here because services don't seem to support multiple search crieteria on relationships. 

public class ServicesRelationStorage implements ContextualisedStorage {
	private static final Logger log=LoggerFactory.getLogger(ServicesRelationStorage.class);
	private ServicesConnection conn;
	private RelationFactory factory;

	private static Set<String> types=new HashSet<String>();
	
	static {
		types.add("affects");
		types.add("new"); // XXX Only one type is bad for testing. remove when there's a second real one
	}
	
	public ServicesRelationStorage(ServicesConnection conn) throws JaxenException, InvalidXTmplException, DocumentException, IOException {
		this.conn=conn;
		factory=new RelationFactory();
	}

	private String[] splitTypeFromId(String path) throws UnderlyingStorageException {
		String[] out=path.split("/");
		if(out.length!=2)
			throw new UnderlyingStorageException("Path must be two components, not "+path);
		return out;
	}
	
	private Relation dataToRelation(CSPRequestCache cache,String id,JSONObject data) throws JSONException, UnderlyingStorageException {
		String[] src=splitTypeFromId(data.getString("src"));
		String[] dst=splitTypeFromId(data.getString("dst"));
		String type=data.getString("type");
		if(!types.contains(type))
			throw new UnderlyingStorageException("type "+type+" is undefined");
		return factory.create(id,src[0],src[1],type,dst[0],dst[1]);
	}

	private JSONObject relationToData(CSPRequestCache cache,Relation r) throws JSONException {
		JSONObject out=new JSONObject();
		out.put("src",r.getSourceType()+"/"+r.getSourceId());
		out.put("dst",r.getDestinationType()+"/"+r.getDestinationId());
		out.put("type",r.getRelationshipType());
		out.put("csid",r.getID());
		return out;
	}

	// XXX refactor
	private String[] extractPaths(String in,String[] prefixes,int var) throws UnderlyingStorageException {
		if(in==null) 
			throw new UnderlyingStorageException("null is not a path");
		if(in.startsWith("/"))
			in=in.substring(1);
		if(in.endsWith("/"))
			in=in.substring(0,in.length()-1);
		String[] split=in.split("/");
		if(split.length!=prefixes.length+var)
			throw new UnderlyingStorageException("Path is incorrect length (should be "+(prefixes.length+var)+" but is "+split.length);
		for(int i=0;i<prefixes.length;i++)
			if(!prefixes[i].equals(split[i]))
				throw new UnderlyingStorageException("Path component "+i+" must be "+prefixes[i]+" but is "+split[i]);
		if(var==0)
			return new String[0];
		String[] ret=new String[var];
		System.arraycopy(split,prefixes.length,ret,0,var);
		return ret;
	}

	public String autocreateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject data)
	throws ExistException,UnimplementedException, UnderlyingStorageException {
		try {
			extractPaths(filePath,new String[]{"main"},0);
			Map<String,Document> in=new HashMap<String,Document>();
			in.put("relations_common",dataToRelation(cache,null,data).toDocument());
			ReturnedURL out=conn.getMultipartURL(RequestMethod.POST,"/relations/",in,creds,cache);
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not add relation status="+out.getStatus());
			return out.getURLTail();
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not add relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve data",e);
		}
	}

	public void createJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath, JSONObject jsonObject)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		throw new UnimplementedException("Cannot create relations to path");
	}

	public void deleteJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			int status=conn.getNone(RequestMethod.DELETE,"/relations/"+parts[0],null,creds,cache);
			if(status>299)
				throw new UnderlyingStorageException("Could not delete relation, status="+status);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not delete relation",e);
		}
	}

	private String searchPath(JSONObject in) throws UnderlyingStorageException, JSONException {
		if(in==null)
			return "";
		StringBuffer out=new StringBuffer();
		boolean xxx_cspace_1080=false;
		if(!xxx_cspace_1080 && in.has("src")) {
			String[] src=splitTypeFromId(in.getString("src"));
			out.append("/subject/"+src[1]);
			xxx_cspace_1080=true;
		}
		if(!xxx_cspace_1080 && in.has("dst")) {
			String[] dst=splitTypeFromId(in.getString("dst"));
			out.append("/object/"+dst[1]);
			xxx_cspace_1080=true;
		}
		if(!xxx_cspace_1080 && in.has("type")) {
			out.append("/type/"+in.getString("type"));
			xxx_cspace_1080=true;
		}
		String ret=out.toString();
		if(ret.startsWith("/"))
			ret=ret.substring(1);
		return ret;
	}
	
	// Needed because of CSPACE-1080
	private boolean post_filter(CSPRequestCredentials creds,CSPRequestCache cache,JSONObject restrictions,Node candidate) throws ExistException, UnderlyingStorageException, ConnectionException, JSONException {
		if(restrictions==null)
			return true;
		// Subject
		String src_csid=candidate.selectSingleNode("subjectCsid").getText();
		String rest_src=restrictions.optString("src");
		if(rest_src!=null && !"".equals(rest_src)) {
			if(!src_csid.equals(rest_src.split("/")[1]))
				return false;
		}
		String dst_csid=candidate.selectSingleNode("objectCsid").getText();		
		String rest_dst=restrictions.optString("dst");
		if(rest_dst!=null && !"".equals(rest_dst)) {
			if(!dst_csid.equals(rest_dst.split("/")[1]))
				return false;
		}
		// Retrieve the relation (CSPACE-1081)
		ReturnedMultipartDocument rel=conn.getMultipartXMLDocument(RequestMethod.GET,candidate.selectSingleNode("uri").getText(),null,creds,cache);
		if(rel.getStatus()==404)
			throw new ExistException("Not found");
		Document rel_doc=rel.getDocument("relations_common");
		if(rel_doc==null)
			throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
		String type=rel_doc.selectSingleNode("relations_common/relationshipType").getText();
		if(restrictions.has("type") && !type.equals(restrictions.optString("type")))
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getPathsJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		JSONObject out = new JSONObject();
		extractPaths(rootPath,new String[]{"main"},0);
		try {
			List<String> list=new ArrayList<String>();
			ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/relations/"+searchPath(restrictions),null,creds,cache);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
			JSONObject pagination = new JSONObject();
			String xmlroot = "relations-common-list";
			List<Node> nodes=doc.getDocument().selectNodes("/"+xmlroot+"/*");
			for(Node node : nodes){
				if(node.matches("/"+xmlroot+"/relation-list-item")){
					if(post_filter(creds,cache,restrictions,node))
						list.add(node.selectSingleNode("csid").getText());
				}else{
					pagination.put(node.getName(), node.getText());
				}
			}
			
			out.put("pagination", pagination);
			out.put("listItems",list.toArray(new String[0]));
			return out;
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		}
	}

	@SuppressWarnings("unchecked")
	public String[] getPaths(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String rootPath,JSONObject restrictions)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		extractPaths(rootPath,new String[]{"main"},0);
		try {
			List<String> out=new ArrayList<String>();
			ReturnedDocument data=conn.getXMLDocument(RequestMethod.GET,"/relations/"+searchPath(restrictions),null,creds,cache);
			Document doc=data.getDocument();
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
			List<Node> objects=doc.getDocument().selectNodes("relations-common-list/relation-list-item");
			for(Node object : objects) {
				if(post_filter(creds,cache,restrictions,object))
					out.add(object.selectSingleNode("csid").getText());
			}
			return out.toArray(new String[0]);
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		}
	}

	public JSONObject retrieveJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath)
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.GET,"/relations/"+parts[0],null,creds,cache);
			if(out.getStatus()==404)
				throw new ExistException("Not found");
			Document doc=out.getDocument("relations_common");
			if(doc==null)
				throw new UnderlyingStorageException("Could not retrieve relation, missing relations_common");
			return relationToData(cache,factory.load(parts[0],doc));
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		} catch (JaxenException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve relation",e);
		}
	}

	public void updateJSON(ContextualisedStorage root,CSPRequestCredentials creds,CSPRequestCache cache, String filePath,JSONObject data) 
	throws ExistException, UnimplementedException, UnderlyingStorageException {
		try {
			String[] parts=extractPaths(filePath,new String[]{"main"},1);
			Map<String,Document> in=new HashMap<String,Document>();
			in.put("relations_common",dataToRelation(cache,parts[0],data).toDocument());
			ReturnedMultipartDocument out=conn.getMultipartXMLDocument(RequestMethod.PUT,"/relations/"+parts[0],in,creds,cache);
			if(out.getStatus()==404)
				throw new ExistException("Not found");
			if(out.getStatus()>299)
				throw new UnderlyingStorageException("Could not update relation, status="+out.getStatus());
		} catch (ConnectionException e) {
			throw new UnderlyingStorageException("Could not update relation",e);
		} catch (JSONException e) {
			throw new UnderlyingStorageException("Could not retrieve data",e);
		}
	}
}
