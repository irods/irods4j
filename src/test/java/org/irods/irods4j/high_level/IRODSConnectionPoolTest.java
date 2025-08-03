package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.connection.IRODSConnectionPool;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRODSConnectionPoolTest {

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
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testSynchronousCreationOfPoolWithMultipleConnections() throws Exception {
		try (var pool = new IRODSConnectionPool(10)) {
			pool.start(host, port, new QualifiedUsername(username, zone), comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), password);

					// Returning true lets the connection pool know that authentication was
					// successful.
					return true;
				} catch (Exception e) {
					// Returning false lets the connection pool know that authentication failed.
					// This means the connection pool must not be used.
					return false;
				}
			});

			var homeCollection = String.format("/%s/home/%s", zone, username);
			for (int i = 0; i < 30; ++i) {
				try (var conn = pool.getConnection()) {
					assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), homeCollection));
				}
			}
		}
	}

	@Test
	void testAsynchronousCreationOfPoolWithMultipleConnections() throws Exception {
		try (var pool = new IRODSConnectionPool(10)) {
			// Create a thread pool containing 5 threads.
			var threadPool = Executors.newFixedThreadPool(5);

			// Use the thread pool to speed up the connection process.
			pool.start(threadPool, host, port, new QualifiedUsername(username, zone), comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), password);

					// Returning true lets the connection pool know that authentication was
					// successful.
					return true;
				} catch (Exception e) {
					// Returning false lets the connection pool know that authentication failed.
					// This means the connection pool must not be used.
					return false;
				}
			});

			var homeCollection = String.format("/%s/home/%s", zone, username);
			for (int i = 0; i < 30; ++i) {
				try (var conn = pool.getConnection()) {
					assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), homeCollection));
				}
			}

			threadPool.shutdown();
		}
	}

	@Test
	void testBadHostResultsInExceptionBeingThrown() throws Exception {
		assertThrows(IllegalStateException.class, () -> {
			try (var pool = new IRODSConnectionPool(1)) {
				pool.start("INVALID_HOST", port, new QualifiedUsername(username, zone), comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), password);
						return true;
					} catch (Exception e) {
						return false;
					}
				});
			}
		});
	}

	@Test
	void testBadPortResultsInExceptionBeingThrown() throws Exception {
		assertThrows(IllegalStateException.class, () -> {
			try (var pool = new IRODSConnectionPool(1)) {
				pool.start(host, 9000, new QualifiedUsername(username, zone), comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), password);
						return true;
					} catch (Exception e) {
						return false;
					}
				});
			}
		});
	}

	@Test
	void testBadAuthSchemeResultsInExceptionBeingThrown() throws Exception {
		assertThrows(IllegalStateException.class, () -> {
			try (var pool = new IRODSConnectionPool(1)) {
				pool.start(host, port, new QualifiedUsername(username, zone), comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, null, password);
						return true;
					} catch (Exception e) {
						return false;
					}
				});
			}
		});
	}

}
