package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Optional;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.ConnectionOptions;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

		ConnectionOptions options = new ConnectionOptions();
//		options.clientServerNegotiation = "CS_NEG_REQUIRE";
//		options.sslProtocol = "TLSv1.2";
//		options.sslTruststore = "/home/kory/eclipse-workspace/irods4j/truststore.jks";
//		options.sslTruststorePassword = "changeit";
		comm = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.of(options),
				Optional.empty());
		assertNotNull(comm);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		IRODSApi.rcDisconnect(comm);
	}

	@Test
	void testPamPasswordAuthentication() {
		assumeTrue(false, "Disabled until there are config options for working with a PAM-enabled server");
		assertDoesNotThrow(() -> IRODSApi.rcAuthenticateClient(comm, "pam_password", password));
	}

}
