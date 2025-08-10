package org.irods.irods4j.high_level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSResources;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectOutputStream;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream;
import org.irods.irods4j.high_level.vfs.CollectionEntry;
import org.irods.irods4j.high_level.vfs.IRODSCollectionIterator;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.high_level.vfs.IRODSReplicas;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IRODSCollectionIteratorTest {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static IRODSConnection conn;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

		conn = new IRODSConnection();
		conn.connect(host, port, new QualifiedUsername(username, zone));
		conn.authenticate(new NativeAuthPlugin(), password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		conn.disconnect();

		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testIteratingOverCollectionHoldingAllHomeCollections() throws Exception {
		List<String> paths = new ArrayList<>();
		String collection = '/' + String.join("/", zone, "home");
		for (CollectionEntry e : new IRODSCollectionIterator(conn.getRcComm(), collection)) {
			paths.add(e.path());
		}
		paths.forEach(log::debug);
		assertFalse(paths.isEmpty());
	}

	@Test
	void testIteratingOverAnEmptyCollection() throws Exception {
		List<String> paths = new ArrayList<>();
		String collection = '/' + String.join("/", zone, "home", "public");
		for (CollectionEntry e : new IRODSCollectionIterator(conn.getRcComm(), collection)) {
			paths.add(e.path());
		}
		assertTrue(paths.isEmpty());
	}

	@Test
	void testConstructingAnIteratorFromADataObjectPathResultsInANullIterator() throws Exception {
		String dataObjectName = "testIteratingUsingADataObjectPathResultsInAnException";
		String path = '/' + String.join("/", zone, "home", username, dataObjectName);

		try {
			// Create the data object.
			try (IRODSDataObjectStream dataObject = new IRODSDataObjectStream()) {
				dataObject.open(conn.getRcComm(), path, OpenFlags.O_CREAT | OpenFlags.O_WRONLY);
			}

			IRODSCollectionIterator collIterator = new IRODSCollectionIterator(conn.getRcComm(), path);
			Iterator<CollectionEntry> iter = collIterator.iterator();
			assertNotNull(iter);
			assertFalse(iter.hasNext());
		} finally {
			// Remove the data object.
			DataObjInp_PI input = new DataObjInp_PI();
			input.objPath = path;
			input.KeyValPair_PI = new KeyValPair_PI();
			input.KeyValPair_PI.ssLen = 1;
			input.KeyValPair_PI.keyWord = new ArrayList<>();
			input.KeyValPair_PI.svalue = new ArrayList<>();
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
			input.KeyValPair_PI.svalue.add("");
			assertEquals(IRODSApi.rcDataObjUnlink(conn.getRcComm(), input), 0);
		}
	}

	@Test
	void testCollectionIteratorDoesNotShowMultipleReplicas() throws Exception {
		String ufs0Resc = "ufs0_resc";
		String ufs1Resc = "ufs1_resc";
		String sandbox = '/' + String.join("/", zone, "home", username, "collection_iterators");

		try {
			// Create two unixfilesystem resources.
			IRODSResources.ResourceRegistrationInfo rescInfo = new IRODSResources.ResourceRegistrationInfo();
			rescInfo.hostName = host;
			rescInfo.resourceName = ufs0Resc;
			rescInfo.resourceType = IRODSResources.ResourceTypes.UNIXFILESYSTEM;
			rescInfo.vaultPath = "/tmp/" + ufs0Resc + "_vault";
			IRODSResources.addResource(conn.getRcComm(), rescInfo);

			rescInfo.resourceName = ufs1Resc;
			rescInfo.vaultPath = "/tmp/" + ufs1Resc + "_vault";
			IRODSResources.addResource(conn.getRcComm(), rescInfo);

			// Reconnect to the server so the agent sees the new resources.
			conn.disconnect();
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new NativeAuthPlugin(), password);

			// Create multiple data objects and replicate them to the new resources.
			IRODSFilesystem.createCollection(conn.getRcComm(), sandbox);
			for (int i = 0; i < 5; ++i) {
				String logicalPath = sandbox + "/data_object." + i;
				try (IRODSDataObjectOutputStream out = new IRODSDataObjectOutputStream(conn.getRcComm(), logicalPath, true, false)) {
					out.write(("unique data for data object #" + i).getBytes(StandardCharsets.UTF_8));
				}
				IRODSReplicas.replicateReplica(conn.getRcComm(), logicalPath, 0, ufs0Resc);
				IRODSReplicas.replicateReplica(conn.getRcComm(), logicalPath, 0, ufs1Resc);

				// Show that each data object has three replicas.
				String query = String.format("select count(DATA_ID) where COLL_NAME = '%s' and DATA_NAME = 'data_object.%d'", sandbox, i);
				List<List<String>> rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
				log.debug("Number of replicas for [{}] is [{}].", logicalPath, rows.get(0).get(0));
				assertEquals(3, Integer.parseInt(rows.get(0).get(0)));
			}

			// Show that the collection iterator avoids presenting duplicate entries.
			Set<String> seen = new HashSet<>();
			for (CollectionEntry e : new IRODSCollectionIterator(conn.getRcComm(), sandbox)) {
				log.debug("id=[{}], path=[{}]", e.id(), e.path());
				assertTrue(seen.add(e.id()));
			}
		}
		finally {
			// Remove all data objects and resources.
			try {
				IRODSFilesystem.removeAll(conn.getRcComm(), sandbox, IRODSFilesystem.RemoveOptions.NO_TRASH);
			} catch (IOException | IRODSException e) {}

			try {
				IRODSResources.removeResource(conn.getRcComm(), ufs0Resc);
			} catch (IOException | IRODSException e) {}

			try {
				IRODSResources.removeResource(conn.getRcComm(), ufs1Resc);
			} catch (IOException | IRODSException e) {}
		}
	}

}
