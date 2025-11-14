package org.irods.irods4j.low_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.PamInteractiveAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PamInteractiveAuthenticationTest {

    static final Logger log = LogManager.getLogger();

    static String host = "localhost";
    static int port = 1247;
    static String zone = "tempZone";
    static String username = "john";
    static String password = "=i;r@o\\d&s";
    static boolean requireSecureConnection = true;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        XmlUtil.enablePrettyPrinting();
        JsonUtil.enablePrettyPrinting();

//      ConnectionOptions options = new ConnectionOptions();
//		options.clientServerNegotiation = "CS_NEG_REQUIRE";
//		options.sslProtocol = "TLSv1.2";
//		options.sslTruststore = "/home/kory/eclipse-workspace/irods4j/truststore.jks";
//		options.sslTruststorePassword = "changeit";
    }

    @AfterAll
    static void tearDownAfterClass() throws Exception {
        XmlUtil.disablePrettyPrinting();
        JsonUtil.disablePrettyPrinting();
    }

    @Test
    void testPamInteractiveAuthentication() throws Exception {
        final String runTest = System.getProperty("irods.test.pam_interactive.enable");
        assumeTrue("1".equals(runTest), "Requires a PAM-enabled iRODS server");

        final IRODSApi.ConnectionOptions connOptions = new IRODSApi.ConnectionOptions();
        connOptions.clientServerNegotiation = "CS_NEG_REQUIRE";
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
            Function<String, String> getInput = prompt -> {
                log.debug("PROMPT (ECHO ON): {}", prompt);
                return "";
            };

            class Counter {
                int count = 0;
            }

            final Counter counter = new Counter();
            Function<String, String> getPassword = prompt -> {
                log.debug("PROMPT (ECHO OFF): {}", prompt);
                if (0 == counter.count++) {
                    return password;
                }
                return "otherrods";
            };

            conn.connect(host, port, new QualifiedUsername(username, zone));
            conn.authenticate(new PamInteractiveAuthPlugin(requireSecureConnection, getInput, getPassword), "");
        }
    }

    @Test
    void testIncorrectPasswordResultsInError() throws Exception {
        final String runTest = System.getProperty("irods.test.pam_interactive.enable");
        assumeTrue("1".equals(runTest), "Requires a PAM-enabled iRODS server");

        final IRODSApi.ConnectionOptions connOptions = new IRODSApi.ConnectionOptions();
        connOptions.clientServerNegotiation = "CS_NEG_REQUIRE";
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
            Function<String, String> getInput = prompt -> {
                log.debug("PROMPT (ECHO ON): {}", prompt);
                return "";
            };

            class Counter {
                int count = 0;
            }

            final Counter counter = new Counter();
            Function<String, String> getPassword = prompt -> {
                log.debug("PROMPT (ECHO OFF): {}", prompt);
                if (0 == counter.count++) {
                    return password;
                }
                return "incorrect_password";
            };

            conn.connect(host, port, new QualifiedUsername(username, zone));
            PamInteractiveAuthPlugin plugin = new PamInteractiveAuthPlugin(requireSecureConnection, getInput, getPassword);
            IRODSException e = assertThrows(IRODSException.class, () -> conn.authenticate(plugin, ""));
            assertEquals(IRODSErrorCodes.CAT_INVALID_AUTHENTICATION, e.getErrorCode());
        }
    }

}
