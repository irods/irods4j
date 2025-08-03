package org.irods.irods4j.high_level;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers.UserType;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem.RemoveOptions;
import org.irods.irods4j.high_level.vfs.ObjectStatus;
import org.irods.irods4j.high_level.vfs.Permission;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class IRODSFilesystemTest {

	static final Logger log = LogManager.getLogger();

	static String host = "localhost";
	static int port = 1247;
	static String zone = "tempZone";
	static String username = "rods";
	static String password = "rods";
	static IRODSConnection conn;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		XmlUtil.enablePrettyPrinting();
		JsonUtil.enablePrettyPrinting();

		conn = new IRODSConnection();
		conn.connect(host, port, new QualifiedUsername(username, zone));
		conn.authenticate(new NativeAuthPlugin(), password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		conn.disconnect();

		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testCreateAndDeleteCollection() throws Exception {
		String collection = '/' + String.join("/", zone, "home", username, "testCreateAndDeleteCollection");
		assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), collection));
		assertTrue(IRODSFilesystem.remove(conn.getRcComm(), collection, RemoveOptions.NO_TRASH));
	}

	@Test
	void testModifyingTheInheritanceFlagOfACollection() throws IOException, IRODSException {
		String collection = '/' + String.join("/", zone, "home", username, "testModifyingTheInheritanceFlagOfACollection");

		try {
			// Create a new collection.
			assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), collection));

			// Show that inheritance is NOT enabled on the new collection.
			ObjectStatus status = IRODSFilesystem.status(conn.getRcComm(), collection);
			assertFalse(status.isInheritanceEnabled());

			// Enable inheritance on the collection and show that it is indeed enabled.
			IRODSFilesystem.enableInheritance(conn.getRcComm(), collection, true);
			status = IRODSFilesystem.status(conn.getRcComm(), collection);
			assertTrue(status.isInheritanceEnabled());

			// Now disable inheritance on the new collection.
			IRODSFilesystem.enableInheritance(conn.getRcComm(), collection, false);
			status = IRODSFilesystem.status(conn.getRcComm(), collection);
			assertFalse(status.isInheritanceEnabled());
		} finally {
			assertTrue(IRODSFilesystem.remove(conn.getRcComm(), collection, RemoveOptions.NO_TRASH));
		}
	}

	@Test
	void testRenameACollection() throws IOException, IRODSException {
		String collection = '/' + String.join("/", zone, "home", username, "testRenameACollection");
		String collToRemove = collection;

		try {
			// Create a new collection.
			assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), collection));

			// Rename the collection.
			String newCollName = collection + ".renamed";
			IRODSFilesystem.rename(conn.getRcComm(), collection, newCollName);

			// Update the path so that the test can clean up. It's important that this
			// happen AFTER the rename operation.
			collToRemove = newCollName;

			// Show the collection no longer exists by its original name.
			assertFalse(IRODSFilesystem.exists(conn.getRcComm(), collection));
			assertFalse(IRODSFilesystem.isCollection(conn.getRcComm(), collection));

			// Show the collection exists by its new name.
			assertTrue(IRODSFilesystem.exists(conn.getRcComm(), newCollName));
			assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), newCollName));
		} finally {
			assertTrue(IRODSFilesystem.remove(conn.getRcComm(), collToRemove, RemoveOptions.NO_TRASH));
		}
	}

	@Test
	void testCopyADataObjectUsingCopyDataObjectFunction() throws Exception {
		String sandbox = '/' + String.join("/", zone, "home", username, "testCopyADataObjectUsingCopyDataObjectFunction");

		try {
			assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), sandbox));

			// Create a data object.
			String from = String.join("/", sandbox, "data_object1");
			try (IRODSDataObjectStream stream = new IRODSDataObjectStream()) {
				stream.open(conn.getRcComm(), from, OpenFlags.O_CREAT | OpenFlags.O_WRONLY);
			}
			ObjectStatus fromStatus = IRODSFilesystem.status(conn.getRcComm(), from);
			assertTrue(IRODSFilesystem.exists(fromStatus));
			assertTrue(IRODSFilesystem.isDataObject(fromStatus));

			// Copy the data object and show that it exists.
			String to = String.join("/", sandbox, "data_object2");
			assertTrue(IRODSFilesystem.copyDataObject(conn.getRcComm(), from, to));

			ObjectStatus toStatus = IRODSFilesystem.status(conn.getRcComm(), to);
			assertTrue(IRODSFilesystem.exists(toStatus));
			assertTrue(IRODSFilesystem.isDataObject(toStatus));
		} finally {
			IRODSFilesystem.removeAll(conn.getRcComm(), sandbox, RemoveOptions.NO_TRASH);
		}
	}

	@Test
	void testListPermissionsOnDataObject() throws Exception {
		String path = '/' + String.join("/", zone, "home", username, "testListPermissionsOnDataObject");

		// Create a new data object.
		try (IRODSDataObjectStream out = new IRODSDataObjectStream()) {
			out.open(conn.getRcComm(), path, OpenFlags.O_CREAT | OpenFlags.O_WRONLY);
		}

		// Get the permissions on the data object.
		ObjectStatus status = IRODSFilesystem.status(conn.getRcComm(), path);
		status.getPermissions().forEach(p -> {
			log.debug("[name={}, zone={}, permission={}, type={}]", p.getName(), p.getZone(), p.getPermission(),
					p.getUserType());
		});
		assertFalse(status.getPermissions().isEmpty());
		assertTrue(status.getPermissions().stream().anyMatch(ep -> {
			return username.equals(ep.getName()) && zone.equals(ep.getZone()) && Permission.OWN == ep.getPermission()
					&& UserType.RODSADMIN == ep.getUserType();
		}));
	}

}
