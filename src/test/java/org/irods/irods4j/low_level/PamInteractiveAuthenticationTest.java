package org.irods.irods4j.low_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.PamInteractiveAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.ConnectionOptions;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
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
    static RcComm comm;
    static boolean requireSecureConnection = true;

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        XmlUtil.enablePrettyPrinting();
        JsonUtil.enablePrettyPrinting();

        var options = new ConnectionOptions();
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

        XmlUtil.disablePrettyPrinting();
        JsonUtil.disablePrettyPrinting();
    }

    @Test
    void testPamInteractiveAuthentication() throws Exception {
        assumeTrue(false, "Disabled until there are config options for working with a PAM-enabled server");

        try (var conn = new IRODSConnection()) {
            Function<String, String> getInput = prompt -> {
                log.debug("PROMPT (ECHO ON): {}", prompt);
                return "";
            };

            class Counter {
                int count = 0;
            }

            final var counter = new Counter();
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
        assumeTrue(false, "Disabled until there are config options for working with a PAM-enabled server");

        try (var conn = new IRODSConnection()) {
            Function<String, String> getInput = prompt -> {
                log.debug("PROMPT (ECHO ON): {}", prompt);
                return "";
            };

            class Counter {
                int count = 0;
            }

            final var counter = new Counter();
            Function<String, String> getPassword = prompt -> {
                log.debug("PROMPT (ECHO OFF): {}", prompt);
                if (0 == counter.count++) {
                    return password;
                }
                return "incorrect_password";
            };

            conn.connect(host, port, new QualifiedUsername(username, zone));
            var plugin = new PamInteractiveAuthPlugin(requireSecureConnection, getInput, getPassword);
            var e = assertThrows(IRODSException.class, () -> conn.authenticate(plugin, ""));
            assertEquals(IRODSErrorCodes.CAT_INVALID_AUTHENTICATION, e.getErrorCode());
        }
    }

}
