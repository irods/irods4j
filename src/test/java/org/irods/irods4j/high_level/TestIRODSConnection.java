package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers;
import org.irods.irods4j.high_level.administration.IRODSUsers.User;
import org.irods.irods4j.high_level.administration.IRODSUsers.UserType;
import org.irods.irods4j.high_level.administration.IRODSZones.ZoneType;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RodsObjStat_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestIRODSConnection {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testConnectAuthenticateAndDisconnect() throws Exception {
		@SuppressWarnings("resource")
		IRODSConnection conn = new IRODSConnection();
		conn.connect(host, port, new QualifiedUsername(username, zone));
		conn.authenticate("native", password);
		conn.disconnect();
	}

	static IRODSConnection connectUsingTryWithResources() throws Exception {
		try (IRODSConnection conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate("native", password);
			return conn;
		}
	}

	@Test
	void testAutoDisconnect() throws Exception {
		IRODSConnection conn = connectUsingTryWithResources();
		assertNotNull(conn);
		assertFalse(conn.isConnected());
		assertThrows(IllegalStateException.class, () -> conn.getRcComm());
	}

	@Test
	void testReconnectionUsingExistingState() throws Exception {
		try (IRODSConnection conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.disconnect();
			assertFalse(conn.isConnected());

			conn.connect();
			assertTrue(conn.isConnected());
		}
	}

	@Test
	void testConnectingViaAProxyUser() throws Exception {
		try (IRODSConnection adminConn = new IRODSConnection()) {
			adminConn.connect(host, port, new QualifiedUsername(username, zone));
			adminConn.authenticate("native", password);
			assertTrue(adminConn.isConnected());

			// Create a new user. We don't need to set a password for the user because we
			// are authenticating as the admin on behalf of the user.
			User testUser = new User("testuser", Optional.of(zone));
			IRODSUsers.addUser(adminConn.getRcComm(), testUser, UserType.RODSUSER, ZoneType.LOCAL);

			try (IRODSConnection conn = new IRODSConnection()) {
				// Create the proxied connection.
				QualifiedUsername proxyUser = new QualifiedUsername(username, zone);
				QualifiedUsername clientUser = new QualifiedUsername(testUser.name, testUser.zone);
				conn.connect(host, port, proxyUser, clientUser);
				conn.authenticate("native", password);
				assertTrue(conn.isConnected());

				// TODO Stat is a special operation which apparently doesn't care about
				// permissions. It returns the type of the referenced logical path regardless of
				// permissions. May need to create a data object in the admin's home collection.
				//
				// Stat the admin's home collection. This will fail due to the test user not
				// having permission to see the target collection.
				DataObjInp_PI input = new DataObjInp_PI();
				input.objPath = Paths.get("/", zone, "home", username).toString();
				input.KeyValPair_PI = new KeyValPair_PI();
				Reference<RodsObjStat_PI> output = new Reference<RodsObjStat_PI>();
				int ec = IRODSApi.rcObjStat(conn.getRcComm(), input, output);
				log.debug("rcObjStat ec = {}", ec); // TODO Remove this.
				assertEquals(ec, 2);

				// Stat the test user's home collection. This will succeed because the test user
				// has the appropriate permissions for viewing the collection.
				input.objPath = Paths.get("/", zone, "home", testUser.name).toString();
				ec = IRODSApi.rcObjStat(conn.getRcComm(), input, output);
				assertEquals(ec, 2);
			} finally {
				IRODSUsers.removeUser(adminConn.getRcComm(), testUser);
			}
		}
	}

}
