package org.irods.irods4j.high_level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.authentication.NativeAuthPlugin;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.high_level.connection.IRODSConnection;
import org.irods.irods4j.high_level.connection.QualifiedUsername;
import org.irods.irods4j.high_level.io.IRODSDataObjectOutputStream;
import org.irods.irods4j.high_level.metadata.IRODSMetadata;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem;
import org.irods.irods4j.high_level.vfs.IRODSFilesystem.RemoveOptions;
import org.irods.irods4j.high_level.vfs.Permission;
import org.irods.irods4j.low_level.api.GenQuery1Columns;
import org.irods.irods4j.low_level.api.IRODSException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IRODSMetadataTest {

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
		assertTrue(conn.isConnected());
		conn.authenticate(new NativeAuthPlugin(), password);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		conn.disconnect();

		XmlUtil.disablePrettyPrinting();
		JsonUtil.disablePrettyPrinting();
	}

	@Test
	void testAddSetRemoveMetadataForDataObject() throws IOException, IRODSException {
		var dataName = "testAddSetRemoveMetadataForDataObject";
		var logicalPath = '/' + String.join("/", zone, "home", username, dataName);
		var attrName = "data_object_attr_name";
		var attrValue = "data_object_attr_value";

		try {
			// Create a new data object.
			try (var out = new IRODSDataObjectOutputStream(conn.getRcComm(), logicalPath, true /* truncate */, false /* append */)) {
				assertTrue(out.isOpen());
			}

			// Show that the data object has no metadata attached to it.
			var query = String.format("select DATA_NAME where META_DATA_ATTR_NAME = '%s' and META_DATA_ATTR_VALUE = '%s'", attrName, attrValue);
			assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());

			// Attach metadata to the data object.
			IRODSMetadata.addMetadataToDataObject(conn.getRcComm(), logicalPath, attrName, attrValue, Optional.empty());
			var rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
			assertFalse(rows.isEmpty());
			assertFalse(rows.get(0).isEmpty());
			assertEquals(dataName, rows.get(0).get(0));

			// Change the attribute value. "Setting" an AVU will cause all attributes sharing the same
			// attribute name to be collapsed to the new AVU. This does not affect other data objects.
			attrValue = "new_data_object_attr_value";
			IRODSMetadata.setMetadataOnDataObject(conn.getRcComm(), logicalPath, attrName, attrValue, Optional.empty());
			// Show that the operation cleared the old AVU.
			rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
			assertTrue(rows.isEmpty());
			// Show that the new AVU is attached to the data object.
			query = String.format("select DATA_NAME where META_DATA_ATTR_NAME = '%s' and META_DATA_ATTR_VALUE = '%s'", attrName, attrValue);
			rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
			assertFalse(rows.isEmpty());
			assertFalse(rows.get(0).isEmpty());
			assertEquals(dataName, rows.get(0).get(0));

			// Remove the recently attached metadata from the data object.
			IRODSMetadata.removeMetadataFromDataObject(conn.getRcComm(), logicalPath, attrName, attrValue, Optional.empty());
			assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
		} finally {
			IRODSFilesystem.remove(conn.getRcComm(), logicalPath, RemoveOptions.NO_TRASH);
		}
	}

	@Test
	void testAddSetRemoveMetadataForDataObjectAsAdmin() throws IOException, IRODSException {
		var dataName = "testAddSetRemoveMetadataForDataObject";
		var logicalPath = '/' + String.join("/", zone, "home", username, dataName);
		var attrName = "data_object_attr_name";
		var attrValue = "data_object_attr_value";

		try {
			// Create a new data object.
			try (var out = new IRODSDataObjectOutputStream(conn.getRcComm(), logicalPath, true /* truncate */, false /* append */)) {
				assertTrue(out.isOpen());
			}

			// Remove the permissions from the data object.
			assertDoesNotThrow(() -> IRODSFilesystem.permissions(conn.getRcComm(), logicalPath, username, Permission.NULL));
			assertTrue(IRODSFilesystem.status(conn.getRcComm(), logicalPath).getPermissions().isEmpty());

			// Show that the data object has no metadata attached to it.
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			var query = String.format("select DATA_NAME where META_DATA_ATTR_NAME = '%s' and META_DATA_ATTR_VALUE = '%s'", attrName, attrValue);
//			assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
			var queryArgs = new IRODSQuery.GenQuery1QueryArgs();
			queryArgs.addColumnToSelectClause(GenQuery1Columns.COL_DATA_NAME);
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_DATA_ATTR_NAME, String.format("= '%s'", attrName));
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_DATA_ATTR_VALUE, String.format("= '%s'", attrValue));
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				fail("Data object should not have any metadata attached to it.");
				return false;
			});

			// Attach metadata to the data object.
			IRODSMetadata.addMetadataToDataObject(IRODSMetadata.asAdmin, conn.getRcComm(), logicalPath, attrName, attrValue, Optional.empty());
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			var rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
//			assertFalse(rows.isEmpty());
//			assertFalse(rows.get(0).isEmpty());
//			assertEquals(dataName, rows.get(0).get(0));
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				assertEquals(dataName, row.get(0));
				return false;
			});

			// Change the attribute value. "Setting" an AVU will cause all attributes sharing the same
			// attribute name to be collapsed to the new AVU. This does not affect other data objects.
			attrValue = "new_data_object_attr_value";
			IRODSMetadata.setMetadataOnDataObject(IRODSMetadata.asAdmin, conn.getRcComm(), logicalPath, attrName, attrValue, Optional.empty());
			// Show that the operation cleared the old AVU.
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
//			assertTrue(rows.isEmpty());
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				fail("Found previous AVU attached to data object.");
				return false;
			});
			// Show that the new AVU is attached to the data object.
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			query = String.format("select DATA_NAME where META_DATA_ATTR_NAME = '%s' and META_DATA_ATTR_VALUE = '%s'", attrName, attrValue);
//			rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
//			assertFalse(rows.isEmpty());
//			assertFalse(rows.get(0).isEmpty());
//			assertEquals(dataName, rows.get(0).get(0));
			queryArgs.init(); // Reset the object state.
			queryArgs.addColumnToSelectClause(GenQuery1Columns.COL_DATA_NAME);
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_DATA_ATTR_NAME, String.format("= '%s'", attrName));
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_DATA_ATTR_VALUE, String.format("= '%s'", attrValue));
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				assertEquals(dataName, row.get(0));
				return false;
			});

			// Remove the recently attached metadata from the data object.
			IRODSMetadata.removeMetadataFromDataObject(IRODSMetadata.asAdmin, conn.getRcComm(), logicalPath, attrName, attrValue, Optional.empty());
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				fail("No metadata should be attached to the data object.");
				return false;
			});
		} finally {
			IRODSFilesystem.permissions(IRODSFilesystem.asAdmin, conn.getRcComm(), logicalPath, username, Permission.OWN);
			IRODSFilesystem.remove(conn.getRcComm(), logicalPath, RemoveOptions.NO_TRASH);
		}
	}

	@Test
	void testAddSetRemoveMetadataForCollection() throws IOException, IRODSException {
		var collection = '/' + String.join("/", zone, "home", username);
		var attrName = "collection_attr_name";
		var attrValue = "collection_attr_value";

		// Show that the collection has no metadata attached to it.
		var query = String.format("select COLL_NAME where META_COLL_ATTR_NAME = '%s' and META_COLL_ATTR_VALUE = '%s'", attrName, attrValue);
		assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());

		// Attach metadata to the collection.
		IRODSMetadata.addMetadataToCollection(conn.getRcComm(), collection, attrName, attrValue, Optional.empty());
		var rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertFalse(rows.isEmpty());
		assertFalse(rows.get(0).isEmpty());
		assertEquals(collection, rows.get(0).get(0));

		// Change the attribute value. "Setting" an AVU will cause all attributes sharing the same
		// attribute name to be collapsed to the new AVU. This does not affect other collections.
		attrValue = "new_collection_attr_value";
		IRODSMetadata.setMetadataOnCollection(conn.getRcComm(), collection, attrName, attrValue, Optional.empty());
		// Show that the operation cleared the old AVU.
		rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertTrue(rows.isEmpty());
		// Show that the new AVU is attached to the collection.
		query = String.format("select COLL_NAME where META_COLL_ATTR_NAME = '%s' and META_COLL_ATTR_VALUE = '%s'", attrName, attrValue);
		rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertFalse(rows.isEmpty());
		assertFalse(rows.get(0).isEmpty());
		assertEquals(collection, rows.get(0).get(0));

		// Remove the recently attached metadata from the collection.
		IRODSMetadata.removeMetadataFromCollection(conn.getRcComm(), collection, attrName, attrValue, Optional.empty());
		assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
	}

	@Test
	void testAddSetRemoveMetadataForCollectionAsAdmin() throws IOException, IRODSException {
		var collection = '/' + String.join("/", zone, "home", username);
		var attrName = "collection_attr_name";
		var attrValue = "collection_attr_value";

		try {
			// Remove the permissions from the collection.
			assertDoesNotThrow(() -> IRODSFilesystem.permissions(conn.getRcComm(), collection, username, Permission.NULL));
			assertTrue(IRODSFilesystem.status(conn.getRcComm(), collection).getPermissions().isEmpty());

			// Show that the collection has no metadata attached to it.
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			var query = String.format("select COLL_NAME where META_COLL_ATTR_NAME = '%s' and META_COLL_ATTR_VALUE = '%s'", attrName, attrValue);
//			assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
			var queryArgs = new IRODSQuery.GenQuery1QueryArgs();
			queryArgs.addColumnToSelectClause(GenQuery1Columns.COL_COLL_NAME);
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_COLL_ATTR_NAME, String.format("= '%s'", attrName));
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_COLL_ATTR_VALUE, String.format("= '%s'", attrValue));
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				fail("Collection should not have any metadata attached to it.");
				return false;
			});

			// Attach metadata to the collection.
			IRODSMetadata.addMetadataToCollection(IRODSMetadata.asAdmin, conn.getRcComm(), collection, attrName, attrValue, Optional.empty());
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			var rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
//			assertFalse(rows.isEmpty());
//			assertFalse(rows.get(0).isEmpty());
//			assertEquals(collection, rows.get(0).get(0));
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				assertEquals(collection, row.get(0));
				return false;
			});

			// Change the attribute value. "Setting" an AVU will cause all attributes sharing the same
			// attribute name to be collapsed to the new AVU. This does not affect other collections.
			attrValue = "new_collection_attr_value";
			IRODSMetadata.setMetadataOnCollection(IRODSMetadata.asAdmin, conn.getRcComm(), collection, attrName, attrValue, Optional.empty());
			// Show that the operation cleared the old AVU.
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
//			assertTrue(rows.isEmpty());
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				fail("Found previous AVU attached to collection.");
				return false;
			});
			// Show that the new AVU is attached to the collection.
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			query = String.format("select COLL_NAME where META_COLL_ATTR_NAME = '%s' and META_COLL_ATTR_VALUE = '%s'", attrName, attrValue);
//			rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
//			assertFalse(rows.isEmpty());
//			assertFalse(rows.get(0).isEmpty());
//			assertEquals(collection, rows.get(0).get(0));
			queryArgs.init(); // Reset the object state.
			queryArgs.addColumnToSelectClause(GenQuery1Columns.COL_COLL_NAME);
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_COLL_ATTR_NAME, String.format("= '%s'", attrName));
			queryArgs.addConditionToWhereClause(GenQuery1Columns.COL_META_COLL_ATTR_VALUE, String.format("= '%s'", attrValue));
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				assertEquals(collection, row.get(0));
				return false;
			});

			// Remove the recently attached metadata from the collection.
			IRODSMetadata.removeMetadataFromCollection(IRODSMetadata.asAdmin, conn.getRcComm(), collection, attrName, attrValue, Optional.empty());
			// TODO(irods/irods#8546): Enable these checks once GenQuery2 is fixed. For now, use GenQuery1.
