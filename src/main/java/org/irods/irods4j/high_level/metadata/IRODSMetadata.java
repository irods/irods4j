package org.irods.irods4j.high_level.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.high_level.common.AdminTag;
import org.irods.irods4j.input_validation.Preconditions;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ModAVUMetadataInp_PI;

/**
 * A high-level class providing functions for modifying metadata on various
 * iRODS entities.
 * 
 * @since 0.1.0
 */
public class IRODSMetadata {

	/**
	 * Instructs the server to execute operations using rodsadmin level privileges.
	 * 
	 * @since 0.1.0
	 */
	public static final AdminTag asAdmin = AdminTag.instance;

	private static enum OneOffMetadataOperation {
		ADD("add"), REMOVE("rm"), SET("set");

		String value;

		OneOffMetadataOperation(String v) {
			value = v;
		}
	}

	private static void doOneOffMetadataOperationOnLogicalPath(boolean asAdmin, RcComm comm, OneOffMetadataOperation op,
			boolean isCollection, String logicalPath, String attrName, String attrValue, Optional<String> attrUnits)
			throws IOException, IRODSException {
		Preconditions.notNull(comm, "RcComm is null");
		Preconditions.notNullOrEmpty(logicalPath, "Logical path is null or empty");
		Preconditions.notNullOrEmpty(attrName, "Metadata attribute name is null or empty");
		Preconditions.notNullOrEmpty(attrValue, "Metadata attribute value is null or empty");
		attrUnits.ifPresent(v -> Preconditions.notNullOrEmpty(v, "Metadata attribute units is null or empty"));

		var input = new ModAVUMetadataInp_PI();
		input.arg0 = op.value;
		input.arg1 = isCollection ? "-C" : "-d";
		input.arg2 = logicalPath;
		input.arg3 = attrName;
		input.arg4 = attrValue;
		input.arg5 = attrUnits.orElse("");

		if (asAdmin) {
			input.KeyValPair_PI = new KeyValPair_PI();
			input.KeyValPair_PI.ssLen = 1;
			input.KeyValPair_PI.keyWord = new ArrayList<>();
			input.KeyValPair_PI.svalue = new ArrayList<>();
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
			input.KeyValPair_PI.svalue.add("");
		}

		var ec = IRODSApi.rcModAVUMetadata(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcModAVUMetadata error");
		}
	}

