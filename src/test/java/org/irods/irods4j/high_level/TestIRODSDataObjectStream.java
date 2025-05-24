package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnectionPool;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectInputStream;
import org.irods.irods4j.high_level.io.IRODSDataObjectOutputStream;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream.OnCloseSuccess;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream.SeekDirection;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem.RemoveOptions;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.ByteArrayReference;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestIRODSDataObjectStream {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static RcComm comm;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

		comm = IRODSApi.rcConnect(host, port, username, zone, Optional.empty(), Optional.empty(), Optional.empty(),
				Optional.empty());
		assertNotNull(comm);
		IRODSApi.rcAuthenticateClient(comm, "native", password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		IRODSApi.rcDisconnect(comm);

		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testDataObjectStreamCapturesReplicaNumberAndReplicaToken() throws Exception {
		var logicalPath = Paths
				.get("/", zone, "home", username, "testDataObjectStreamCapturesReplicaNumberAndReplicaToken.txt")
				.toString();

		var in = new IRODSDataObjectStream();
		in.open(comm, logicalPath, OpenFlags.O_CREAT | OpenFlags.O_WRONLY);
		assertTrue(in.isOpen());

		assertTrue(in.getNativeHandle() >= 3);
		log.info("Native handle = {}", in.getNativeHandle());

		assertTrue(in.getReplicaNumber() >= 0);
		log.info("Replica number = {}", in.getReplicaNumber());

		assertNotNull(in.getReplicaToken());
		log.info("Replica token = {}", in.getReplicaToken());

		in.close();
		assertFalse(in.isOpen());
	}

	@Test
	void testReadingAndWritingUsingInputOutputStreams() throws IOException, IRODSException {
		var logicalPath = Paths.get("/", zone, "home", username, "testInputOutputStreamImplementation").toString();

		try {
			var truncate = true;
			var append = false;
			var data = """
					This was written using an IRODSDataObjectOutputStream.
					The data will be written on close due to the internal buffer not being filled.
					""".getBytes(StandardCharsets.UTF_8);

			// Create a new data object and write some data to it.
			try (var out = new IRODSDataObjectOutputStream(comm, logicalPath, truncate, append)) {
				out.write(data);
				assertTrue(out.isOpen());
			}

			// Read the data from the newly created data object.
			try (var in = new IRODSDataObjectInputStream(comm, logicalPath)) {
				assertTrue(in.isOpen());
				var buffer = new byte[data.length];
				in.read(buffer);
				assertArrayEquals(data, buffer);
			}
		} finally {
			IRODSFilesystem.remove(comm, logicalPath, RemoveOptions.NO_TRASH);
		}
	}

	@Test
	void testReadingAndWritingUsingInputOutputStreamsAndVerySmallInternalBuffer() throws IOException, IRODSException {
		var logicalPath = Paths.get("/", zone, "home", username,
				"testReadingAndWritingUsingInputOutputStreamsAndVerySmallInternalBuffer").toString();

		try {
			// The buffer size used by the input/output streams. The size is intentionally
			// small to trigger more network requests.
			var internalBufferSize = 20;

			var truncate = true;
			var append = false;
			var data = """
					This was written using an IRODSDataObjectOutputStream.
					The data will be written on close due to the internal buffer not being filled.
					""".getBytes(StandardCharsets.UTF_8);

			// Create a new data object and write some data to it.
			try (var out = new IRODSDataObjectOutputStream(internalBufferSize)) {
				out.open(comm, logicalPath, truncate, append);
				assertTrue(out.isOpen());
				out.write(data);
			}

			// Read the data from the newly created data object.
			try (var in = new IRODSDataObjectInputStream(internalBufferSize)) {
				in.open(comm, logicalPath);
				assertTrue(in.isOpen());
				var buffer = new byte[data.length];
				in.read(buffer);
				assertArrayEquals(data, buffer);
			}
		} finally {
			IRODSFilesystem.remove(comm, logicalPath, RemoveOptions.NO_TRASH);
		}
	}

	@Test
	void testParallelTransferOverPort1247() throws Exception {
		var logicalPath = Paths.get("/", zone, "home", username, "testParallelTransferOverPort1247.txt").toString();
		var streamCount = 3;

		try (var connPool = new IRODSConnectionPool(streamCount)) {
			connPool.start(host, port, new QualifiedUsername(username, zone), conn -> {
				try {
					IRODSApi.rcAuthenticateClient(conn, "native", password);
					return true;
				} catch (Exception e) {
					return false;
				}
			});

			// Create the primary data stream. This stream must be closed last so that
			// policy fires appropriately.
			try (var stream1 = new IRODSDataObjectStream(); var conn1 = connPool.getConnection()) {
				stream1.open(conn1.getRcComm(), logicalPath,
						OpenFlags.O_CREAT | OpenFlags.O_WRONLY | OpenFlags.O_TRUNC);

				// These are needed so that iRODS knows they are for the parallel transfer.
				var replicaToken = stream1.getReplicaToken();
				var replicaNumber = stream1.getReplicaNumber();

				try (var stream2 = new IRODSDataObjectStream();
						var stream3 = new IRODSDataObjectStream();
						var conn2 = connPool.getConnection();
						var conn3 = connPool.getConnection()) {
					// Open the secondary streams using the replica token and replica number from
					// the primary stream.
					stream2.open(conn2.getRcComm(), replicaToken, logicalPath, replicaNumber, OpenFlags.O_WRONLY);
					stream3.open(conn3.getRcComm(), replicaToken, logicalPath, replicaNumber, OpenFlags.O_WRONLY);

					// Seek the appropriate offsets.
					stream2.seek(100, SeekDirection.BEGIN);
					stream3.seek(200, SeekDirection.BEGIN);

					// Write the bytes.

					var threadPool = Executors.newFixedThreadPool(streamCount);
					var threadExperiencedAnError = new AtomicBoolean();

					var future1 = threadPool.submit(() -> {
						var buffer = new byte[100];
						Arrays.fill(buffer, (byte) 'A');
						try {
							stream1.write(buffer, 100);
						} catch (IOException | IRODSException e) {
							threadExperiencedAnError.set(true);
						}
					});

					var future2 = threadPool.submit(() -> {
						var buffer = new byte[100];
						Arrays.fill(buffer, (byte) 'B');
						try {
							stream2.write(buffer, 100);
						} catch (IOException | IRODSException e) {
							threadExperiencedAnError.set(true);
						}
					});

					var future3 = threadPool.submit(() -> {
						var buffer = new byte[100];
						Arrays.fill(buffer, (byte) 'C');
						try {
							stream3.write(buffer, 100);
						} catch (IOException | IRODSException e) {
							threadExperiencedAnError.set(true);
						}
					});

					// Wait for each thread to finish.
					future1.get();
					future2.get();
					future3.get();

					// Make sure none of the threads threw an exception.
					assertFalse(threadExperiencedAnError.get());

					// Instruct the secondary streams to not update the catalog or trigger policy on
					// close.
					var closeInstructions = new OnCloseSuccess();
					closeInstructions.updateSize = false;
					closeInstructions.updateStatus = false;
					closeInstructions.computeChecksum = false;
					closeInstructions.sendNotifications = false;
					closeInstructions.preserveReplicaStateTable = false;

					stream2.close(closeInstructions);
					stream3.close(closeInstructions);
				}
			}

			// The primary stream is closed automatically using the default options. The
			// default options to ".close()" update the replica's status, size, and trigger
			// policy.
			var dataSize = IRODSFilesystem.dataObjectSize(comm, logicalPath);
			assertEquals(dataSize, 300);

			// Read the contents of the data object and assert it is what we wrote.
			try (var in = new IRODSDataObjectStream(); var conn1 = connPool.getConnection()) {
				in.open(conn1.getRcComm(), logicalPath, OpenFlags.O_RDONLY);

				// Read the data.
				var buffer = new byte[300];
				var bytesRead = in.read(buffer, buffer.length);
				assertEquals(bytesRead, buffer.length);

				// Create a buffer containing the expected contents.
				var expected = new byte[300];
				Arrays.fill(expected, 0, 100, (byte) 'A');
				Arrays.fill(expected, 100, 200, (byte) 'B');
				Arrays.fill(expected, 200, 300, (byte) 'C');

				// Show the buffers contain identical data.
				assertArrayEquals(buffer, expected);
			}
		} finally {
			IRODSFilesystem.remove(comm, logicalPath, RemoveOptions.NO_TRASH);
		}
	}

}
