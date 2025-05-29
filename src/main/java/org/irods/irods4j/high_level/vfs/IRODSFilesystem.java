package org.irods.irods4j.high_level.vfs;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.high_level.administration.IRODSUsers;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.high_level.common.AdminTag;
import org.irods.irods4j.high_level.vfs.ObjectStatus.ObjectType;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.protocol.packing_instructions.CollInpNew_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.CollOprStat_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjCopyInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ModAccessControlInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RodsObjStat_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.TransferStat_PI;

/**
 * A class providing high-level functions for working with collections and data
 * objects.
 * 
 * @since 0.1.0
 */
public class IRODSFilesystem {

	private static final Logger log = LogManager.getLogger();

	/**
	 * Defines values which influence the behavior of a remove operation.
	 * 
	 * @since 0.1.0
	 */
	public static enum RemoveOptions {
		NONE, NO_TRASH
	}

	/**
	 * Defines bitmask values which influence the behavior of a copy operation.
	 * 
	 * @since 0.1.0
	 */
	public static final class CopyOptions {
		public static final int NONE = 0;
		public static final int SKIP_EXISTING = 1;
		public static final int OVERWRITE_EXISTING = 2;
		public static final int UPDATE_EXISTING = 4;
		public static final int RECURSIVE = 8;
		public static final int COLLECTIONS_ONLY = 16;

		// Reserved for the implementation.
		private static final int IN_RECURSIVE_COPY = 32;
	}

	/**
	 * Instructs the server to execute operations using rodsadmin level privileges.
	 * 
	 * @since 0.1.0
	 */
	public static final AdminTag asAdmin = AdminTag.instance;

