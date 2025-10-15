package org.irods.irods4j.high_level.vfs;

/**
 * Holds information about a single logical path inside a collection.
 * 
 * @since 0.1.0
 */
public class CollectionEntry implements Comparable<CollectionEntry> {

	String path;
	ObjectStatus status;
	int dataMode;
	long dataSize;
	String dataId;
	long ctime;
	long mtime;
	String checksum;
	String owner;
	String dataType;

	CollectionEntry() {
	}

	/**
	 * Returns the logical path which identifies the filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public String path() {
		return path;
	}

	/**
	 * Returns true if the path points to a valid filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public boolean exists() {
		return IRODSFilesystem.exists(status);
	}

	/**
	 * Checks whether the logical path identifies a data object.
	 * 
	 * @since 0.1.0
	 */
	public boolean isDataObject() {
		return IRODSFilesystem.isDataObject(status);
	}

	/**
	 * Checks whether the logical path identifies a collection.
	 * 
	 * @since 0.1.0
	 */
	public boolean isCollection() {
		return IRODSFilesystem.isCollection(status);
	}

	/**
	 * Checks whether the logical path identifies something that is not a data
	 * object or collection.
	 * 
	 * @since 0.1.0
	 */
	public boolean isOther() {
		return IRODSFilesystem.isOther(status);
	}

	/**
	 * Returns the epoch seconds representing the time the filesystem object was
	 * created.
	 * 
	 * @since 0.1.0
	 */
	public long createdAt() {
		return ctime;
	}

	/**
	 * Returns the epoch seconds representing the time the filesystem object was
	 * last modified.
	 * 
	 * @since 0.1.0
	 */
	public long modifiedAt() {
		return mtime;
	}

	/**
	 * Returns the status information of the filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public ObjectStatus status() {
		return status;
	}

	/**
	 * Returns the data mode of the filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public int dataMode() {
		return dataMode;
	}

	/**
	 * Returns the ID of the filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public String id() {
		return dataId;
	}

//	/**
//	 * Returns the checksum of the filesystem object.
//	 * 
//	 * @since 0.1.0
//	 */
//	public String checksum() {
//		// TODO This is a questionable function. Should probably remove it.
//		return checksum;
//	}
//
//	/**
//	 * 
//	 * 
//	 * @since 0.1.0
//	 */
//	public String owner() {
//		// TODO This likely refers to the data_owner_name column, which means it should
//		// NOT be exposed/used. Remove it.
//		return owner;
//	}

	/**
	 * Returns the data type describing the filesystem object, if available.
	 * 
	 * @since 0.1.0
	 */
	public String dataType() {
		return dataType;
	}

	/**
	 * Returns the size of the filesystem object.
	 * 
	 * @since 0.1.0
	 */
	public long dataSize() {
		return dataSize;
	}

	/**
	 * Compares this collection entry with another using the logical path.
	 * 
	 * @since 0.1.0
	 */
	@Override
	public int compareTo(CollectionEntry o) {
		return path.compareTo(o.path);
	}

}