	/**
	 * Adds a single metadata triple to a collection.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addMetadataToCollection(RcComm comm, String logicalPath, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = false;
		final var op = OneOffMetadataOperation.ADD;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Adds a single metadata triple to a collection using rodsadmin privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addMetadataToCollection(AdminTag adminTag, RcComm comm, String logicalPath, String attrName,
			String attrValue, Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = true;
		final var op = OneOffMetadataOperation.ADD;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Removes a single metadata triple from a collection.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeMetadataFromCollection(RcComm comm, String logicalPath, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = false;
		final var op = OneOffMetadataOperation.REMOVE;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Removes a single metadata triple from a collection using rodsadmin
	 * privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeMetadataFromCollection(AdminTag adminTag, RcComm comm, String logicalPath, String attrName,
			String attrValue, Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = true;
		final var op = OneOffMetadataOperation.REMOVE;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Sets a single metadata triple on a collection.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void setMetadataOnCollection(RcComm comm, String logicalPath, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = false;
		final var op = OneOffMetadataOperation.SET;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Sets a single metadata triple on a collection using rodsadmin privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void setMetadataOnCollection(AdminTag adminTag, RcComm comm, String logicalPath, String attrName,
			String attrValue, Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = true;
		final var op = OneOffMetadataOperation.SET;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Adds a single metadata triple to a data object.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addMetadataToDataObject(RcComm comm, String logicalPath, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = false;
		final var op = OneOffMetadataOperation.ADD;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Adds a single metadata triple to a data object using rodsadmin privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addMetadataToDataObject(AdminTag adminTag, RcComm comm, String logicalPath, String attrName,
			String attrValue, Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = true;
		final var op = OneOffMetadataOperation.ADD;
		final var isCollection = true;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Removes a single metadata triple from a data object.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeMetadataFromDataObject(RcComm comm, String logicalPath, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = false;
		final var op = OneOffMetadataOperation.REMOVE;
		final var isCollection = false;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Removes a single metadata triple from a data object using rodsadmin
	 * privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeMetadataFromDataObject(AdminTag adminTag, RcComm comm, String logicalPath, String attrName,
			String attrValue, Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = true;
		final var op = OneOffMetadataOperation.REMOVE;
		final var isCollection = false;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Sets a single metadata triple on a data object.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void setMetadataOnDataObject(RcComm comm, String logicalPath, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = false;
		final var op = OneOffMetadataOperation.SET;
		final var isCollection = false;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Sets a single metadata triple on a data object using rodsadmin privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void setMetadataOnDataObject(AdminTag adminTag, RcComm comm, String logicalPath, String attrName,
			String attrValue, Optional<String> attrUnits) throws IOException, IRODSException {
		final var asAdmin = true;
		final var op = OneOffMetadataOperation.SET;
		final var isCollection = false;
		doOneOffMetadataOperationOnLogicalPath(asAdmin, comm, op, isCollection, logicalPath, attrName, attrValue,
				attrUnits);
	}

	private static void doOneOffMetadataOperationOnResource(RcComm comm, String resourceName,
			OneOffMetadataOperation op, String attrName, String attrValue, Optional<String> attrUnits)
			throws IOException, IRODSException {
		Preconditions.notNull(comm, "RcComm is null");
		Preconditions.notNullOrEmpty(resourceName, "Resource name is null or empty");
		Preconditions.notNullOrEmpty(attrName, "Metadata attribute name is null or empty");
		Preconditions.notNullOrEmpty(attrValue, "Metadata attribute value is null or empty");
		attrUnits.ifPresent(v -> Preconditions.notNullOrEmpty(v, "Metadata attribute units is null or empty"));

		var input = new ModAVUMetadataInp_PI();
		input.arg0 = op.value;
		input.arg1 = "-R";
		input.arg2 = resourceName;
		input.arg3 = attrName;
		input.arg4 = attrValue;
		input.arg5 = attrUnits.orElse("");

		var ec = IRODSApi.rcModAVUMetadata(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcModAVUMetadata error");
		}
	}

	/**
	 * Adds a single metadata triple to a resource.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm         The connection to an iRODS server.
	 * @param resourceName The name of a resource.
	 * @param attrName     The metadata attribute name.
	 * @param attrValue    The metadata attribute value.
	 * @param attrUnits    The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addMetadataToResource(RcComm comm, String resourceName, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		doOneOffMetadataOperationOnResource(comm, resourceName, OneOffMetadataOperation.ADD, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Removes a single metadata triple from a resource.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm         The connection to an iRODS server.
	 * @param resourceName The name of a resource.
	 * @param attrName     The metadata attribute name.
	 * @param attrValue    The metadata attribute value.
	 * @param attrUnits    The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeMetadataFromResource(RcComm comm, String resourceName, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		doOneOffMetadataOperationOnResource(comm, resourceName, OneOffMetadataOperation.REMOVE, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Sets a single metadata triple on a resource.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm         The connection to an iRODS server.
	 * @param resourceName The name of a resource.
	 * @param attrName     The metadata attribute name.
	 * @param attrValue    The metadata attribute value.
	 * @param attrUnits    The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void setMetadataOnResource(RcComm comm, String resourceName, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		doOneOffMetadataOperationOnResource(comm, resourceName, OneOffMetadataOperation.SET, attrName, attrValue,
				attrUnits);
	}

	private static void doOneOffMetadataOperationOnUserOrGroup(RcComm comm, String userOrGroup,
			OneOffMetadataOperation op, String attrName, String attrValue, Optional<String> attrUnits)
			throws IOException, IRODSException {
		Preconditions.notNull(comm, "RcComm is null");
		Preconditions.notNullOrEmpty(userOrGroup, "User/Group name is null or empty");
		Preconditions.notNullOrEmpty(attrName, "Metadata attribute name is null or empty");
		Preconditions.notNullOrEmpty(attrValue, "Metadata attribute value is null or empty");
		attrUnits.ifPresent(v -> Preconditions.notNullOrEmpty(v, "Metadata attribute units is null or empty"));

		var input = new ModAVUMetadataInp_PI();
		input.arg0 = op.value;
		input.arg1 = "-u";
		input.arg2 = userOrGroup;
		input.arg3 = attrName;
		input.arg4 = attrValue;
		input.arg5 = attrUnits.orElse("");

		var ec = IRODSApi.rcModAVUMetadata(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcModAVUMetadata error");
		}
	}

	/**
	 * Adds a single metadata triple to a user or group.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param userOrGroup The name of a user or group.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addMetadataToUserOrGroup(RcComm comm, String userOrGroup, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		doOneOffMetadataOperationOnUserOrGroup(comm, userOrGroup, OneOffMetadataOperation.ADD, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Removes a single metadata triple from a user or group.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param userOrGroup The name of a user or group.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeMetadataFromUserOrGroup(RcComm comm, String userOrGroup, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		doOneOffMetadataOperationOnUserOrGroup(comm, userOrGroup, OneOffMetadataOperation.REMOVE, attrName, attrValue,
				attrUnits);
	}

	/**
	 * Sets a single metadata triple on a user or group.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm        The connection to an iRODS server.
	 * @param userOrGroup The name of a user or group.
	 * @param attrName    The metadata attribute name.
	 * @param attrValue   The metadata attribute value.
	 * @param attrUnits   The metadata attribute units.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void setMetadataOnUserOrGroup(RcComm comm, String userOrGroup, String attrName, String attrValue,
			Optional<String> attrUnits) throws IOException, IRODSException {
		doOneOffMetadataOperationOnUserOrGroup(comm, userOrGroup, OneOffMetadataOperation.SET, attrName, attrValue,
				attrUnits);
	}

	/**
	 * The atomic metadata operations supported by iRODS.
	 * 
	 * @since 0.1.0
	 */
	public static enum AtomicOperation {
		ADD("add"), REMOVE("remove");