	/**
	 * 
	 * @param comm
	 * @param from
	 * @param to
	 * @param copyOptions
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void copy(RcComm comm, String from, String to, int copyOptions)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(from, "From/Source path is null or empty");
		throwIfNullOrEmpty(to, "To/Destination path is null or empty");
		throwIfPathLengthExceedsLimit(from);
		throwIfPathLengthExceedsLimit(to);

		var fromStatus = status(comm, from);
		if (!exists(fromStatus)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.OBJ_PATH_DOES_NOT_EXIST, from,
					"From/Source path does not exist");
		}

		var toStatus = status(comm, to);
		if (exists(toStatus) && equivalent(comm, from, to)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.SAME_SRC_DEST_PATHS_ERR, to,
					"Paths identify the same object");
		}

		if (isOther(fromStatus)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.CAT_NOT_A_DATAOBJ_AND_NOT_A_COLLECTION, from,
					"Object type is not supported");
		}

		if (isOther(toStatus)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.CAT_NOT_A_DATAOBJ_AND_NOT_A_COLLECTION, to,
					"Object type is not supported");
		}

		if (isCollection(fromStatus) && isDataObject(toStatus)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.SYS_INVALID_INPUT_PARAM, from, to, "Incompatible paths");
		}

		if (isDataObject(fromStatus)) {
			if (CopyOptions.COLLECTIONS_ONLY == (CopyOptions.COLLECTIONS_ONLY & copyOptions)) {
				return;
			}

			if (isCollection(toStatus)) {
				var objectName = Paths.get(from).getFileName().toString();
				var toPath = Paths.get(to, objectName).toString();
				copyDataObject(comm, from, toPath, copyOptions);
				return;
			}

			copyDataObject(comm, from, to, copyOptions);
		} else if (isCollection(fromStatus)) {
			if (CopyOptions.RECURSIVE == (CopyOptions.RECURSIVE & copyOptions) || CopyOptions.NONE == copyOptions) {
				if (!exists(toStatus)) {
					if (!createCollection(comm, to, from)) {
						throw new IRODSFilesystemException(IRODSErrorCodes.FILE_CREATE_ERROR, to,
								"Cannot create collection");
					}
				}

				for (var e : new IRODSCollectionIterator(comm, from)) {
					var objectName = Paths.get(e.path).getFileName().toString();
					var toPath = Paths.get(to, objectName).toString();
					copy(comm, e.path, toPath, copyOptions | CopyOptions.IN_RECURSIVE_COPY);
				}
			}
		}
	}

	/**
	 * 
	 * @param comm
	 * @param from
	 * @param to
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void copy(RcComm comm, String from, String to)
			throws IRODSFilesystemException, IOException, IRODSException {
		copy(comm, from, to, CopyOptions.NONE);
	}

	/**
	 * 
	 * @param comm
	 * @param from
	 * @param to
	 * @param copyOptions
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean copyDataObject(RcComm comm, String from, String to, int copyOptions)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(from, "From/Source path is null or empty");
		throwIfNullOrEmpty(to, "To/Destination path is null or empty");
		throwIfPathLengthExceedsLimit(from);
		throwIfPathLengthExceedsLimit(to);

		if (!isDataObject(comm, from)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.INVALID_OBJECT_TYPE,
					"From/Source path does not identify a data object", from);
		}

		var input = new DataObjCopyInp_PI();
		input.DataObjInp_PI = new DataObjInp_PI[] { new DataObjInp_PI(), new DataObjInp_PI() };

		var srcInput = input.DataObjInp_PI[0];
		srcInput.objPath = from;
//		srcInput.oprType = 10; // COPY_SRC - see dataObjInpOut.h
		srcInput.KeyValPair_PI = new KeyValPair_PI();

		var dstInput = input.DataObjInp_PI[1];
		dstInput.objPath = to;
//		dstInput.oprType = 9; // COPY_DEST - see dataObjInpOut.h
		dstInput.KeyValPair_PI = new KeyValPair_PI();

		var s = status(comm, to);
		if (exists(s)) {
			if (equivalent(comm, from, to)) {
				throw new IRODSFilesystemException(IRODSErrorCodes.SAME_SRC_DEST_PATHS_ERR,
						"Paths identify the same data object", from, to);
			}

			if (!isDataObject(s)) {
				throw new IRODSFilesystemException(IRODSErrorCodes.INVALID_OBJECT_TYPE,
						"To/Destination path does not identify a data object", to);
			}

			if (CopyOptions.SKIP_EXISTING == copyOptions) {
				return false;
			}

			if (CopyOptions.OVERWRITE_EXISTING == copyOptions) {
				++dstInput.KeyValPair_PI.ssLen;
				dstInput.KeyValPair_PI.keyWord = new ArrayList<>();
				dstInput.KeyValPair_PI.svalue = new ArrayList<>();
				dstInput.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
				dstInput.KeyValPair_PI.svalue.add("");
			} else if (CopyOptions.UPDATE_EXISTING == copyOptions) {
				if (lastWriteTime(comm, from) <= lastWriteTime(comm, to)) {
					return false;
				}

				++dstInput.KeyValPair_PI.ssLen;
				dstInput.KeyValPair_PI.keyWord = new ArrayList<>();
				dstInput.KeyValPair_PI.svalue = new ArrayList<>();
				dstInput.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
				dstInput.KeyValPair_PI.svalue.add("");
			}
		}

		var output = new Reference<TransferStat_PI>();

		var ec = IRODSApi.rcDataObjCopy(comm, input, output);
		if (ec < 0) {
			throw new IRODSFilesystemException(ec, "rcDataObjCopy error", from, to);
		}

		return true;
	}

	/**
	 * 
	 * @param comm
	 * @param from
	 * @param to
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean copyDataObject(RcComm comm, String from, String to)
			throws IRODSFilesystemException, IOException, IRODSException {
		return copyDataObject(comm, from, to, CopyOptions.NONE);
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws IRODSFilesystemException
	 * 
	 * @since 0.1.0
	 */
	public static boolean createCollection(RcComm comm, String path) throws IOException, IRODSFilesystemException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		throwIfPathLengthExceedsLimit(path);

		var input = new CollInpNew_PI();
		input.collName = path;
		input.KeyValPair_PI = new KeyValPair_PI();

		var ec = IRODSApi.rcCollCreate(comm, input);
		if (ec < 0) {
			throw new IRODSFilesystemException(ec, "rcCollCreate error", path);
		}

		return true;
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * @param existingPath
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean createCollection(RcComm comm, String path, String existingPath)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		throwIfNullOrEmpty(existingPath, "Existing path is null or empty");
		throwIfPathLengthExceedsLimit(path);
		throwIfPathLengthExceedsLimit(existingPath);

		var s = status(comm, existingPath);

