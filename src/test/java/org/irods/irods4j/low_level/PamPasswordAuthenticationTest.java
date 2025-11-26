package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import org.irods.irods4j.authentication.PamPasswordAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class PamPasswordAuthenticationTest {

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "john";
	static String password = "=i;r@o\\d&s";
	static RcComm comm;

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

		try (var conn = new IRODSConnection(options)) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new PamPasswordAuthPlugin(true), password);
		}
	}

}
