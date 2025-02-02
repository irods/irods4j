package org.irods.irods4j.high_level.vfs;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.high_level.common.AdminTag;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.TransferStat_PI;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * A class providing high-level data types and functions for working with
 * replicas.
 * 
 * @since 0.1.0
 */
public class IRODSReplicas {

	/**
	 * Defines values which influence the behavior of checksum calculations.
	 * 
	 * @since 0.1.0
	 */
	public static enum VerificationCalculation {
		IF_EMPTY, ALWAYS
	}

	/**
	 * Instructs the server to execute operations using rodsadmin level privileges.
	 * 
	 * @since 0.1.0
	 */
	public static final AdminTag asAdmin = AdminTag.instance;

	/**
	 * Returns the size of a replica in the catalog.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to a data object.
	 * @param replicaNumber The replica number of the target replica.
	 * 
	 * @return An integer representing the size.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static long replicaSize(RcComm comm, String logicalPath, long replicaNumber)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select DATA_SIZE where COLL_NAME = '%s' and DATA_NAME = '%s' and DATA_REPL_NUM = '%d'";
		var rows = IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, replicaNumber));
		if (!rows.isEmpty()) {
			throw new IllegalStateException("Replica does not exist");
		}

		return Long.parseLong(rows.get(0).get(0));
	}

	/**
	 * Returns the size of a replica in the catalog.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to a data object.
	 * @param leafResourceName The resource holding the target replica.
	 * 
	 * @return An integer representing the size.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static long replicaSize(RcComm comm, String logicalPath, String leafResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Leaf resource is null or empty");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select DATA_SIZE where COLL_NAME = '%s' and DATA_NAME = '%s' and RESC_NAME = '%s'";
		var rows = IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, leafResourceName));
		if (!rows.isEmpty()) {
			throw new IllegalStateException("Replica does not exist");
		}

		return Long.parseLong(rows.get(0).get(0));
	}

	/**
	 * Returns the physical size of a replica in storage.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to a data object.
	 * @param replicaNumber The replica number of the target replica.
	 * 
	 * @return An integer representing the size.
	 * 
	 * @since 0.1.0
	 */
	public static long replicaSizeInStorage(RcComm comm, String logicalPath, long replicaNumber) {
		throw new UnsupportedOperationException(); // TODO
	}

	/**
	 * Returns the physical size of a replica in storage.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to a data object.
	 * @param leafResourceName The resource holding the target replica.
	 * 
	 * @return An integer representing the size.
	 * 
	 * @since 0.1.0
	 */
	public static long replicaSizeInStorage(RcComm comm, String logicalPath, String leafResourceName) {
		throw new UnsupportedOperationException(); // TODO
	}