//			assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
			IRODSQuery.executeGenQuery1(conn.getRcComm(), queryArgs, row -> {
				fail("No metadata should be attached to the collection.");
				return false;
			});
		} finally {
			IRODSFilesystem.permissions(IRODSFilesystem.asAdmin, conn.getRcComm(), collection, username, Permission.OWN);
		}
	}

	@Test
	void testAddSetRemoveMetadataForResource() throws IOException, IRODSException {
		var resource = "demoResc";
		var attrName = "resource_attr_name";
		var attrValue = "resource_attr_value";

		// Show that the resource has no metadata attached to it.
		var query = String.format("select RESC_NAME where META_RESC_ATTR_NAME = '%s' and META_RESC_ATTR_VALUE = '%s'", attrName, attrValue);
		assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());

		// Attach metadata to the resource.
		IRODSMetadata.addMetadataToResource(conn.getRcComm(), resource, attrName, attrValue, Optional.empty());
		var rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertFalse(rows.isEmpty());
		assertFalse(rows.get(0).isEmpty());
		assertEquals(resource, rows.get(0).get(0));

		// Change the attribute value. "Setting" an AVU will cause all attributes sharing the same
		// attribute name to be collapsed to the new AVU. This does not affect other resources.
		attrValue = "new_resource_attr_value";
		IRODSMetadata.setMetadataOnResource(conn.getRcComm(), resource, attrName, attrValue, Optional.empty());
		// Show that the operation cleared the old AVU.
		rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertTrue(rows.isEmpty());
		// Show that the new AVU is attached to the resource.
		query = String.format("select RESC_NAME where META_RESC_ATTR_NAME = '%s' and META_RESC_ATTR_VALUE = '%s'", attrName, attrValue);
		rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertFalse(rows.isEmpty());
		assertFalse(rows.get(0).isEmpty());
		assertEquals(resource, rows.get(0).get(0));

		// Remove the recently attached metadata from the resource.
		IRODSMetadata.removeMetadataFromResource(conn.getRcComm(), resource, attrName, attrValue, Optional.empty());
		assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
	}

	@Test
	void testAddSetRemoveMetadataForUserOrGroup() throws IOException, IRODSException {
		var attrName = "user_attr_name";
		var attrValue = "user_attr_value";

		// Show that the user has no metadata attached to it.
		var query = String.format("select USER_NAME where META_USER_ATTR_NAME = '%s' and META_USER_ATTR_VALUE = '%s'", attrName, attrValue);
		assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());

		// Attach metadata to the user.
		IRODSMetadata.addMetadataToUserOrGroup(conn.getRcComm(), username, attrName, attrValue, Optional.empty());
		var rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertFalse(rows.isEmpty());
		assertFalse(rows.get(0).isEmpty());
		assertEquals(username, rows.get(0).get(0));

		// Change the attribute value. "Setting" an AVU will cause all attributes sharing the same
		// attribute name to be collapsed to the new AVU. This does not affect other users.
		attrValue = "new_user_attr_value";
		IRODSMetadata.setMetadataOnUserOrGroup(conn.getRcComm(), username, attrName, attrValue, Optional.empty());
		// Show that the operation cleared the old AVU.
		rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertTrue(rows.isEmpty());
		// Show that the new AVU is attached to the user.
		query = String.format("select USER_NAME where META_USER_ATTR_NAME = '%s' and META_USER_ATTR_VALUE = '%s'", attrName, attrValue);
		rows = IRODSQuery.executeGenQuery2(conn.getRcComm(), query);
		assertFalse(rows.isEmpty());
		assertFalse(rows.get(0).isEmpty());
		assertEquals(username, rows.get(0).get(0));

		// Remove the recently attached metadata from the user.
		IRODSMetadata.removeMetadataFromUserOrGroup(conn.getRcComm(), username, attrName, attrValue, Optional.empty());
		assertTrue(IRODSQuery.executeGenQuery2(conn.getRcComm(), query).isEmpty());
	}

}
