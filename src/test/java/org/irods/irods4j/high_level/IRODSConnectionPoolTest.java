package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers;
import org.irods.irods4j.high_level.administration.IRODSUsers.User;
import org.irods.irods4j.high_level.administration.IRODSUsers.UserType;
import org.irods.irods4j.high_level.administration.IRODSZones.ZoneType;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.IRODSConnectionPool;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectInputStream;
import org.irods.irods4j.high_level.io.IRODSDataObjectOutputStream;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem.RemoveOptions;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRODSConnectionPoolTest {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";

	static String adminUsername = "rods";
	static String adminPassword = "rods";

	// Used to verify proxy connection support is functional.
	static String clientUsername = "alice";
	static final String clientUserDataObject = String.format("/%s/home/%s/some_data_object.txt", zone, clientUsername);

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

		// Create a rodsuser for testing proxied connections.
		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(adminUsername, zone));
			conn.authenticate(new NativeAuthPlugin(), adminPassword);
			var comm = conn.getRcComm();
			var user = new User(clientUsername, Optional.of(zone));
			IRODSUsers.addUser(comm, user, UserType.RODSUSER, ZoneType.LOCAL);
		}

		// Create a data object using the rodsuser.
		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(adminUsername, zone), new QualifiedUsername(clientUsername, zone));
			conn.authenticate(new NativeAuthPlugin(), adminPassword);
			try (var out = new IRODSDataObjectOutputStream(conn.getRcComm(), clientUserDataObject, true, false)) {
				out.write("test data".getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();

		// Remove the rodsuser's data object.
		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(adminUsername, zone), new QualifiedUsername(clientUsername, zone));
			conn.authenticate(new NativeAuthPlugin(), adminUsername);
			IRODSFilesystem.remove(conn.getRcComm(), clientUserDataObject, RemoveOptions.NO_TRASH);
		}

		// Remove the rodsuser.
		try (var conn = new IRODSConnection()) {
			conn.connect(host, port, new QualifiedUsername(adminUsername, zone));
			conn.authenticate(new NativeAuthPlugin(), adminUsername);
			var user = new User(clientUsername, Optional.of(zone));
			IRODSUsers.removeUser(conn.getRcComm(), user);
		}
	}

	@Test
	void testSynchronousCreationOfPoolWithMultipleConnections() throws Exception {
		try (var pool = new IRODSConnectionPool(10)) {
			pool.start(host, port, new QualifiedUsername(adminUsername, zone), comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);

					// Returning true lets the connection pool know that authentication was
					// successful.
					return true;
				} catch (Exception e) {
					// Returning false lets the connection pool know that authentication failed.
					// This means the connection pool must not be used.
					return false;
				}
			});

			var homeCollection = String.format("/%s/home/%s", zone, adminUsername);
			for (int i = 0; i < 30; ++i) {
				try (var conn = pool.getConnection()) {
					assertTrue(conn.isValid());
					assertNotNull(conn.getRcComm());
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
			pool.start(threadPool, host, port, new QualifiedUsername(adminUsername, zone), comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);

					// Returning true lets the connection pool know that authentication was
					// successful.
					return true;
				} catch (Exception e) {
					// Returning false lets the connection pool know that authentication failed.
					// This means the connection pool must not be used.
					return false;
				}
			});

			var homeCollection = String.format("/%s/home/%s", zone, adminUsername);
			for (int i = 0; i < 30; ++i) {
				try (var conn = pool.getConnection()) {
					assertTrue(conn.isValid());
					assertNotNull(conn.getRcComm());
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
				pool.start("INVALID_HOST", port, new QualifiedUsername(adminUsername, zone), comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
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
				pool.start(host, 9000, new QualifiedUsername(adminUsername, zone), comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
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
				pool.start(host, port, new QualifiedUsername(adminUsername, zone), comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, null, adminPassword);
						return true;
					} catch (Exception e) {
						return false;
					}
				});
			}
		});
	}

	@Test
	void testSynchronousPoolStartWithProxyUser() throws Exception {
		var proxyUser = new QualifiedUsername(adminUsername, zone);
		var clientUser = new QualifiedUsername(clientUsername, zone); // same user for the test

		try (var pool = new IRODSConnectionPool(5)) {
			pool.start(host, port, proxyUser, clientUser, comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
					return true;
				} catch (Exception e) {
					return false;
				}
			});

			var homeCollection = String.format("/%s/home/%s", zone, clientUsername);
			for (int i = 0; i < 15; ++i) {
				try (var conn = pool.getConnection()) {
					assertTrue(conn.isValid());
					assertNotNull(conn.getRcComm());
					assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), homeCollection));
				}
			}
		}
	}

	@Test
	void testAsynchronousPoolStartWithProxyUser() throws Exception {
		var proxyUser = new QualifiedUsername(adminUsername, zone);
		var clientUser = new QualifiedUsername(clientUsername, zone);

		var threadPool = Executors.newFixedThreadPool(5);
		try (var pool = new IRODSConnectionPool(10)) {
			pool.start(threadPool, host, port, proxyUser, clientUser, comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
					return true;
				} catch (Exception e) {
					return false;
				}
			});

			var homeCollection = String.format("/%s/home/%s", zone, clientUsername);
			for (int i = 0; i < 15; ++i) {
				try (var conn = pool.getConnection()) {
					assertTrue(conn.isValid());
					assertNotNull(conn.getRcComm());
					assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), homeCollection));
				}
			}
		} finally {
			threadPool.shutdown();
		}
	}

	@Test
	void testNullProxyUserResultsInExceptionBeingThrown() {
		assertThrows(IllegalArgumentException.class, () -> {
			try (var pool = new IRODSConnectionPool(3)) {
				pool.start(host, port, null, new QualifiedUsername(adminUsername, zone), comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
						return true;
					} catch (Exception e) {
						return false;
					}
				});
			}
		});
	}

	@Test
	void testBadHostWithProxyUserResultsInExceptionBeingThrown() {
		var proxyUser = new QualifiedUsername(adminUsername, zone);
		var clientUser = new QualifiedUsername(clientUsername, zone);

		assertThrows(IllegalStateException.class, () -> {
			try (var pool = new IRODSConnectionPool(1)) {
				pool.start("INVALID_HOST", port, proxyUser, clientUser, comm -> {
					try {
						IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
						return true;
					} catch (Exception e) {
						return false;
					}
				});
			}
		});
	}

	@Test
	void testPoolOfProxyConnectionsHonorsClientPermissions() throws Exception {
		var proxyUser = new QualifiedUsername(adminUsername, zone);
		var clientUser = new QualifiedUsername(clientUsername, zone);

		try (var pool = new IRODSConnectionPool(2)) {
			pool.start(host, port, proxyUser, clientUser, comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
					return true;
				} catch (Exception e) {
					return false;
				}
			});

			try (var conn1 = pool.getConnection(); var conn2 = pool.getConnection()) {
				// The following should succeed because all connections in the pool
				// are viewed by the iRODS server as representing the client user and
				// the client user owns the target data object.
				assertDoesNotThrow(() -> {
					try (var in = new IRODSDataObjectInputStream(conn1.getRcComm(), clientUserDataObject)) {
						in.read();
					}

					try (var in = new IRODSDataObjectInputStream(conn2.getRcComm(), clientUserDataObject)) {
						in.read();
					}
				});
			}
		}
	}

	@Test
	void testPoolOfProxyConnectionsDoesNotAllowRodsadminToReadDataWithoutPermission() throws Exception {
		var adminUser = new QualifiedUsername(adminUsername, zone);

		try (var pool = new IRODSConnectionPool(2)) {
			pool.start(host, port, adminUser, comm -> {
				try {
					IRODSApi.rcAuthenticateClient(comm, new NativeAuthPlugin(), adminPassword);
					return true;
				} catch (Exception e) {
					return false;
				}
			});

			try (var conn1 = pool.getConnection(); var conn2 = pool.getConnection()) {
				var ex = assertThrows(IRODSException.class, () -> {
					try (var in = new IRODSDataObjectInputStream(conn1.getRcComm(), clientUserDataObject)) {
						in.read();
					}

					try (var in = new IRODSDataObjectInputStream(conn2.getRcComm(), clientUserDataObject)) {
						in.read();
					}
				});
				assertTrue(ex.getErrorCode() < 0);
				assertTrue(ex.getMessage().contains("rcReplicaOpen error"));
			}
		}
	}

}