	/**
	 * Replicates a replica to a resource.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         source replica.
	 * @param srcReplicaNumber The replica number of the source replica to read
	 *                         from.
	 * @param dstResourceName  The root resource to replicate the data to.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void replicateReplica(AdminTag adminTag, RcComm comm, String logicalPath, int srcReplicaNumber,
			String dstResourceName) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(srcReplicaNumber, 0, "Source replica number is less than 0");
		throwIfNullOrEmpty(dstResourceName, "Destination resource is null or empty");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Use admin privileges.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
		input.KeyValPair_PI.svalue.add("");
		// Source replica number.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPL_NUM);
		input.KeyValPair_PI.svalue.add(String.valueOf(srcReplicaNumber));
		// Destination resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.DEST_RESC_NAME);
		input.KeyValPair_PI.svalue.add(dstResourceName);

		var output = new Reference<TransferStat_PI>();

		var ec = IRODSApi.rcDataObjRepl(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjRepl error");
		}
	}

	/**
	 * Replicates a replica to a resource.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         source replica.
	 * @param srcReplicaNumber The replica number of the source replica to read
	 *                         from.
	 * @param dstResourceName  The root resource to replicate the data to.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void replicateReplica(RcComm comm, String logicalPath, int srcReplicaNumber, String dstResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(srcReplicaNumber, 0, "Source replica number is less than 0");
		throwIfNullOrEmpty(dstResourceName, "Destination resource is null or empty");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Source replica number.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPL_NUM);
		input.KeyValPair_PI.svalue.add(String.valueOf(srcReplicaNumber));
		// Destination resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.DEST_RESC_NAME);
		input.KeyValPair_PI.svalue.add(dstResourceName);

		var output = new Reference<TransferStat_PI>();

		var ec = IRODSApi.rcDataObjRepl(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjRepl error");
		}
	}

	/**
	 * Replicates a replica to a resource.
	 * 
	 * @param comm            The connection to an iRODS server.
	 * @param logicalPath     The absolute path to the data object which owns the
	 *                        source replica.
	 * @param srcResourceName The resource of the source replica to read from.
	 * @param dstResourceName The root resource to replicate the data to.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void replicateReplica(AdminTag adminTag, RcComm comm, String logicalPath, String srcResourceName,
			String dstResourceName) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(srcResourceName, "Source resource is null or empty");
		throwIfNullOrEmpty(dstResourceName, "Destination resource is null or empty");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Use admin privileges.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
		input.KeyValPair_PI.svalue.add("");
		// Source replica number.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.RESC_NAME);
		input.KeyValPair_PI.svalue.add(String.valueOf(srcResourceName));
		// Destination resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.DEST_RESC_NAME);
		input.KeyValPair_PI.svalue.add(dstResourceName);

		var output = new Reference<TransferStat_PI>();

		var ec = IRODSApi.rcDataObjRepl(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjRepl error");
		}
	}

	/**
	 * Replicates a replica to a resource.
	 * 
	 * @param comm            The connection to an iRODS server.
	 * @param logicalPath     The absolute path to the data object which owns the
	 *                        source replica.
	 * @param srcResourceName The resource of the source replica to read from.
	 * @param dstResourceName The root resource to replicate the data to.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void replicateReplica(RcComm comm, String logicalPath, String srcResourceName, String dstResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(srcResourceName, "Source resource is null or empty");
		throwIfNullOrEmpty(dstResourceName, "Destination resource is null or empty");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Source resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.RESC_NAME);
		input.KeyValPair_PI.svalue.add(String.valueOf(srcResourceName));
		// Destination resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.DEST_RESC_NAME);
		input.KeyValPair_PI.svalue.add(dstResourceName);

		var output = new Reference<TransferStat_PI>();

		var ec = IRODSApi.rcDataObjRepl(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjRepl error");
		}
	}

	/**
	 * Trims a replica.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      target replica.
	 * @param replicaNumber The replica number of the replica to trim.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void trimReplica(AdminTag adminTag, RcComm comm, String logicalPath, long replicaNumber)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Use admin privileges.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
		input.KeyValPair_PI.svalue.add("");
		// Replica number.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPL_NUM);
		input.KeyValPair_PI.svalue.add(String.valueOf(replicaNumber));
		// The minimum number of replicas to keep.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.COPIES);
		input.KeyValPair_PI.svalue.add("1");

		var ec = IRODSApi.rcDataObjTrim(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjTrim error");
		}
	}

	/**
	 * Trims a replica.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      target replica.
	 * @param replicaNumber The replica number of the replica to trim.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void trimReplica(RcComm comm, String logicalPath, long replicaNumber)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Replica number.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPL_NUM);
		input.KeyValPair_PI.svalue.add(String.valueOf(replicaNumber));
		// The minimum number of replicas to keep.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.COPIES);
		input.KeyValPair_PI.svalue.add("1");

		var ec = IRODSApi.rcDataObjTrim(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjTrim error");
		}
	}

	/**
	 * Trims a replica.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         target replica.
	 * @param leafResourceName The resource holding the replica to trim.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void trimReplica(AdminTag adminTag, RcComm comm, String logicalPath, String leafResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Resource is null or empty");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Use admin privileges.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
		input.KeyValPair_PI.svalue.add("");
		// Resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.RESC_NAME);
		input.KeyValPair_PI.svalue.add(leafResourceName);
		// The minimum number of replicas to keep.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.COPIES);
		input.KeyValPair_PI.svalue.add("1");

		var ec = IRODSApi.rcDataObjTrim(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjTrim error");
		}
	}

	/**
	 * Trims a replica.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         target replica.
	 * @param leafResourceName The resource holding the replica to trim.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void trimReplica(RcComm comm, String logicalPath, String leafResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Resource is null or empty");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.RESC_NAME);
		input.KeyValPair_PI.svalue.add(leafResourceName);
		// The minimum number of replicas to keep.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.COPIES);
		input.KeyValPair_PI.svalue.add("1");

		var ec = IRODSApi.rcDataObjTrim(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjTrim error");
		}
	}

	/**
	 * Optionally calculates and returns the checksum of a replica.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      source replica.
	 * @param replicaNumber The replica number of the replica to checksum.
	 * @param calculation   An option which influences the behavior of the checksum
	 *                      operation.
	 * 
	 * @return A string representing the checksum.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static String replicaChecksum(AdminTag adminTag, RcComm comm, String logicalPath, long replicaNumber,
			VerificationCalculation calculation) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");
		throwIfNull(calculation, "Checksum verification calculation is null");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Use admin privileges.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
		input.KeyValPair_PI.svalue.add("");
		// Replica number.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPL_NUM);
		input.KeyValPair_PI.svalue.add(String.valueOf(replicaNumber));

		if (VerificationCalculation.ALWAYS == calculation) {
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
			input.KeyValPair_PI.svalue.add("");
		}

		var output = new Reference<String>();

		var ec = IRODSApi.rcDataObjChksum(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjChksum error");
		}

		return output.value;
	}

	/**
	 * Optionally calculates and returns the checksum of a replica.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      source replica.
	 * @param replicaNumber The replica number of the replica to checksum.
	 * @param calculation   An option which influences the behavior of the checksum
	 *                      operation.
	 * 
	 * @return A string representing the checksum.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static String replicaChecksum(RcComm comm, String logicalPath, long replicaNumber,
			VerificationCalculation calculation) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");
		throwIfNull(calculation, "Checksum verification calculation is null");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Replica number.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPL_NUM);
		input.KeyValPair_PI.svalue.add(String.valueOf(replicaNumber));

		if (VerificationCalculation.ALWAYS == calculation) {
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
			input.KeyValPair_PI.svalue.add("");
		}

		var output = new Reference<String>();

		var ec = IRODSApi.rcDataObjChksum(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjChksum error");
		}

		return output.value;
	}

	/**
	 * Optionally calculates and returns the checksum of a replica.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         source replica.
	 * @param leafResourceName The resource holding the target replica.
	 * @param calculation      An option which influences the behavior of the
	 *                         checksum operation.
	 * 
	 * @return A string representing the checksum.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static String replicaChecksum(AdminTag adminTag, RcComm comm, String logicalPath, String leafResourceName,
			VerificationCalculation calculation) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Destination resource is null or empty");
		throwIfNull(calculation, "Checksum verification calculation is null");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Use admin privileges.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
		input.KeyValPair_PI.svalue.add("");
		// Resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.RESC_NAME);
		input.KeyValPair_PI.svalue.add(leafResourceName);

		if (VerificationCalculation.ALWAYS == calculation) {
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
			input.KeyValPair_PI.svalue.add("");
		}

		var output = new Reference<String>();

		var ec = IRODSApi.rcDataObjChksum(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjChksum error");
		}

		return output.value;
	}

	/**
	 * Optionally calculates and returns the checksum of a replica.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         source replica.
	 * @param leafResourceName The resource holding the target replica.
	 * @param calculation      An option which influences the behavior of the
	 *                         checksum operation.
	 * 
	 * @return A string representing the checksum.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static String replicaChecksum(RcComm comm, String logicalPath, String leafResourceName,
			VerificationCalculation calculation) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Destination resource is null or empty");
		throwIfNull(calculation, "Checksum verification calculation is null");

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		// Resource name.
		++input.KeyValPair_PI.ssLen;
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.RESC_NAME);
		input.KeyValPair_PI.svalue.add(leafResourceName);

		if (VerificationCalculation.ALWAYS == calculation) {
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
			input.KeyValPair_PI.svalue.add("");
		}

		var output = new Reference<String>();

		var ec = IRODSApi.rcDataObjChksum(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjChksum error");
		}

		return output.value;
	}

	/**
	 * Returns the modification time of a replica.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      replica.
	 * @param replicaNumber The replica number of the target replica.
	 * 
	 * @return An integer representing the time.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static long lastWriteTime(RcComm comm, String logicalPath, long replicaNumber)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select DATA_MODIFY_TIME where COLL_NAME = '%s' and DATA_NAME = '%s' and DATA_REPL_NUM = '%d'";
		var rows = IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, replicaNumber));
		if (!rows.isEmpty()) {
			throw new IllegalStateException("Replica does not exist");
		}

		return Long.parseLong(rows.get(0).get(0));
	}

	/**
	 * Returns the modification time of a replica.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         replica.
	 * @param leafResourceName The resource holding the target replica.
	 * 
	 * @return An integer representing the time.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static long lastWriteTime(RcComm comm, String logicalPath, String leafResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Resource is null or empty");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select DATA_MODIFY_TIME where COLL_NAME = '%s' and DATA_NAME = '%s' and RESC_NAME = '%s'";
		var rows = IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, leafResourceName));
		if (!rows.isEmpty()) {
			throw new IllegalStateException("Replica does not exist");
		}

		return Long.parseLong(rows.get(0).get(0));
	}

	/**
	 * Sets the modification time of a replica.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      replica.
	 * @param replicaNumber The replica number of the target replica.
	 * @param newTime       The new modification time in seconds since epoch.
	 * 
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void lastWriteTime(RcComm comm, String logicalPath, long replicaNumber, long newTime)
			throws JsonProcessingException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");
		throwIfLessThanLowerBound(newTime, 0, "Modification time is less than 0");

		var input = new HashMap<String, Object>();
		input.put("logical_path", logicalPath);

		var options = new HashMap<String, Object>();
		input.put("options", options);

		options.put("no_create", true); // Do not create new data objects!
		options.put("replica_number", replicaNumber);
		options.put("seconds_since_epoch", newTime);

		var ec = IRODSApi.rcTouch(comm, JsonUtil.toJsonString(input));
		if (ec < 0) {
			throw new IRODSException(ec, "rcTouch error");
		}
	}

	/**
	 * Sets the modification time of a replica.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         replica.
	 * @param leafResourceName The resource holding the target replica.
	 * @param newTime          The new modification time in seconds since epoch.
	 * 
	 * @throws IOException
	 * @throws JsonProcessingException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void lastWriteTime(RcComm comm, String logicalPath, String leafResourceName, long newTime)
			throws JsonProcessingException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Resource is null or empty");
		throwIfLessThanLowerBound(newTime, 0, "Modification time is less than 0");

		var input = new HashMap<String, Object>();
		input.put("logical_path", logicalPath);

		var options = new HashMap<String, Object>();
		input.put("options", options);

		options.put("no_create", true); // Do not create new data objects!
		options.put("leaf_resource_name", leafResourceName);
		options.put("seconds_since_epoch", newTime);

		var ec = IRODSApi.rcTouch(comm, JsonUtil.toJsonString(input));
		if (ec < 0) {
			throw new IRODSException(ec, "rcTouch error");
		}
	}

	/**
	 * Returns the replica number of a replica.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         replica.
	 * @param leafResourceName The resource holding the target replica.
	 * 
	 * @return An integer representing the replica number.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static long toReplicaNumber(RcComm comm, String logicalPath, String leafResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Resource is null or empty");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select DATA_REPL_NUM where COLL_NAME = '%s' and DATA_NAME = '%s' and RESC_NAME = '%s'";
		var rows = IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, leafResourceName));
		if (!rows.isEmpty()) {
			throw new IllegalStateException("Replica does not exist");
		}

		return Long.parseLong(rows.get(0).get(0));
	}

	/**
	 * Returns the name of the leaf resource which holds a replica.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      replica.
	 * @param replicaNumber The replica number of the target replica.
	 * 
	 * @return A string representing the resource name.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static String toLeafResource(RcComm comm, String logicalPath, long replicaNumber)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select RESC_NAME where COLL_NAME = '%s' and DATA_NAME = '%s' and DATA_REPL_NUM = '%d'";
		var rows = IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, replicaNumber));
		if (!rows.isEmpty()) {
			throw new IllegalStateException("Replica does not exist");
		}

		return rows.get(0).get(0);
	}

	/**
	 * Checks if a replica exists in the catalog.
	 * 
	 * @param comm          The connection to an iRODS server.
	 * @param logicalPath   The absolute path to the data object which owns the
	 *                      replica.
	 * @param replicaNumber The replica number of the target replica.
	 * 
	 * @return A boolean indicating whether the target replica exists.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static boolean replicaExists(RcComm comm, String logicalPath, long replicaNumber)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfLessThanLowerBound(replicaNumber, 0, "Replica number is less than 0");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select DATA_ID where COLL_NAME = '%s' and DATA_NAME = '%s' and DATA_REPL_NUM = '%d'";
		return !IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, replicaNumber)).isEmpty();
	}

	/**
	 * Checks if a replica exists in the catalog.
	 * 
	 * @param comm             The connection to an iRODS server.
	 * @param logicalPath      The absolute path to the data object which owns the
	 *                         replica.
	 * @param leafResourceName The resource holding the target replica.
	 * 
	 * @return A boolean indicating whether the target replica exists.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static boolean replicaExists(RcComm comm, String logicalPath, String leafResourceName)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfNullOrEmpty(leafResourceName, "Resource is null or empty");

		var p = Paths.get(logicalPath);
		var collName = p.getParent().toString();
		var dataName = p.getFileName().toString();

		var query = "select DATA_ID where COLL_NAME = '%s' and DATA_NAME = '%s' and RESC_NAME = '%s'";
		return !IRODSQuery.executeGenQuery2(comm, String.format(query, collName, dataName, leafResourceName)).isEmpty();
	}

	private static void throwIfNull(Object object, String msg) {
		if (null == object) {
			throw new IllegalArgumentException(msg);
		}
	}

	private static void throwIfNullOrEmpty(String s, String msg) {
		if (null == s || s.isEmpty()) {
			throw new IllegalArgumentException(msg);
		}
	}

	private static void throwIfLessThanLowerBound(long value, long lowerBound, String msg) {
		if (value < lowerBound) {
			throw new IllegalArgumentException(msg);
		}
	}

}
