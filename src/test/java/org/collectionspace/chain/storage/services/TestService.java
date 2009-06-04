package org.collectionspace.chain.storage.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.collectionspace.chain.storage.services.ReturnedDocument;
import org.collectionspace.chain.storage.services.ReturnedURL;
import org.collectionspace.chain.storage.services.ServicesConnection;
import org.collectionspace.chain.util.BadRequestException;
import org.collectionspace.chain.util.RequestMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class TestService {
	private static String BASE_URL="http://chalk-233:8080"; // XXX configure
	private ServicesConnection conn;
	private Random rnd=new Random();
	
	private InputStream getResource(String name) {
		String path=getClass().getPackage().getName().replaceAll("\\.","/")+"/"+name;
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
	}
	
	private Document getDocument(String name) throws DocumentException {
		SAXReader reader=new SAXReader();
		// TODO errorhandling
		return reader.read(getResource(name));
	}
	
	@Before public void checkServicesRunning() throws BadRequestException {
		try {
			conn=new ServicesConnection(BASE_URL+"/helloworld/cspace-nuxeo/");
			ReturnedDocument out=conn.getXMLDocument(RequestMethod.GET,"collectionobjects");
			Assume.assumeTrue(out.getStatus()==200);
		} catch(BadRequestException e) {
			Assume.assumeTrue(false);
		}
	}
	
	@Test public void testAssumptionMechanism() {
		System.err.println("Services Running!");
	}

	@Test public void testObjectsPost() throws Exception {
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",getDocument("obj1.xml"));
		assertEquals(201,url.getStatus());
		System.err.println("got "+url.getURL());
		assertTrue(url.getURL().startsWith("/collectionobjects/"));		
		ReturnedDocument doc=conn.getXMLDocument(RequestMethod.GET,url.getURL());
		assertEquals(200,doc.getStatus());
		String num=doc.getDocument().selectSingleNode("collection-object/objectNumber").getText();
		assertEquals("2",num);
	}
	
	// TODO pre-emptive cache population
	
	// Speeds up many tests, ensures others work at all
	@SuppressWarnings("unchecked")
	private void deleteAll() throws Exception {
		ReturnedDocument all=conn.getXMLDocument(RequestMethod.GET,"collectionobjects/");
		if(all.getStatus()!=200)
			throw new BadRequestException("Bad request during identifier cache map update: status not 200");
		List<Node> objects=all.getDocument().selectNodes("collection-object-list/collection-object-list-item");
		for(Node object : objects) {
			String csid=object.selectSingleNode("csid").getText();
			conn.getNone(RequestMethod.DELETE,"collectionobjects/"+csid,null);
		}
	}
	
	@Test public void testSetvicesIdentifierMapBasic() throws Exception {
		deleteAll(); // for speed
		ServicesIdentifierMap sim=new ServicesIdentifierMap(conn);
		InputStream data_stream=getResource("obj2.xml");
		String data=IOUtils.toString(data_stream);
		data_stream.close();
		String objid="test-sim-"+rnd.nextInt(Integer.MAX_VALUE);
		data=data.replaceAll("<<objnum>>",objid);
		SAXReader reader=new SAXReader();
		Document doc=reader.read(new StringReader(data));
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",doc);
		assertEquals(201,url.getStatus());
		String csid=url.getURL().substring(url.getURL().lastIndexOf("/")+1);
		String csid2=sim.getCSID(objid);
		assertEquals(csid,csid2);
	}
	
	@Test public void testDelete() throws Exception {
		ReturnedURL url=conn.getURL(RequestMethod.POST,"collectionobjects/",getDocument("obj1.xml"));
		assertEquals(201,url.getStatus());
		ReturnedDocument doc1=conn.getXMLDocument(RequestMethod.GET,url.getURL());
		assertEquals(200,doc1.getStatus());		
		int status=conn.getNone(RequestMethod.DELETE,url.getURL(),null);
		assertEquals(204,status); // CSPACE-73, should be 404
		ReturnedDocument doc2=conn.getXMLDocument(RequestMethod.GET,url.getURL());
		assertEquals(200,doc2.getStatus());	 // CSPACE-209, should be 404
		assertEquals(0,doc2.getDocument().selectNodes("collection-object/*").size());
	}
}
