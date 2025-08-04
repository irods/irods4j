package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.Genquery2Input_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RcAtomicApplyMetadataOperationsTest {

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static RcComm comm;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		comm = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty());
		assertNotNull(comm);
		IRODSApi.rcAuthenticateClient(comm, "native", password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		IRODSApi.rcDisconnect(comm);
	}

	@Test
	void testAtomicApplyMetadataOperations() throws IOException {
		String logicalPath = '/' + String.join("/", zone, "home", username);
		String attrName = "irods4j::atomic_attr_name";
		String attrValue = "irods4j::atomic_attr_value";
		String attrUnits = "irods4j::atomic_attr_units";

		// Add some metadata to the user's home collection.
		HashMap<String, Object> op = new HashMap<String, Object>();
		op.put("operation", "add");
		op.put("attribute", attrName);
		op.put("value", attrValue);
		op.put("units", attrUnits);

		ArrayList<Object> ops = new ArrayList<Object>();
		ops.add(op);

		HashMap<String, Object> inputStruct = new HashMap<String, Object>();
		inputStruct.put("entity_name", logicalPath);
		inputStruct.put("entity_type", "collection");
		inputStruct.put("operations", ops);
		inputStruct.put("admin_mode", false);

		String operations = JsonUtil.toJsonString(inputStruct);
		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcAtomicApplyMetadataOperations(comm, operations, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);
		assertTrue("{}".equals(output.value.trim()));

		// Show the metadata exists on the user's home collection.
		Genquery2Input_PI gq2Input = new Genquery2Input_PI();
		gq2Input.query_string = String.format(
				"select META_COLL_ATTR_NAME, META_COLL_ATTR_VALUE, META_COLL_ATTR_UNITS where COLL_NAME = '%s'",
				logicalPath);
		gq2Input.zone = zone;

		ec = IRODSApi.rcGenQuery2(comm, gq2Input, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);
		String expectedString = String.format("[\"%s\",\"%s\",\"%s\"]", attrName, attrValue, attrUnits);
		assertTrue(output.value.contains(expectedString));

		// Remove the metadata.
		op.clear();
		op.put("operation", "remove");
		op.put("attribute", attrName);
		op.put("value", attrValue);
		op.put("units", attrUnits);

		operations = JsonUtil.toJsonString(inputStruct);
		output = new Reference<String>();

		ec = IRODSApi.rcAtomicApplyMetadataOperations(comm, operations, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);
		assertTrue("{}".equals(output.value.trim()));

		// Show the metadata no longer exists on the collection.
		gq2Input = new Genquery2Input_PI();
		// The extra condition is required due to GenQuery2 using LEFT-JOINS.
		gq2Input.query_string = String.format(
				"select META_COLL_ATTR_NAME where COLL_NAME = '%s' and META_COLL_ATTR_NAME is not null", logicalPath);
		gq2Input.zone = zone;

		ec = IRODSApi.rcGenQuery2(comm, gq2Input, output);
		assertEquals(ec, 0);
		assertNotNull(output);
		assertNotNull(output.value);
		assertTrue("[]".equals(output.value));
	}

}
