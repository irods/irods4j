package org.irods.irods4j.low_level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.ByteArrayReference;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.OpenedDataObjInp_PI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StreamOperationsTest {

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
		IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		IRODSApi.rcDisconnect(comm);
	}

	@Test
	void testStreamOperations() throws IOException {
		var logicalPath = '/' + String.join("/", zone, "home", username, "testStreamOperations.txt");

		// Open a data object for writing.
		var openInput = new DataObjInp_PI();
		openInput.objPath = logicalPath.toString();
		openInput.dataSize = -1;
		openInput.createMode = 0600;
		openInput.openFlags = OpenFlags.O_CREAT | OpenFlags.O_WRONLY | OpenFlags.O_TRUNC;
		openInput.KeyValPair_PI = new KeyValPair_PI();
		openInput.KeyValPair_PI.ssLen = 0;

		var l1descInfo = new Reference<String>();
		var fd = IRODSApi.rcReplicaOpen(comm, openInput, l1descInfo);
		assertTrue(fd >= 3);
		assertNotNull(l1descInfo);
		assertNotNull(l1descInfo.value);
		assertFalse(l1descInfo.value.isEmpty());

		// Write some data to the open replica.
		var writeBuffer = "Hello, irods4j!\n".getBytes(StandardCharsets.UTF_8);
		var writeInput = new OpenedDataObjInp_PI();
		writeInput.l1descInx = fd;
		writeInput.len = writeBuffer.length;
		writeInput.KeyValPair_PI = new KeyValPair_PI();
		writeInput.KeyValPair_PI.ssLen = 0;
		
		var bytesWritten = IRODSApi.rcDataObjWrite(comm, writeInput, writeBuffer);
		assertEquals(bytesWritten, writeInput.len);
		assertEquals(bytesWritten, writeBuffer.length);

		// Close the replica.
		var closeOptions = new HashMap<String, Object>();
		closeOptions.put("fd", fd);
		var closeInput = JsonUtil.toJsonString(closeOptions);
		var ec = IRODSApi.rcReplicaClose(comm, closeInput);
		assertEquals(ec, 0);

		// Open the data object again, but for reading.
		openInput = new DataObjInp_PI();
		openInput.objPath = logicalPath.toString();
		openInput.dataSize = -1;
		openInput.openFlags = OpenFlags.O_RDONLY;
		openInput.KeyValPair_PI = new KeyValPair_PI();
		openInput.KeyValPair_PI.ssLen = 0;

		l1descInfo = new Reference<String>();
		fd = IRODSApi.rcReplicaOpen(comm, openInput, l1descInfo);
		assertTrue(fd >= 3);
		assertNotNull(l1descInfo);
		assertNotNull(l1descInfo.value);
		assertFalse(l1descInfo.value.isEmpty());

		// Read some data from the open replica.
		var readInput = new OpenedDataObjInp_PI();
		readInput.l1descInx = fd;
		readInput.len = 8192;

		var readBuffer = new ByteArrayReference();
		readBuffer.data = new byte[readInput.len];
		var bytesRead = IRODSApi.rcDataObjRead(comm, readInput, readBuffer);
		assertTrue(bytesRead > 0);
		assertTrue(Arrays.equals(Arrays.copyOf(readBuffer.data, bytesRead), writeBuffer));

		// Close the replica.
		closeOptions = new HashMap<String, Object>();
		closeOptions.put("fd", fd);
		closeInput = JsonUtil.toJsonString(closeOptions);
		ec = IRODSApi.rcReplicaClose(comm, closeInput);
		assertEquals(ec, 0);
	}

}
