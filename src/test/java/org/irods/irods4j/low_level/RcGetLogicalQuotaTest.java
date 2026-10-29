package org.irods.irods4j.low_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.Versioning;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.vfs.LogicalPath;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.protocol.packing_instructions.GeneralAdminInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GetLogicalQuotaInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GridConfigurationInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.LogicalQuotaList_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class RcGetLogicalQuotaTest {

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

		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new NativeAuthPlugin(), NativeAuthPlugin.initAuthArguments(password, 0L));

			var serverVersion = conn.getRcComm().relVersion.substring(4);
			var authSchemeSupported = Versioning.compareVersions(serverVersion, "5.1.0") >= 0;
			assumeTrue(authSchemeSupported, "Requires server running iRODS 5.1.0 or later");

			var input = new GridConfigurationInp_PI();
			input.name_space = "logical_quotas";
			input.option_name = "enabled";
			input.option_value = "1";
			assertEquals(0, IRODSApi.rcSetGridConfigurationValue(conn.getRcComm(), input));
		}
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new NativeAuthPlugin(), NativeAuthPlugin.initAuthArguments(password, 0L));

			var serverVersion = conn.getRcComm().relVersion.substring(4);
			var authSchemeSupported = Versioning.compareVersions(serverVersion, "5.1.0") >= 0;
			assumeTrue(authSchemeSupported, "Requires server running iRODS 5.1.0 or later");

			var input = new GridConfigurationInp_PI();
			input.name_space = "logical_quotas";
			input.option_name = "enabled";
			input.option_value = "0";
			assertEquals(0, IRODSApi.rcSetGridConfigurationValue(conn.getRcComm(), input));
		}

		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testRcGetLogicalQuotaReturnsQuotaInfoAboutMonitoredCollections() throws Exception {
		var collection = String.format("/%s/home/%s", zone, username);
		final var collectionMaxBytes = "10";
		final var collectionMaxObjects = "20";

		var parentCollection = LogicalPath.parentPath(collection);
		final var parentCollectionMaxBytes = "30";
		final var parentCollectionMaxObjects = "40";

		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new NativeAuthPlugin(), NativeAuthPlugin.initAuthArguments(password, 0L));

			var serverVersion = conn.getRcComm().relVersion.substring(4);
			var authSchemeSupported = Versioning.compareVersions(serverVersion, "5.1.0") >= 0;
			assumeTrue(authSchemeSupported, "Requires server running iRODS 5.1.0 or later");

			try {
				// Set a quota on two collections.
				// This helps in guaranteeing the library properly deserializes
				// output from the server containing more than one entry.
				var setQuotaInput = new GeneralAdminInp_PI();
				setQuotaInput.arg0 = "set_logical_quota";
				setQuotaInput.arg1 = collection;
				setQuotaInput.arg2 = collectionMaxBytes;
				setQuotaInput.arg3 = collectionMaxObjects;
				assertEquals(0, IRODSApi.rcGeneralAdmin(conn.getRcComm(), setQuotaInput));

				setQuotaInput.arg0 = "set_logical_quota";
				setQuotaInput.arg1 = parentCollection;
				setQuotaInput.arg2 = parentCollectionMaxBytes;
				setQuotaInput.arg3 = parentCollectionMaxObjects;
				assertEquals(0, IRODSApi.rcGeneralAdmin(conn.getRcComm(), setQuotaInput));

				// Get the logical quota information for all collections in the path.
				var input = new GetLogicalQuotaInp_PI();
				input.collName = collection;
				input.KeyValPair_PI = new KeyValPair_PI();
				input.KeyValPair_PI.ssLen = 0;

				var output = new Reference<LogicalQuotaList_PI>();
				assertEquals(0, IRODSApi.rcGetLogicalQuota(conn.getRcComm(), input, output));
				assertNotNull(output.value);
				assertEquals(2, output.value.len);
				assertNotNull(output.value.logicalQuota_PI);
				assertFalse(output.value.logicalQuota_PI.isEmpty());

				output.value.logicalQuota_PI.forEach(e -> {
					log.debug("---");
					log.debug("coll name    = [{}]", e.collName);
					log.debug("max bytes    = [{}]", e.maxBytes);
					log.debug("max objects  = [{}]", e.maxObjects);
					log.debug("over bytes   = [{}]", e.overBytes);
					log.debug("over objects = [{}]", e.overObjects);
				});

				assertTrue(output.value.logicalQuota_PI.stream().anyMatch(e -> {
					var bytes = Long.parseLong(collectionMaxBytes);
					var objects = Long.parseLong(collectionMaxObjects);
					return collection.equals(e.collName) && bytes == e.maxBytes && objects == e.maxObjects &&
						   -bytes == e.overBytes && -objects == e.overObjects && e.modify_time.length() == 11;
				}));

				assertTrue(output.value.logicalQuota_PI.stream().anyMatch(e -> {
					var bytes = Long.parseLong(parentCollectionMaxBytes);
					var objects = Long.parseLong(parentCollectionMaxObjects);
					return parentCollection.equals(e.collName) && bytes == e.maxBytes && objects == e.maxObjects &&
						   -bytes == e.overBytes && -objects == e.overObjects && e.modify_time.length() == 11;
				}));
			} finally {
				// Remove quotas from collections.
				var setQuotaInput = new GeneralAdminInp_PI();
				setQuotaInput.arg0 = "set_logical_quota";
				setQuotaInput.arg1 = collection;
				setQuotaInput.arg2 = "0";
				setQuotaInput.arg3 = "0";
				var ec = IRODSApi.rcGeneralAdmin(conn.getRcComm(), setQuotaInput);
				log.info("rcGeneralAdmin error code = [{}]", ec);

				setQuotaInput.arg0 = "set_logical_quota";
				setQuotaInput.arg1 = parentCollection;
				setQuotaInput.arg2 = "0";
				setQuotaInput.arg3 = "0";
				ec = IRODSApi.rcGeneralAdmin(conn.getRcComm(), setQuotaInput);
				log.info("rcGeneralAdmin error code = [{}]", ec);
			}
		}
	}

	@Test
	void testRcGetLogicalQuotaReturnsNoInfoAboutUnmonitoredCollection() throws Exception {
		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(username, zone));
			conn.authenticate(new NativeAuthPlugin(), NativeAuthPlugin.initAuthArguments(password, 0L));

			var serverVersion = conn.getRcComm().relVersion.substring(4);
			var authSchemeSupported = Versioning.compareVersions(serverVersion, "5.1.0") >= 0;
			assumeTrue(authSchemeSupported, "Requires server running iRODS 5.1.0 or later");

			var input = new GetLogicalQuotaInp_PI();
			input.collName = String.format("/%s/home/%s", zone, username);
			input.KeyValPair_PI = new KeyValPair_PI();
			input.KeyValPair_PI.ssLen = 0;

			var output = new Reference<LogicalQuotaList_PI>();
			assertEquals(0, IRODSApi.rcGetLogicalQuota(conn.getRcComm(), input, output));
			assertNotNull(output.value);
			assertEquals(0, output.value.len);
			assertNull(output.value.logicalQuota_PI);
		}
	}

}
