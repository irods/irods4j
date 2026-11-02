package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.PamPasswordAuthPlugin;
import org.irods.irods4j.authentication.Password;
import org.irods.irods4j.authentication.SessionToken;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class PamPasswordAuthenticationTest {

	private static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "john";
	static String password = "=i;r@o\\d&s";
	static String homeCollection = String.format("/%s/home/%s", zone, username);

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
	void testPamPasswordAuthentication() throws Exception {
		final var runTest = System.getProperty("irods.test.pam_password.enable");
		assumeTrue("1".equals(runTest), "Requires a PAM-enabled iRODS server");

		var options = initConnectionOptionsForTLS();
		var sessionTokenRef = new Reference<String>();

		try (var conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new PamPasswordAuthPlugin(true, sessionTokenRef),
					PamPasswordAuthPlugin.initAuthArguments(new Password(password)));
			assertNotNull(sessionTokenRef.value);
			assertFalse(sessionTokenRef.value.isEmpty());
			log.debug("Session token: {}", sessionTokenRef.value);

			// Test the connection.
			assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), homeCollection));
		}

		//
		// From this point on, the session token provided by the server will be used for
		// authentication. The raw password will not be used.
		//

		// Capture the session token. This is needed to confirm that the next authentication
		// doesn't produce a new session token.
		final var existingSessionToken = sessionTokenRef.value;

		// Show that authentication is possible using the existing session token. No password
		// required.
		try (var conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new PamPasswordAuthPlugin(true, sessionTokenRef),
					PamPasswordAuthPlugin.initAuthArguments(new SessionToken(existingSessionToken)));
			assertEquals(existingSessionToken, sessionTokenRef.value);

			// Test the connection.
			assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), homeCollection));
		}

		// This case is to show that session token usage removes the need to pass the session
		// token reference object to the plugin constructor.
		try (var conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new PamPasswordAuthPlugin(true),
					PamPasswordAuthPlugin.initAuthArguments(new SessionToken(existingSessionToken)));

			// Test the connection.
			assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), homeCollection));
		}
	}

	@Test
	void testIncorrectPasswordResultsInError() throws Exception {
		final var runTest = System.getProperty("irods.test.pam_password.enable");
		assumeTrue("1".equals(runTest), "Requires a PAM-enabled iRODS server");

		var options = initConnectionOptionsForTLS();
		var sessionTokenRef = new Reference<String>();

		// Authenticate using the password to get a valid session token.
		try (var conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new PamPasswordAuthPlugin(true, sessionTokenRef),
					PamPasswordAuthPlugin.initAuthArguments(new Password(password)));
			assertNotNull(sessionTokenRef.value);
			assertFalse(sessionTokenRef.value.isEmpty());
		}

		// Show that passing an invalid session token will result in failure.
		try (var conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			var e = assertThrows(IRODSException.class, () -> conn.authenticate(new PamPasswordAuthPlugin(true),
					PamPasswordAuthPlugin.initAuthArguments(new SessionToken("invalid-token"))));
			assertEquals(IRODSErrorCodes.CAT_INVALID_AUTHENTICATION, e.getErrorCode());
		}
	}

	/**
	 * Returns connection settings that exercise basic support for TLS.
	 * DO NOT use this configuration for production purposes.
	 */
	static IRODSApi.ConnectionOptions initConnectionOptionsForTLS() {
		var options = new IRODSApi.ConnectionOptions();
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
		return options;
	}

}
