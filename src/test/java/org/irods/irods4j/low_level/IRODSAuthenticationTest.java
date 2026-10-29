package org.irods.irods4j.low_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.AuthPlugin;
import org.irods.irods4j.authentication.IRODSAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.Versioning;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class IRODSAuthenticationTest {

	private static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";

	// This rodsuser is created by the docker-compose.tls.yml project.
	static String username = "alice";
	static String password = "apass";

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		JsonUtil.disablePrettyPrinting();
		XmlUtil.disablePrettyPrinting();
	}

	@Test
	void testIRODSAuthenticationWithTLS() throws Exception {
		final String runTest = System.getProperty("irods.test.tls.enable");
		assumeTrue("1".equals(runTest), "Requires a TLS-enabled iRODS server");

		IRODSApi.ConnectionOptions options = new IRODSApi.ConnectionOptions();
		options.clientServerNegotiation = "CS_NEG_REQUIRE";
		options.trustManagers = new TrustManager[]{new X509TrustManager() {
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

		Reference<String> sessionTokenRef = new Reference<String>();
		AuthPlugin authPlugin = new IRODSAuthPlugin(true, sessionTokenRef);

		try (IRODSConnection conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));

			String serverVersion = conn.getRcComm().relVersion.substring(4);
			boolean authSchemeSupported = Versioning.compareVersions(serverVersion, "5.1.0") >= 0;
			assumeTrue(authSchemeSupported, "Requires server running iRODS 5.1.0 or later");

			conn.authenticate(authPlugin, IRODSAuthPlugin.initAuthArguments(new IRODSAuthPlugin.Password(password)));
		}

		assertNotNull(sessionTokenRef.value);
		assertFalse(sessionTokenRef.value.isEmpty());
		log.debug("Session token = [{}]", sessionTokenRef.value);

		// Show the session token is valid by authenticating with the iRODS server
		// over a new connection.
		try (IRODSConnection conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(authPlugin,
					IRODSAuthPlugin.initAuthArguments(new IRODSAuthPlugin.SessionToken(sessionTokenRef.value)));
		}

		// Show that the server rejects an invalid session token.
		try (IRODSConnection conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			IRODSException e = assertThrows(IRODSException.class, () -> conn.authenticate(authPlugin,
					IRODSAuthPlugin.initAuthArguments(new IRODSAuthPlugin.SessionToken("invalid-token"))));
			assertTrue(e.getErrorCode() < 0);
		}
	}

}
