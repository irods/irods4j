package org.irods.irods4j.high_level;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.administration.IRODSUsers;
import org.irods.irods4j.high_level.administration.IRODSUsers.UserType;
import org.irods.irods4j.high_level.administration.IRODSZones;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectOutputStream;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem.RemoveOptions;
import org.irods.irods4j.high_level.vfs.IRODSReplicas;
import org.irods.irods4j.high_level.vfs.Permission;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
		var collection = '/' + String.join("/", zone, "home", username, "testCreateAndDeleteCollection");
		assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), collection));
		assertTrue(IRODSFilesystem.remove(conn.getRcComm(), collection, RemoveOptions.NO_TRASH));
	}

	@Test
	void testModifyingTheInheritanceFlagOfACollection() throws IOException, IRODSException {
		var collection = '/' + String.join("/", zone, "home", username, "testModifyingTheInheritanceFlagOfACollection");

		try {
			// Create a new collection.
			assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), collection));

			// Show that inheritance is NOT enabled on the new collection.
			var status = IRODSFilesystem.status(conn.getRcComm(), collection);
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
		var collection = '/' + String.join("/", zone, "home", username, "testRenameACollection");
		String collToRemove = collection;

		try {
			// Create a new collection.
			assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), collection));

			// Rename the collection.
			var newCollName = collection + ".renamed";
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
		var sandbox = '/' + String.join("/", zone, "home", username, "testCopyADataObjectUsingCopyDataObjectFunction");

		try {
			assertTrue(IRODSFilesystem.createCollection(conn.getRcComm(), sandbox));

			// Create a data object.
			var from = String.join("/", sandbox, "data_object1");
			try (var stream = new IRODSDataObjectStream()) {
				stream.open(conn.getRcComm(), from, OpenFlags.O_CREAT | OpenFlags.O_WRONLY);
			}
			var fromStatus = IRODSFilesystem.status(conn.getRcComm(), from);
			assertTrue(IRODSFilesystem.exists(fromStatus));
			assertTrue(IRODSFilesystem.isDataObject(fromStatus));

			// Copy the data object and show that it exists.
			var to = String.join("/", sandbox, "data_object2");
			assertTrue(IRODSFilesystem.copyDataObject(conn.getRcComm(), from, to));

			var toStatus = IRODSFilesystem.status(conn.getRcComm(), to);
			assertTrue(IRODSFilesystem.exists(toStatus));
			assertTrue(IRODSFilesystem.isDataObject(toStatus));
		} finally {
			IRODSFilesystem.removeAll(conn.getRcComm(), sandbox, RemoveOptions.NO_TRASH);
		}
	}

	@Test
	void testListPermissionsOnDataObject() throws Exception {
		var path = '/' + String.join("/", zone, "home", username, "testListPermissionsOnDataObject");

		// Create a new data object.
		try (var out = new IRODSDataObjectStream()) {
			out.open(conn.getRcComm(), path, OpenFlags.O_CREAT | OpenFlags.O_WRONLY);
		}

		// Get the permissions on the data object.
		var status = IRODSFilesystem.status(conn.getRcComm(), path);
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

	@Test
	void testSupportForSingleQuotesInLogicalPaths() throws Exception {
		var collection = '/' + String.join("/", zone, "home", username, "single'quotes'in'collection");
		var dataObject = String.join("/", collection, "single'quotes'in'data'object");

		try {
			// Create a collection and verify the filesystem library can identify it.
			// Keep in mind that the collection path contains embedded single quotes.
			IRODSFilesystem.createCollections(conn.getRcComm(), collection);
			assertTrue(IRODSFilesystem.exists(conn.getRcComm(), collection));
			assertTrue(IRODSFilesystem.isCollection(conn.getRcComm(), collection));
			assertTrue(IRODSFilesystem.isCollectionRegistered(conn.getRcComm(), collection));
			assertFalse(IRODSFilesystem.isSpecialCollection(conn.getRcComm(), collection));
			assertTrue(IRODSFilesystem.lastWriteTime(conn.getRcComm(), collection) > 0L);

			// Create a new data object. This data object path contains embedded single
			// quotes in the collection and data object name.
			var dataBuffer = "Hello, iRODS supports embedded single quotes!".getBytes(StandardCharsets.UTF_8);
			try (var out = new IRODSDataObjectStream()) {
				out.open(conn.getRcComm(), dataObject, OpenFlags.O_CREAT | OpenFlags.O_WRONLY);
				out.write(dataBuffer, dataBuffer.length);
			}
			assertTrue(IRODSFilesystem.exists(conn.getRcComm(), collection));
			assertTrue(IRODSFilesystem.isDataObject(conn.getRcComm(), dataObject));
			assertTrue(IRODSFilesystem.isDataObjectRegistered(conn.getRcComm(), dataObject));
			assertEquals(dataBuffer.length, IRODSFilesystem.dataObjectSize(conn.getRcComm(), dataObject));
			assertFalse(IRODSFilesystem.isSpecialCollection(conn.getRcComm(), dataObject));
			assertTrue(IRODSFilesystem.lastWriteTime(conn.getRcComm(), dataObject) > 0L);

			// Calculate a checksum for the replica.
			assertEquals("", IRODSFilesystem.dataObjectChecksum(conn.getRcComm(), dataObject));
			IRODSReplicas.replicaChecksum(conn.getRcComm(), dataObject, 0, IRODSReplicas.VerificationCalculation.ALWAYS);
			assertEquals("sha2:dbN9VEj3kAmeU/objQ+ffJENPHqwYZYK5+dOE4oLy5M=", IRODSFilesystem.dataObjectChecksum(conn.getRcComm(), dataObject));
		}
		finally {
			IRODSFilesystem.removeAll(conn.getRcComm(), collection, RemoveOptions.NO_TRASH);
		}
	}

	@Test
	void testGroupMembershipIsNotExpandedWhenListingPermissionsOnDataObject() throws Exception {
		var sandbox = '/' + String.join("/", zone, "home", username, "group_member_expansion");
		var dataObject = sandbox + "/group_owned_data_object.txt";

		var rodsuser1 = new IRODSUsers.User("rodsuser1", Optional.empty());
		var rodsuser2 = new IRODSUsers.User("rodsuser2", Optional.empty());

		var group1 = new IRODSUsers.Group("user1_group");
		var group2 = new IRODSUsers.Group("user2_group");

		try {
			IRODSFilesystem.createCollections(conn.getRcComm(), sandbox);

			// Create two rodsuser users.
			IRODSUsers.addUser(conn.getRcComm(), rodsuser1, UserType.RODSUSER, IRODSZones.ZoneType.LOCAL);
			IRODSUsers.addUser(conn.getRcComm(), rodsuser2, UserType.RODSUSER, IRODSZones.ZoneType.LOCAL);

			// Create two groups and add one user to each of them.
			IRODSUsers.addGroup(conn.getRcComm(), group1);
			IRODSUsers.addGroup(conn.getRcComm(), group2);
			IRODSUsers.addUserToGroup(conn.getRcComm(), group1, rodsuser2);
			IRODSUsers.addUserToGroup(conn.getRcComm(), group2, rodsuser1);

			// Create a data object and adjust the permissions such that only members
			// of the groups can access it.
			try (var out = new IRODSDataObjectOutputStream(conn.getRcComm(), dataObject, true, false)) {
				out.write("not empty".getBytes(StandardCharsets.UTF_8));
			}
			IRODSFilesystem.permissions(conn.getRcComm(), dataObject, group1.name, Permission.READ_OBJECT);
			IRODSFilesystem.permissions(conn.getRcComm(), dataObject, group2.name, Permission.READ_OBJECT);
			IRODSFilesystem.permissions(conn.getRcComm(), dataObject, username, Permission.NULL);

			// Show that retrieval of permissions for groups are not expanded. That is,
			// we expect the permissions list to contain only the recently created groups.
			// Indirect permissions (i.e. the members within the groups) must not be part
			// of the permissions list.
			var status = IRODSFilesystem.status(conn.getRcComm(), dataObject);
			var perms = status.getPermissions();
			assertEquals(2, perms.size());
			assertTrue(perms.stream().anyMatch(p -> UserType.RODSGROUP == p.getUserType() && group1.name.equals(p.getName())));
			assertTrue(perms.stream().anyMatch(p -> UserType.RODSGROUP == p.getUserType() && group2.name.equals(p.getName())));
		}
		finally {
			try { IRODSFilesystem.permissions(IRODSFilesystem.asAdmin, conn.getRcComm(), dataObject, username, Permission.OWN); } catch (Exception ignored) {}
			try { IRODSFilesystem.removeAll(conn.getRcComm(), sandbox, RemoveOptions.NO_TRASH); } catch (Exception ignored) {}
			try { IRODSUsers.removeGroup(conn.getRcComm(), group1); } catch (Exception ignored) {}
			try { IRODSUsers.removeGroup(conn.getRcComm(), group2); } catch (Exception ignored) {}
			try { IRODSUsers.removeUser(conn.getRcComm(), rodsuser1); } catch (Exception ignored) {}
			try { IRODSUsers.removeUser(conn.getRcComm(), rodsuser2); } catch (Exception ignored) {}
		}
	}

}
