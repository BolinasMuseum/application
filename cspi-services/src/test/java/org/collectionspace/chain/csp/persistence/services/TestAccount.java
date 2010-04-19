package org.collectionspace.chain.csp.persistence.services;

import static org.junit.Assert.*;

import org.collectionspace.bconfigutils.bootstrap.BootstrapConfigLoadFailedException;
import org.collectionspace.chain.csp.persistence.services.connection.ConnectionException;
import org.collectionspace.chain.csp.persistence.services.connection.RequestMethod;
import org.collectionspace.chain.csp.persistence.services.connection.ReturnedDocument;
import org.collectionspace.csp.api.persistence.Storage;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAccount extends ServicesBaseClass {
	private static final Logger log=LoggerFactory.getLogger(TestAccount.class);
	@Before public void checkServicesRunning() throws ConnectionException, BootstrapConfigLoadFailedException {
		setup();
	}
	
	@Test public void testAccountCreate() throws Exception {
		Storage ss=makeServicesStorage(base+"/cspace-services/");
		/* delete user so we can create it later - will return 404 if user doesn't exist */
		String[] paths=ss.getPaths("users/",new JSONObject("{\"userId\":\"test1@collectionspace.org\"}"));
		if(paths.length>0)
			ss.deleteJSON("users/"+paths[0]);
		
		JSONObject u1=getJSON("user1.json");
		/* create the user based on json */
		/* will give a hidden 500 error if userid is not unique (useful eh?) */
		String path=ss.autocreateJSON("users/",u1);
		log.info("path="+path);
		assertNotNull(path);
		JSONObject u2=getJSON("user1.json");
		ss.updateJSON("users/"+path,u2);
		JSONObject u3=ss.retrieveJSON("users/"+path);
		assertNotNull(u3);
		log.info("JSONOBJ",u3);
		// Check output
		assertEquals("Test Mccollectionspace.org",u3.getString("screenName"));
		assertEquals("test@collectionspace.org",u3.getString("userId"));
		assertEquals("test@collectionspace.org",u3.getString("email"));
		assertEquals("active",u3.getString("status"));
		// Check the method we're about to use to check if login works works
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test@collectionspace.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"blahblah");
		cache.reset();
		ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
		assertFalse(out.getStatus()==200);		
		// Check login works
		creds.setCredential(ServicesStorageGenerator.CRED_USERID,"test@collectionspace.org");
		creds.setCredential(ServicesStorageGenerator.CRED_PASSWORD,"testtestt");
		cache.reset();
		out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects",null,creds,cache);
		log.info("Status",out.getStatus());
		assertTrue(out.getStatus()==200);
		//
		ss.deleteJSON("users/"+path);
		/* tidy up and delete user */
	}
}