		String value;

		AtomicOperation(String v) {
			value = v;
		}
	}

	/**
	 * A class which represents a single atomic metadata operation to execute.
	 * 
	 * @since 0.1.0
	 */
	public static final class AtomicMetadataOperation {

		String operation;
		String attribute;
		String value;
		String units;

		/**
		 * Initializes a instance without attribute units.
		 * 
		 * @param op        The metadata operation to perform.
		 * @param attrName  The metadata attribute name.
		 * @param attrValue The metadata attribute value.
		 * 
		 * @since 0.1.0
		 */
		public AtomicMetadataOperation(AtomicOperation op, String attrName, String attrValue) {
			Preconditions.notNull(op, "Atomic operation is null");
			Preconditions.notNullOrEmpty(attrName, "Metadata attribute name is null or empty");
			Preconditions.notNullOrEmpty(attrValue, "Metadata attribute value is null or empty");

			operation = op.value;
			attribute = attrName;
			value = attrValue;
			units = "";
		}

		/**
		 * Initializes an instance with attribute units.
		 * 
		 * @param op        The metadata operation to perform.
		 * @param attrName  The metadata attribute name.
		 * @param attrValue The metadata attribute value.
		 * @param attrUnits The metadata attribute units.
		 * 
		 * @since 0.1.0
		 */
		public AtomicMetadataOperation(AtomicOperation op, String attrName, String attrValue, String attrUnits) {
			Preconditions.notNull(op, "Atomic operation is null");
			Preconditions.notNullOrEmpty(attrName, "Metadata attribute name is null or empty");
			Preconditions.notNullOrEmpty(attrValue, "Metadata attribute value is null or empty");
			Preconditions.notNullOrEmpty(attrUnits, "Metadata attribute units is null or empty");

			operation = op.value;
			attribute = attrName;
			value = attrValue;
			units = attrUnits;
		}

	}

	/**
	 * Holds the results of an atomic metadata API operation.
	 * 
	 * @since 0.1.0
	 */
	public static final class AtomicMetadataOperationsResult {
		public int errorCode;
		public String jsonOutput;

		AtomicMetadataOperationsResult() {
		}
	}

	private static AtomicMetadataOperationsResult atomicApplyMetadataOperationsToLogicalPathImpl(boolean asAdmin,
			RcComm comm, String logicalPath, String entityType, List<AtomicMetadataOperation> operations)
			throws IOException {
		Preconditions.notNull(comm, "RcComm is null");
		Preconditions.notNullOrEmpty(logicalPath, "Logical path is null or empty");
		Preconditions.greaterThanOrEqualToValue(operations.size(), 0, "Atomic metadata operations is null or empty");

		var errorInfo = new AtomicMetadataOperationsResult();

		var inputStruct = new HashMap<String, Object>();
		inputStruct.put("admin_mode", asAdmin);
		inputStruct.put("entity_name", logicalPath);
		inputStruct.put("entity_type", entityType);
		inputStruct.put("operations", operations);

		var input = JsonUtil.toJsonString(inputStruct);
		var output = new Reference<String>();

		errorInfo.errorCode = IRODSApi.rcAtomicApplyMetadataOperations(comm, input, output);
		if (errorInfo.errorCode < 0) {
			errorInfo.jsonOutput = output.value;
		}

		return errorInfo;
	}

