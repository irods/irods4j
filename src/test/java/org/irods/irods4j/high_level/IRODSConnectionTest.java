package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
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
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RodsObjStat_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class IRODSConnectionTest {

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
		conn.authenticate(new NativeAuthPlugin(), password);
		conn.disconnect();
	}

	static IRODSConnection connectUsingTryWithResources() throws Exception {
		try (IRODSConnection conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new NativeAuthPlugin(), password);
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
			adminConn.authenticate(new NativeAuthPlugin(), password);
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
				conn.authenticate(new NativeAuthPlugin(), password);
				assertTrue(conn.isConnected());

				// Stat the admin's home collection. This will fail due to the test user not
				// having permission to see the target collection.
				DataObjInp_PI  input = new DataObjInp_PI();
				input.objPath = '/' + String.join("/", zone, "home", username);
				input.KeyValPair_PI = new KeyValPair_PI();
				Reference<RodsObjStat_PI> output = new Reference<RodsObjStat_PI>();
				int ec = IRODSApi.rcObjStat(conn.getRcComm(), input, output);
				assertEquals(IRODSErrorCodes.USER_FILE_DOES_NOT_EXIST, ec);

				// Stat the test user's home collection. This will succeed because the test user
				// has the appropriate permissions for viewing the collection.
				input.objPath = '/' + String.join("/", zone, "home", testUser.name);
				ec = IRODSApi.rcObjStat(conn.getRcComm(), input, output);
				assertEquals(2, ec);
			} finally {
				IRODSUsers.removeUser(adminConn.getRcComm(), testUser);
			}
		}
	}

	@Test
	void testTlsWithTrustManagerWhichTrustsAllCertificates() throws Exception {
		final String runTest = System.getProperty("irods.test.tls.enable");
		assumeTrue("1".equals(runTest), "Requires a TLS-enabled iRODS server");

		final IRODSApi.ConnectionOptions connOptions = new IRODSApi.ConnectionOptions();
		connOptions.clientServerNegotiation = "CS_NEG_REQUIRE";

		// This TrustManager makes it so that all certificates are trusted by the client.
		// DO NOT use this configuration in a production environment. It is only meant for
		// testing purposes.
		connOptions.trustManagers = new TrustManager[]{new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		}};

		try (IRODSConnection conn = new IRODSConnection(connOptions)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new NativeAuthPlugin(), password);
			assertTrue(conn.isConnected());
		}
	}

	@Test
	void testTlsWithSelfSignedCertificate() throws Exception {
		final String runTest = System.getProperty("irods.test.tls.enable");
		assumeTrue("1".equals(runTest), "Requires a TLS-enabled iRODS server");

		final X509Certificate tlsCert = loadPemCert(Paths.get("src/test/resources/docker/tls_certs/irods_server.crt"));

		final IRODSApi.ConnectionOptions connOptions = new IRODSApi.ConnectionOptions();
		connOptions.clientServerNegotiation = "CS_NEG_REQUIRE";
		connOptions.trustManagers = new TrustManager[]{new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				for (X509Certificate cert : chain) {
					if (cert.equals(tlsCert)) {
						return;
					}
				}
				throw new CertificateException("Server certificate not trusted.");
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		}};

		try (IRODSConnection conn = new IRODSConnection(connOptions)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
		}
	}

	@Test
	void testTlsFailsOnMismatchCertificateInfo() throws Exception {
		final String runTest = System.getProperty("irods.test.tls.enable");
		assumeTrue("1".equals(runTest), "Requires a TLS-enabled iRODS server");

		final IRODSApi.ConnectionOptions connOptions = new IRODSApi.ConnectionOptions();
		connOptions.clientServerNegotiation = "CS_NEG_REQUIRE";
		connOptions.trustManagers = new TrustManager[]{new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				throw new CertificateException("Server certificate not trusted.");
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}
		}};

		try (IRODSConnection conn = new IRODSConnection(connOptions)) {
			IRODSException ex = assertThrows(IRODSException.class, () -> conn.connect(host, port, new QualifiedUsername(username, zone)));
			assertEquals(IRODSErrorCodes.SYS_LIBRARY_ERROR, ex.getErrorCode());
		}
	}

	@Test
	void testTlsWithJksFile() throws Exception {
		final String runTest = System.getProperty("irods.test.tls.enable");
		assumeTrue("1".equals(runTest), "Requires a TLS-enabled iRODS server");

		final IRODSApi.ConnectionOptions connOptions = new IRODSApi.ConnectionOptions();
		connOptions.clientServerNegotiation = "CS_NEG_REQUIRE";
		connOptions.sslProtocol = "TLSv1.2";
		connOptions.sslTruststore = "src/test/resources/docker/tls_certs/truststore.jks";
		connOptions.sslTruststorePassword = "changeit";

		try (IRODSConnection conn = new IRODSConnection(connOptions)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
		}
	}

	@Test
	void testTlsWithJksFileAndIncorrectPassword() throws Exception {
		final String runTest = System.getProperty("irods.test.tls.enable");
		assumeTrue("1".equals(runTest), "Requires a TLS-enabled iRODS server");

		final IRODSApi.ConnectionOptions connOptions = new IRODSApi.ConnectionOptions();
		connOptions.clientServerNegotiation = "CS_NEG_REQUIRE";
		connOptions.sslProtocol = "TLSv1.2";
		connOptions.sslTruststore = "src/test/resources/docker/tls_certs/truststore.jks";
		connOptions.sslTruststorePassword = "incorrect";

		try (IRODSConnection conn = new IRODSConnection(connOptions)) {
			IRODSException ex = assertThrows(IRODSException.class, () -> conn.connect(host, port, new QualifiedUsername(username, zone)));
			assertEquals(IRODSErrorCodes.SYS_LIBRARY_ERROR, ex.getErrorCode());
		}
	}

	private static X509Certificate loadPemCert(Path pemPath) throws IOException, CertificateException {
		try (InputStream fis = Files.newInputStream(pemPath)) {
			final CertificateFactory cf = CertificateFactory.getInstance("X.509");
			return (X509Certificate) cf.generateCertificate(fis);
		}
	}

}