		if (!isCollection(s)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.INVALID_OBJECT_TYPE,
					"Existing path does not identify a collection", existingPath);
		}

		createCollection(comm, path);

		// TODO Use atomic ACLs API.
		var usernameSb = new StringBuilder();
		for (var perm : s.getPermissions()) {
			usernameSb.delete(0, usernameSb.length());
			usernameSb.append(perm.name).append('#').append(perm.zone);
			permissions(comm, path, usernameSb.toString(), perm.prms);
		}

		return true;
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean createCollections(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		throwIfPathLengthExceedsLimit(path);

		if (exists(comm, path)) {
			return false;
		}

		var input = new CollInpNew_PI();
		input.collName = path;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.ssLen = 1;
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.RECURSIVE_OPR);
		input.KeyValPair_PI.svalue.add("");

		return IRODSApi.rcCollCreate(comm, input) == 0;
	}

	/**
	 * Checks if a filesystem object is known to the catalog.
	 * 
	 * @param status The {@code ObjectStatus} of the filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public static boolean exists(ObjectStatus status) {
		return statusKnown(status) && status.getType() != ObjectType.NOT_FOUND;
	}

	/**
	 * Checks if a filesystem object is known to the catalog.
	 * 
	 * @param comm A connection to the iRODS server.
	 * @param path A path identifying the filesystem object.
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean exists(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		return exists(status(comm, path));
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean isCollectionRegistered(RcComm comm, String path) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		throwIfPathLengthExceedsLimit(path);

		var p = Paths.get(path);
		var query = String.format("select COLL_ID where COLL_NAME = '%s'", p.getParent().toString());
		var zone = extractZoneFromPath(path);

		return !IRODSQuery.executeGenQuery2(comm, zone, query).isEmpty();
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean isDataObjectRegistered(RcComm comm, String path) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		throwIfPathLengthExceedsLimit(path);

		var p = Paths.get(path);
		var query = String.format("select DATA_ID where COLL_NAME = '%s' and DATA_NAME = '%s'",
				p.getParent().toString(), p.getFileName().toString());
		var zone = extractZoneFromPath(path);

		return !IRODSQuery.executeGenQuery2(comm, zone, query).isEmpty();
	}

	/**
	 * 
	 * @param comm
	 * @param path1
	 * @param path2
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean equivalent(RcComm comm, String path1, String path2) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path1, "Path 1 is null or empty");
		throwIfNullOrEmpty(path1, "Path 2 is null or empty");

		var p1Info = stat(comm, path1);

		if (p1Info.error < 0) {
			throw new IRODSFilesystemException(p1Info.error, "Stat error", path1);
		}

		if (0 /* UNKNOWN_OBJ_T */ == p1Info.type) {
			throw new IRODSFilesystemException(IRODSErrorCodes.OBJ_PATH_DOES_NOT_EXIST, "Path 1 does not exist", path1);
		}
		var p2Info = stat(comm, path1);

		if (p2Info.error < 0) {
			throw new IRODSFilesystemException(p2Info.error, "Stat error", path2);
		}

		if (0 /* UNKNOWN_OBJ_T */ == p2Info.type) {
			throw new IRODSFilesystemException(IRODSErrorCodes.OBJ_PATH_DOES_NOT_EXIST, "Path 2 does not exist", path2);
		}

		return p1Info.id == p2Info.id;
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static long dataObjectSize(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");

		if (!isDataObject(comm, path)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.NOT_A_DATA_OBJECT,
					"Path does not identify a data object", path);
		}

		var zone = extractZoneFromPath(path);
		var p = Paths.get(path);
		var query = String.format(
				"select DATA_SIZE, DATA_MODIFY_TIME where COLL_NAME = '%s' and DATA_NAME = '%s' and DATA_REPL_STATUS = '1'",
				p.getParent().toString(), p.getFileName().toString());
		var rows = IRODSQuery.executeGenQuery2(comm, zone, query);

		if (rows.isEmpty()) {
			throw new IRODSFilesystemException(IRODSErrorCodes.SYS_NO_GOOD_REPLICA, "No good replica available", path);
		}

		// This implementation assumes that any good replica will always satisfy the
		// requirement within the loop, therefore the first iteration always causes the
		// size to be captured. The size object should be empty if and only if there are
		// no good replicas.
		long latestMtime = 0;
		long size = 0;

		for (var row : rows) {
			var currentMtime = Long.parseLong(row.get(1));
			if (currentMtime > latestMtime) {
				latestMtime = currentMtime;
				size = Long.parseLong(row.get(0));
			}
		}

		return size;
	}

	/**
	 * Checks if the filesystem object is a collection.
	 * 
	 * @param status The {@code ObjectStatus} of the filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public static boolean isCollection(ObjectStatus status) {
		return status.getType() == ObjectType.COLLECTION;
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean isCollection(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		return isCollection(status(comm, path));
	}

	/**
	 * 
	 * @param status
	 * 
	 * @since 0.1.0
	 */
	public static boolean isDataObject(ObjectStatus status) {
		return status.getType() == ObjectType.DATA_OBJECT;
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean isDataObject(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		return isDataObject(status(comm, path));
	}

	/**
	 * 
	 * @param status
	 * 
	 * @since 0.1.0
	 */
	public static boolean isOther(ObjectStatus status) {
		return status.getType() == ObjectType.UNKNOWN;
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean isOther(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		return isOther(status(comm, path));
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean isSpecialCollection(RcComm comm, String path) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");

		var query = String.format("select COLL_TYPE, COLL_INFO1, COLL_INFO2 where COLL_NAME = '%s'", path);
		var zone = extractZoneFromPath(path);

		for (var row : IRODSQuery.executeGenQuery2(comm, zone, query)) {
			return !row.get(0).isEmpty() && (!row.get(1).isEmpty() || !row.get(2).isEmpty());
		}

		return false;
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean isEmpty(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");

		var s = status(comm, path);

		if (!isCollection(s)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.CAT_NOT_A_DATAOBJ_AND_NOT_A_COLLECTION,
					"Path does not identify a collection", path);
		}

		return isCollectionEmpty(comm, path);
	}

	/**
	 * Returns the time of the last modification to a filesystem object as epoch
	 * seconds.
	 * 
	 * If the filesystem object identifies a data object, only good replicas will be
	 * considered. If no good replicas exists, an exception will be thrown.
	 * 
	 * @param comm A connection to the iRODS server.
	 * @param path The logical path identifying a collection or data object.
	 * 
	 * @return The modification time as seconds since epoch.
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static long lastWriteTime(RcComm comm, String path)
			throws IRODSFilesystemException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");

		var s = status(comm, path);
		String query = null;

		if (isDataObject(s)) {
			var p = Paths.get(path);
			// Fetch information for good replicas only.
			query = String.format(
					"select max(DATA_MODIFY_TIME) where COLL_NAME = '%s' and DATA_NAME = '%s' and DATA_REPL_STATUS = '1'",
					p.getParent().toString(), p.getFileName().toString());
		} else if (isCollection(s)) {
			query = String.format("select COLL_MODIFY_TIME where COLL_NAME = '%s'", path);
		} else {
			throw new IRODSFilesystemException(IRODSErrorCodes.INVALID_OBJECT_TYPE,
					"Path does not identify a data object or collection", path);
		}

		var zone = extractZoneFromPath(path);

		for (var row : IRODSQuery.executeGenQuery2(comm, zone, query)) {
			return Long.parseLong(row.get(0));
		}

		throw new IRODSFilesystemException(IRODSErrorCodes.CAT_NO_ROWS_FOUND, "Modify time unavailable", path);
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * @param newModifyTime
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void lastWriteTime(RcComm comm, String path, long newModifyTime) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");

		if (!isCollection(comm, path)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.NOT_A_COLLECTION, "Path does not identify a collection",
					path);
		}

		var input = new CollInpNew_PI();
		input.collName = path;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.ssLen = 1;
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.COLLECTION_MTIME);
		input.KeyValPair_PI.svalue.add(String.format("%011d", newModifyTime));

		var ec = IRODSApi.rcModColl(comm, input);
		if (ec < 0) {
			throw new IRODSFilesystemException(ec, "rcModColl error", path);
		}
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * @param removeOptions
	 * @return
	 * @throws IOException
	 * @throws IRODSException
	 */
	public static boolean remove(RcComm comm, String path, RemoveOptions removeOptions)
			throws IOException, IRODSException {
		var options = new ExtendedRemoveOptions();
		options.noTrash = (RemoveOptions.NO_TRASH == removeOptions);
		options.recursive = false;
		return removeImpl(comm, path, options);
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws IRODSException
	 */
	public static boolean remove(RcComm comm, String path) throws IOException, IRODSException {
		return remove(comm, path, RemoveOptions.NONE);
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * @param removeOptions
	 * @throws IOException
	 * @throws IRODSException
	 */
	public static void removeAll(RcComm comm, String path, RemoveOptions removeOptions)
			throws IOException, IRODSException {
		var options = new ExtendedRemoveOptions();
		options.noTrash = (RemoveOptions.NO_TRASH == removeOptions);
		options.recursive = true;
		removeImpl(comm, path, options);
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * @throws IOException
	 * @throws IRODSException
	 */
	public static void removeAll(RcComm comm, String path) throws IOException, IRODSException {
		removeAll(comm, path, RemoveOptions.NONE);
	}

	/**
	 * 
	 * @param comm
	 * @param path
	 * @param userOrGroup
	 * @param prms
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static void permissions(RcComm comm, String path, String userOrGroup, Permission prms)
			throws IRODSFilesystemException, IOException {
		final var addAdminFlag = false;
		setPermissions(addAdminFlag, comm, path, userOrGroup, prms);
	}

	/**
	 * 
	 * @param adminTag
	 * @param comm
	 * @param path
	 * @param userOrGroup
	 * @param prms
	 * @throws IRODSFilesystemException
	 * @throws IOException
	 * 
	 * @since 0.1.0
	 */
	public static void permissions(AdminTag adminTag, RcComm comm, String path, String userOrGroup, Permission prms)
			throws IRODSFilesystemException, IOException {
		final var addAdminFlag = true;
		setPermissions(addAdminFlag, comm, path, userOrGroup, prms);
	}

	/**
	 * TODO
	 * 
	 * @param comm
	 * @param path
	 * @param enable
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void enableInheritance(RcComm comm, String path, boolean enable) throws IOException, IRODSException {
		final var addAdminFlag = false;
		setInheritance(addAdminFlag, comm, path, enable);
	}

	/**
	 * TODO
	 * 
	 * @param adminTag
	 * @param comm
	 * @param path
	 * @param enable
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void enableInheritance(AdminTag adminTag, RcComm comm, String path, boolean enable)
			throws IOException, IRODSException {
		final var addAdminFlag = true;
		setInheritance(addAdminFlag, comm, path, enable);
	}

	public static void rename(RcComm comm, String oldPath, String newPath) throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(oldPath, "Old Path is null or empty");
		throwIfNullOrEmpty(newPath, "New Path is null or empty");
		throwIfPathLengthExceedsLimit(oldPath);
		throwIfPathLengthExceedsLimit(newPath);

//		var oldPathStat = status(comm, oldPath);
//		var newPathStat = status(comm, newPath);

		var input = new DataObjCopyInp_PI();
		input.DataObjInp_PI = new DataObjInp_PI[] { new DataObjInp_PI(), new DataObjInp_PI() };
		input.DataObjInp_PI[0].objPath = oldPath;
		input.DataObjInp_PI[0].KeyValPair_PI = new KeyValPair_PI();
		input.DataObjInp_PI[1].objPath = newPath;
		input.DataObjInp_PI[1].KeyValPair_PI = new KeyValPair_PI();

		var ec = IRODSApi.rcDataObjRename(comm, input);
		if (ec < 0) {
			throw new IRODSFilesystemException(ec, "rcDataObjRename error", oldPath, newPath);
		}
	}

	/**
	 * TODO
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * @throws IRODSFilesystemException
	 * 
	 * @since 0.1.0
	 */
	public static ObjectStatus status(RcComm comm, String path)
			throws IOException, IRODSException, IRODSFilesystemException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		throwIfPathLengthExceedsLimit(path);

		var s = stat(comm, path);
		if (s.error < 0) {
			throw new IRODSFilesystemException(s.error, "Stat error");
		}

		var status = new ObjectStatus();
		status.setPermissions(s.prms);
		status.setInheritance(s.inheritance);

		// TODO From the C++ implementation:
		//
		// This does not handle the case of ObjectType.UNKNOWN. This type means a file
		// exists, but the type is unknown. Maybe this case is not possible in iRODS.
		switch (s.type) {
		case 1 /* DATA_OBJ_T */:
			status.setType(ObjectType.DATA_OBJECT);
			break;

		case 2 /* COLL_OBJ_T */:
			status.setType(ObjectType.COLLECTION);
			break;

		// This case indicates that iRODS does not contain a data object or collection
		// at the target path.
		case 0 /* UNKNOWN_OBJ_T */:
			status.setType(ObjectType.NOT_FOUND);
			break;

//		case ?:
//			status.setType(ObjectType.UNKNOWN);
//			break;

		default:
			status.setType(ObjectType.NONE);
			break;
		}

		return status;
	}

	/**
	 * Checks if the filesystem object's type is known.
	 * 
	 * @param status The filesystem object's status.
	 * 
	 * @since 0.1.0
	 */
	public static boolean statusKnown(ObjectStatus status) {
		return status.getType() != ObjectType.NONE;
	}

	/**
	 * TODO
	 * 
	 * @param comm
	 * @param path
	 * 
	 * @return
	 * 
	 * @throws IRODSFilesystemException
	 * @throws IRODSException
	 * @throws IOException
	 * @throws NumberFormatException
	 * 
	 * @since 0.1.0
	 */
	public static String dataObjectChecksum(RcComm comm, String path)
			throws IRODSFilesystemException, NumberFormatException, IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Logical path is null or empty");

		if (!isDataObject(comm, path)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.SYS_INVALID_INPUT_PARAM,
					"Logical path does not point to a data object", path);
		}

		var fspath = Paths.get(path);
		var query = String.format(
				"select DATA_CHECKSUM, DATA_MODIFY_TIME where COLL_NAME = '%s' and DATA_NAME = '%s' and DATA_REPL_STATUS = '1'",
				fspath.getParent().toAbsolutePath(), fspath.getFileName());
		var zone = extractZoneFromPath(path);

		var latestMtime = 0L;
		String checksum = "";

		// This implementation assumes that any good replica will always satisfy the
		// requirement within the loop, therefore the first iteration always causes the
		// checksum to be captured. The checksum object should be empty if and only if
		// there are no good replicas.
		for (var row : IRODSQuery.executeGenQuery2(comm, zone, query)) {
			var curMtime = Long.parseLong(row.get(1));
			if (curMtime > latestMtime) {
				latestMtime = curMtime;
				checksum = row.get(0);
			}
		}

		return checksum;
	}

	private static String extractZoneFromPath(String path) {
		var p = Paths.get(path);

		if (!p.isAbsolute()) {
			throw new IllegalArgumentException("Path is not absolute");
		}

		return p.getName(0).toString();
	}

	private static void throwIfNull(Object object, String message) {
		if (null == object) {
			throw new IllegalArgumentException(message);
		}
	}

	private static void throwIfNullOrEmpty(String s, String message) {
		if (null == s || s.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	private static void throwIfPathLengthExceedsLimit(String path) throws IRODSFilesystemException {
		// Defined in irods/irods/lib/core/include/irods/rodsDef.h.
		//
		// 1088 = MAX_NAME_LEN
		// = MAX_PATH_ALLOWED + 64
		// = 1024 + 64
		//
		// The true max path length is MAX_NAME_LEN - 1, to accomodate space for the
		// null-terminating byte.
		if (path.length() > 1087) {
			throw new IRODSFilesystemException(IRODSErrorCodes.USER_PATH_EXCEEDS_MAX, "Path exceeds maximum length",
					path);
		}
	}

	private static final class StatInfo {
		int error;
		long size;
		int type;
		int mode;
		long id;
		long ctime;
		long mtime;
		List<EntityPermission> prms;
		boolean inheritance;
	}

	private static Permission toPermissionEnum(String perm) {
		switch (perm) {
		case "null":
			return Permission.NULL;
		case "read_metadata":
			return Permission.READ_METADATA;
		case "read_object":
			return Permission.READ_OBJECT;
		case "read object":
			return Permission.READ_OBJECT;
		case "read":
			return Permission.READ_OBJECT;
		case "create_metadata":
			return Permission.CREATE_METADATA;
		case "modify_metadata":
			return Permission.MODIFY_METADATA;
		case "delete_metadata":
			return Permission.DELETE_METADATA;
		case "create_object":
			return Permission.CREATE_OBJECT;
		case "modify_object":
			return Permission.MODIFY_OBJECT;
		case "modify object":
			return Permission.MODIFY_OBJECT;
		case "write":
			return Permission.MODIFY_OBJECT;
		case "delete_object":
			return Permission.DELETE_OBJECT;
		case "own":
			return Permission.OWN;
		}

		throw new IllegalArgumentException("Unknown Permission string: " + perm);
	}

	private static List<EntityPermission> toEntityPermissionsList(RcComm comm, String path, int objectType)
			throws IOException, IRODSException {
		var perms = new ArrayList<EntityPermission>();

		if (/* DATA_OBJ_T */ 1 == objectType) {
			var zone = extractZoneFromPath(path);
			var fspath = Paths.get(path);
			// TODO Open issue for this GenQuery2 query. It uses the wrong table alias for
			// DATA_ACCESS_USER_ZONE. The workaround is to use DATA_ACCESS_USER_ID and
			// resolve the user name, user zone, and user type against it.
//			var query = String.format(
//					"select DATA_ACCESS_USER_NAME, DATA_ACCESS_USER_ZONE, DATA_ACCESS_PERM_NAME, USER_TYPE where COLL_NAME = '%' and DATA_NAME = '%s'",
//					fspath.getParent().toString(), fspath.getFileName().toString());
//			for (var row : IRODSQuery.executeGenQuery(comm, zone, query)) {
//				var ep = new EntityPermission();
//				ep.name = row.get(0);
//				ep.zone = row.get(1);
//				ep.prms = toPermissionEnum(row.get(2));
//				ep.type = row.get(3);
//				perms.add(ep);
//			}

			// TODO THE WORKAROUND.

			// First, get the user id and permissions on the data object.
			var map = new HashMap<String, String>();
			var query = String.format(
					"select DATA_ACCESS_USER_ID, DATA_ACCESS_PERM_NAME where COLL_NAME = '%s' and DATA_NAME = '%s'",
					fspath.getParent().toString(), fspath.getFileName().toString());
			for (var row : IRODSQuery.executeGenQuery2(comm, zone, query)) {
				map.put(row.get(0), row.get(1));
			}

			// Now, retrieve the user information using the user id of each user.
			query = String.format("select USER_ID, USER_NAME, USER_ZONE, USER_TYPE where USER_ID in ('%s')",
					String.join("', '", map.keySet()));
			log.debug("Query for data object permissions = [{}]", query);
			for (var row : IRODSQuery.executeGenQuery2(comm, zone, query)) {
				var ep = new EntityPermission();
				ep.name = row.get(1);
				ep.zone = row.get(2);
				ep.prms = toPermissionEnum(map.get(row.get(0)));
				ep.type = IRODSUsers.toUserType(row.get(3));
				perms.add(ep);
			}
		}

		if (/* COLL_OBJ_T */ 2 == objectType) {
			var zone = extractZoneFromPath(path);
			var bindArgs = Arrays.asList(path);
			IRODSQuery.executeSpecificQuery(comm, zone, "ShowCollAcls", bindArgs, row -> {
				var ep = new EntityPermission();
				ep.name = row.get(0);
				ep.zone = row.get(1);
				ep.prms = toPermissionEnum(row.get(2));
				ep.type = IRODSUsers.toUserType(row.get(3));
				perms.add(ep);
				return true;
			});
		}

		return perms;
	}

	private static boolean getInheritance(RcComm comm, String path, int objectType) throws IOException, IRODSException {
		if (2 /* COLL_OBJ_T */ != objectType) {
			return false;
		}

		var zone = extractZoneFromPath(path);
		var query = String.format("select COLL_INHERITANCE where COLL_NAME = '%s'", path);
		for (var row : IRODSQuery.executeGenQuery2(comm, zone, query)) {
			return "1".equals(row.get(0));
		}

		return false;
	}

	private static StatInfo stat(RcComm comm, String logicalPath) throws IOException, IRODSException {
		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.KeyValPair_PI = new KeyValPair_PI();

		var output = new Reference<RodsObjStat_PI>();
		var statInfo = new StatInfo();

		statInfo.error = IRODSApi.rcObjStat(comm, input, output);

		if (statInfo.error >= 0) {
			statInfo.id = Integer.parseInt(output.value.dataId);
			statInfo.ctime = Long.parseLong(output.value.createTime);
			statInfo.mtime = Long.parseLong(output.value.modifyTime);
			statInfo.size = output.value.objSize;
			statInfo.type = output.value.objType;
			statInfo.mode = output.value.dataMode;
			statInfo.inheritance = getInheritance(comm, logicalPath, statInfo.type);
			statInfo.prms = toEntityPermissionsList(comm, logicalPath, statInfo.type);
		} else if (IRODSErrorCodes.USER_FILE_DOES_NOT_EXIST == statInfo.error) {
			statInfo.error = 0;
			statInfo.type = 0; // UNKNOWN_OBJ_T
		}

		return statInfo;
	}

	private static void setInheritance(boolean addAdminFlag, RcComm comm, String logicalPath, boolean enableInheritance)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfPathLengthExceedsLimit(logicalPath);

		if (!isCollection(comm, logicalPath)) {
			throw new IRODSFilesystemException(IRODSErrorCodes.NOT_A_COLLECTION, "Path does not identify a collection",
					logicalPath);
		}

		var input = new ModAccessControlInp_PI();
		input.userName = "";
		input.zone = "";
		input.path = logicalPath;

		var access = new StringBuilder();
		if (addAdminFlag) {
			access.append("admin:");
		}
		access.append(enableInheritance ? "inherit" : "noinherit");
		input.accessLevel = access.toString();

		var ec = IRODSApi.rcModAccessControl(comm, input);
		if (ec < 0) {
			throw new IRODSFilesystemException(ec, "rcModAccessControl error", logicalPath);
		}
	}

	private static void setPermissions(boolean addAdminFlag, RcComm comm, String logicalPath, String userOrGroup,
			Permission perm) throws IOException, IRODSFilesystemException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(logicalPath, "Path is null or empty");
		throwIfPathLengthExceedsLimit(logicalPath);

		String username = userOrGroup;
		String zone = "";

		var usernameParts = userOrGroup.split("#");
		if (2 == usernameParts.length) {
			username = usernameParts[0];
			zone = usernameParts[1];
		}

		var input = new ModAccessControlInp_PI();
		input.userName = username;
		input.zone = zone;
		input.path = logicalPath;

		var access = new StringBuilder();
		if (addAdminFlag) {
			access.append("admin:");
		}

		switch (perm) {
		case NULL:
			access.append("null");
			break;
		case READ_METADATA:
			access.append("read_metadata");
			break;
		case READ_OBJECT:
			// TODO(#78): Change to "read_object" once we know all servers are at least running iRODS 4.3.0.
			access.append("read");
			break;
		case CREATE_METADATA:
			access.append("create_metadata");
			break;
		case MODIFY_METADATA:
			access.append("modify_metadata");
			break;
		case DELETE_METADATA:
			access.append("delete_metadata");
			break;
		case CREATE_OBJECT:
			access.append("create_object");
			break;
		case MODIFY_OBJECT:
			// TODO(#78): Change to "modify_object" once we know all servers are at least running iRODS 4.3.0.
			access.append("write");
			break;
		case DELETE_OBJECT:
			access.append("delete_object");
			break;
		case OWN:
			access.append("own");
			break;
		}

		input.accessLevel = access.toString();

		var ec = IRODSApi.rcModAccessControl(comm, input);
		if (ec < 0) {
			throw new IRODSFilesystemException(ec, "rcModAccessControl error", logicalPath);
		}
	}

	private static final class ExtendedRemoveOptions {
		public boolean noTrash = false;
		public boolean verbose = false;
		public boolean progress = false;
		public boolean recursive = false;
		public boolean unregister = false;
	}

	private static boolean removeImpl(RcComm comm, String path, ExtendedRemoveOptions removeOptions)
			throws IOException, IRODSException {
		throwIfNull(comm, "RcComm is null");
		throwIfNullOrEmpty(path, "Path is null or empty");
		throwIfPathLengthExceedsLimit(path);

		var s = status(comm, path);

		if (!exists(s)) {
			return false;
		}

		if (isDataObject(s)) {
			var input = new DataObjInp_PI();
			input.objPath = path;
			input.oprType = removeOptions.unregister ? 26 /* UNREG_OPR */ : 0;
			input.KeyValPair_PI = new KeyValPair_PI();

			if (removeOptions.noTrash) {
				input.KeyValPair_PI.ssLen = 1;
				input.KeyValPair_PI.keyWord = new ArrayList<>();
				input.KeyValPair_PI.svalue = new ArrayList<>();
				input.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
				input.KeyValPair_PI.svalue.add("");
			}

			return IRODSApi.rcDataObjUnlink(comm, input) == 0;
		}

		if (isCollection(s)) {
			var input = new CollInpNew_PI();
			input.collName = path;
			input.KeyValPair_PI = new KeyValPair_PI();
			input.KeyValPair_PI.keyWord = new ArrayList<>();
			input.KeyValPair_PI.svalue = new ArrayList<>();

			if (removeOptions.noTrash) {
				++input.KeyValPair_PI.ssLen;
				input.KeyValPair_PI.keyWord.add(IRODSKeywords.FORCE_FLAG);
				input.KeyValPair_PI.svalue.add("");
			}

			if (removeOptions.recursive) {
				++input.KeyValPair_PI.ssLen;
				input.KeyValPair_PI.keyWord.add(IRODSKeywords.RECURSIVE_OPR);
				input.KeyValPair_PI.svalue.add("");
			}

			var output = new Reference<CollOprStat_PI>();

			return IRODSApi.rcRmColl(comm, input, output) == 0;
		}

		throw new IRODSFilesystemException(IRODSErrorCodes.CAT_NOT_A_DATAOBJ_AND_NOT_A_COLLECTION,
				"Object type is not supported", path);
	}

	private static boolean isCollectionEmpty(RcComm comm, String path) throws IRODSException, IOException {
		return new IRODSCollectionIterator(comm, path).iterator().hasNext();
	}

}