	/**
	 * Sequentially executes all metadata operations on a collection atomically
	 * using rodsadmin privileges.
	 * 
	 * @param adminTag    Instructs the server to execute the operation using
	 *                    rodsadmin privileges.
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param operations  The list of metadata operations.
	 * 
	 * @return An object containing error information.
	 * 
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static AtomicMetadataOperationsResult atomicApplyMetadataOperationsToCollection(AdminTag adminTag,
			RcComm comm, String logicalPath, List<AtomicMetadataOperation> operations) throws IOException {
		return atomicApplyMetadataOperationsToLogicalPathImpl(true, comm, logicalPath, "collection", operations);
	}

	/**
	 * Sequentially executes all metadata operations on a collection atomically.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a collection.
	 * @param operations  The list of metadata operations.
	 * 
	 * @return An object containing error information.
	 * 
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static AtomicMetadataOperationsResult atomicApplyMetadataOperationsToCollection(RcComm comm,
			String logicalPath, List<AtomicMetadataOperation> operations) throws IOException {
		return atomicApplyMetadataOperationsToLogicalPathImpl(false, comm, logicalPath, "collection", operations);
	}

	/**
	 * Sequentially executes all metadata operations on a data object atomically
	 * using rodsadmin privileges.
	 * 
	 * @param adminTag    Instructs the server to execute the operation using
	 *                    rodsadmin privileges.
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param operations  The list of metadata operations.
	 * 
	 * @return An object containing error information.
	 * 
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static AtomicMetadataOperationsResult atomicApplyMetadataOperationsToDataObject(AdminTag adminTag,
			RcComm comm, String logicalPath, List<AtomicMetadataOperation> operations) throws IOException {
		return atomicApplyMetadataOperationsToLogicalPathImpl(true, comm, logicalPath, "data_object", operations);
	}

	/**
	 * Sequentially executes all metadata operations on a data object atomically.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param operations  The list of metadata operations.
	 * 
	 * @return An object containing error information.
	 * 
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static AtomicMetadataOperationsResult atomicApplyMetadataOperationsToDataObject(RcComm comm,
			String logicalPath, List<AtomicMetadataOperation> operations) throws IOException {
		return atomicApplyMetadataOperationsToLogicalPathImpl(false, comm, logicalPath, "data_object", operations);
	}

	/**
	 * Sequentially executes all metadata operations on a resource atomically.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm         The connection to the iRODS server.
	 * @param resourceName The name of a resource.
	 * @param operations   The list of metadata operations.
	 * 
	 * @return An object containing error information.
	 * 
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static AtomicMetadataOperationsResult atomicApplyMetadataOperationsToResource(RcComm comm,
			String resourceName, List<AtomicMetadataOperation> operations) throws IOException {
		Preconditions.notNull(comm, "RcComm is null");
		Preconditions.notNullOrEmpty(resourceName, "Resource name is null or empty");
		Preconditions.greaterThanOrEqualToValue(operations.size(), 0, "Atomic metadata operations is empty");

		var errorInfo = new AtomicMetadataOperationsResult();

		var inputStruct = new HashMap<String, Object>();
		inputStruct.put("admin_mode", false);
		inputStruct.put("entity_name", resourceName);
		inputStruct.put("entity_type", "resource");
		inputStruct.put("operations", operations);

		var input = JsonUtil.toJsonString(inputStruct);
		var output = new Reference<String>();

		errorInfo.errorCode = IRODSApi.rcAtomicApplyMetadataOperations(comm, input, output);
		if (errorInfo.errorCode < 0) {
			errorInfo.jsonOutput = output.value;
		}

		return errorInfo;
	}

	/**
	 * Sequentially executes all metadata operations on a user or group atomically.
	 * <p>
	 * This operation requires rodsadmin level privileges.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param userOrGroup The name of a user or group.
	 * @param operations  The list of metadata operations.
	 * 
	 * @return An object containing error information.
	 * 
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static AtomicMetadataOperationsResult atomicApplyMetadataOperationsToUserOrGroup(RcComm comm,
			String userOrGroup, List<AtomicMetadataOperation> operations) throws IOException {
		Preconditions.notNull(comm, "RcComm is null");
		Preconditions.notNullOrEmpty(userOrGroup, "User/Group name is null or empty");
		Preconditions.greaterThanOrEqualToValue(operations.size(), 0, "Atomic metadata operations is empty");

		var errorInfo = new AtomicMetadataOperationsResult();

		var inputStruct = new HashMap<String, Object>();
		inputStruct.put("admin_mode", false);
		inputStruct.put("entity_name", userOrGroup);
		inputStruct.put("entity_type", "user");
		inputStruct.put("operations", operations);

		var input = JsonUtil.toJsonString(inputStruct);
		var output = new Reference<String>();

		errorInfo.errorCode = IRODSApi.rcAtomicApplyMetadataOperations(comm, input, output);
		if (errorInfo.errorCode < 0) {
			errorInfo.jsonOutput = output.value;
		}

		return errorInfo;
	}

}
