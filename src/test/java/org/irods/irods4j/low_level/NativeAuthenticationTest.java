package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.authentication.Password;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers;
import org.irods.irods4j.high_level.administration.IRODSZones;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NativeAuthenticationTest {

	private static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static Password password = new Password("rods");
	static RcComm comm;
	static String homeCollection = String.format("/%s/home/%s", zone, username);

	@BeforeEach
	void setUpBeforeEachTest() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();
		comm = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty());
		assertNotNull(comm);
	}

	@AfterEach
	void tearDownAfterEachTest() throws Exception {
		IRODSApi.rcDisconnect(comm);
	}

	@Test
	void testNativeAuthentication() throws Exception {
		Map<String, Object> input = NativeAuthPlugin.initAuthArguments(password, 0L);
		IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), input);
		assertTrue(IRODSFilesystem.isCollection(comm, homeCollection)); // Test the connection.
	}

	@Test
	void testAuthenticateAsAnonymousUser() throws Exception {
		Map<String, Object> input = NativeAuthPlugin.initAuthArguments(password, 0L);
		IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), input);

		// Add the anonymous user to the system if not present.
		IRODSUsers.User anonymousUser = new IRODSUsers.User("anonymous", Optional.of(zone));

		boolean removeAnonymousUser = false;
		try {
			if (!IRODSUsers.exists(comm, anonymousUser)) {
				removeAnonymousUser = true;
				IRODSUsers.addUser(comm, anonymousUser, IRODSUsers.UserType.RODSUSER, IRODSZones.ZoneType.LOCAL);
			}

			RcComm anonymousComm = IRODSApi.rcConnect(host, port, "anonymous", zone, Optional.empty(), Optional.empty(),
					Optional.empty(), Optional.empty());
			assertNotNull(anonymousComm);
			try {
				IRODSApi.rcAuthenticateClient(anonymousComm, new NativeAuthPlugin(), new HashMap<>());
				// Test the connection.
				List<List<String>> rows = IRODSQuery.executeGenQuery2(anonymousComm, "select ZONE_NAME");
				assertNotNull(rows);
				assertFalse(rows.isEmpty());
			} finally {
				IRODSApi.rcDisconnect(anonymousComm);
			}
		} finally {
			if (removeAnonymousUser) {
				IRODSUsers.removeUser(comm, anonymousUser);
			}
		}
	}

	@Test
	void testSupportForTimeLimitedPasswords() throws Exception {
		// Authenticate with a positive TTL value.
		Reference<String> ttlPasswordRef = new Reference<>();
		IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(ttlPasswordRef),
				NativeAuthPlugin.initAuthArguments(password, 1L));
		assertNotNull(ttlPasswordRef.value);
		assertFalse(ttlPasswordRef.value.isEmpty());
		log.debug("Time-limited password: {}", ttlPasswordRef.value);

		// Use a new connection to prove the time-limited password can be used
		// for authenticating as the same user.
		RcComm comm1 = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(),
				Optional.empty(),
				Optional.empty());
		assertNotNull(comm1);
		try {
			IRODSApi.rcAuthenticateClient(comm1, new NativeAuthPlugin(),
					NativeAuthPlugin.initAuthArguments(new Password(ttlPasswordRef.value)));
			assertTrue(IRODSFilesystem.isCollection(comm1, homeCollection)); // Test the connection.
		} finally {
			IRODSApi.rcDisconnect(comm1);
		}
	}

}
