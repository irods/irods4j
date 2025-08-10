package org.irods.irods4j.high_level.vfs;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.high_level.vfs.ObjectStatus.ObjectType;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;

/**
 * A class which makes it easy to iterate over the contents of a collection.
 * 
 * Instances of this class do not visit subcollections.
 * 
 * @since 0.1.0
 */
public class IRODSCollectionIterator implements Iterable<CollectionEntry> {

	private static final Logger log = LogManager.getLogger();

	private static final int DEFAULT_NUMBER_OF_ROWS_PER_PAGE = 512;

	private RcComm comm;
	private String logicalPath;
	private int rowsPerPage;

	@SuppressWarnings("unused")
	private CollectionOptions collOptions;

	// Holds a reference to the current query results.
	private List<List<String>> rows;

	// The index of a row within the query results.
	private int rowIndex = 0;

	// Instructs the iterator to include a condition which allows the query to
	// return the next set of rows.
	private boolean addPivotCondition = false;

	// Instructs the iterator to query for collections. See the iterator
	// implementation for details on when this gets set to true.
	private boolean searchForCollections = false;

	// Used to build up a query string efficiently. The query which exists in the
	// buffer is used to fetch the next page.
	private StringBuilder querySb;

	// Aids in deduplication of data objects during iteration.
	private String lastObjectId = "";

	/**
	 * Options which affect the behavior of the iterator.
	 * 
	 * Only {@code NONE} is supported at this time.
	 * 
	 * @since 0.1.0
	 */
	public static enum CollectionOptions {
		NONE, SKIP_PERMISSION_DENIED
	}

	/**
	 * Initializes a newly created iterator.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute path to a collection.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * @throws IRODSFilesystemException
	 * 
	 * @throws IllegalArgumentException If invalid inputs are passed.
	 * 
	 * @since 0.1.0
	 */
	public IRODSCollectionIterator(RcComm comm, String logicalPath)
			throws IRODSFilesystemException, IOException, IRODSException {
		this(comm, logicalPath, CollectionOptions.NONE);
	}

	/**
	 * Initializes a newly created iterator.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute path to a collection.
	 * @param rowsPerPage The max number of rows to fetch when a new page of data is
	 *                    needed.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * @throws IRODSFilesystemException
	 * 
	 * @throws IllegalArgumentException If invalid inputs are passed.
	 * 
	 * @since 0.1.0
	 */
	public IRODSCollectionIterator(RcComm comm, String logicalPath, int rowsPerPage)
			throws IRODSFilesystemException, IOException, IRODSException {
		this(comm, logicalPath, rowsPerPage, CollectionOptions.NONE);
	}

	/**
	 * Initializes a newly created iterator.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute path to a collection.
	 * @param options     Options affecting the behavior of the iterator. Currently
	 *                    unused.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * @throws IRODSFilesystemException
	 * 
	 * @since 0.1.0
	 */
	public IRODSCollectionIterator(RcComm comm, String logicalPath, CollectionOptions options)
			throws IRODSFilesystemException, IOException, IRODSException {
		this(comm, logicalPath, DEFAULT_NUMBER_OF_ROWS_PER_PAGE, options);
	}

	/**
	 * Initializes a newly created iterator.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute path to a collection.
	 * @param rowsPerPage The max number of rows to fetch when a new page of data is
	 *                    needed.
	 * @param options     Options affecting the behavior of the iterator. Currently
	 *                    unused.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * @throws IRODSFilesystemException
	 * 
	 * @since 0.1.0
	 */
	public IRODSCollectionIterator(RcComm comm, String logicalPath, int rowsPerPage, CollectionOptions options)
			throws IRODSFilesystemException, IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == logicalPath || logicalPath.isEmpty()) {
			throw new IllegalArgumentException("Logical path is null or empty");
		}

		if (rowsPerPage < 1) {
			throw new IllegalArgumentException("Rows per page is less than 1");
		}

		if (null == options) {
			throw new IllegalArgumentException("Collection options is null");
		}

		this.comm = comm;
		this.logicalPath = logicalPath;
		this.rowsPerPage = rowsPerPage;
		collOptions = options;
	}

	/**
	 * Returns the max number of rows a single page may contain.
	 * 
	 * @since 0.1.0
	 */
	public int getRowsPerPage() {
		return rowsPerPage;
	}

	@Override
	public Iterator<CollectionEntry> iterator() {
		return new CollectionEntryIterator(this);
	}

	/**
	 * The class providing the iterator implementation over a collection.
	 * 
	 * @since 0.1.0
	 */
	public static final class CollectionEntryIterator implements Iterator<CollectionEntry> {

		private IRODSCollectionIterator iter;

		private CollectionEntryIterator(IRODSCollectionIterator iter) {
			this.iter = iter;

			// Return immediately if the iterator has been previously constructed.
			if (null != iter.rows) {
				return;
			}

			iter.rows = null;
			iter.rowIndex = 0;
			iter.addPivotCondition = false;
			iter.searchForCollections = false;
			iter.querySb = new StringBuilder(512);
		}

		@Override
		public boolean hasNext() {
			// We're working with an existing set of rows.
			if (null != iter.rows) {
				// Find the next object, skipping over objects sharing the same object ID.
				// This loop is primarily for data objects having multiple replicas.
				while (++iter.rowIndex < iter.rows.size()) {
					var objectId = iter.rows.get(iter.rowIndex).get(0);
					if (!iter.lastObjectId.equals(objectId)) {
						iter.lastObjectId = objectId;
						return true;
					}
				}

				// This is an optimization which avoids an unnecessary network call to the
				// server just to detect an empty resultset. If the number of rows in the
				// current page is less than the max number of rows that can be held by a page,
				// then we know there's no point in querying the catalog again. The query would
				// simply return an empty resultset.
				if (iter.rows.size() < iter.rowsPerPage) {
					if (iter.searchForCollections) {
						return false;
					} else {
						iter.searchForCollections = true;
						iter.addPivotCondition = false;
					}
				} else {
					// At this point, we know we need the next page of data.
					iter.addPivotCondition = true;
				}

				// Unlike the constructor, the row index must be set to the first row since it
				// will be used to retrieve an entry from the page, if rows exist.
				iter.rowIndex = 0;
			}

			//
			// We've iterated over all rows of the current page and need more data.
			//

			if (!iter.searchForCollections) {
				iter.querySb.delete(0, iter.querySb.length());
				iter.querySb.append(
						"select DATA_ID, DATA_NAME, DATA_SIZE, DATA_CHECKSUM, DATA_MODE, DATA_CREATE_TIME, DATA_MODIFY_TIME, DATA_REPL_STATUS where COLL_NAME = '");
				iter.querySb.append(iter.logicalPath);
				iter.querySb.append("'");

				// All rows have been visited within the current page.
				if (iter.addPivotCondition) {
					iter.addPivotCondition = false;

					// Update the query to find the next page of rows. The next page begins after
					// the ID of the last row within the current set of rows.
					iter.querySb.append(" and DATA_ID > '");
					iter.querySb.append(iter.rows.get(iter.rows.size() - 1).get(0));
					iter.querySb.append("' order by DATA_ID, DATA_REPL_STATUS desc, DATA_MODIFY_TIME desc limit ");
					iter.querySb.append(iter.rowsPerPage);
				} else {
					iter.querySb.append(" order by DATA_ID, DATA_REPL_STATUS desc, DATA_MODIFY_TIME desc limit ");
					iter.querySb.append(iter.rowsPerPage);
				}

				try {
					iter.rows = IRODSQuery.executeGenQuery2(iter.comm, iter.comm.proxyUserZone,
							iter.querySb.toString());
					if (!iter.rows.isEmpty()) {
						iter.lastObjectId = iter.rows.get(0).get(0);
						return true;
					}

					iter.searchForCollections = true;
					iter.rowIndex = 0;
				} catch (IOException | IRODSException e) {
					log.error(e.getMessage());
				}
			}

			if (iter.searchForCollections) {
				iter.querySb.delete(0, iter.querySb.length());
				iter.querySb.append(
						"select COLL_ID, COLL_NAME, COLL_CREATE_TIME, COLL_MODIFY_TIME where COLL_PARENT_NAME = '");
				iter.querySb.append(iter.logicalPath);
				iter.querySb.append("'");

				// All rows have been visited within the current page.
				if (iter.addPivotCondition) {
					iter.addPivotCondition = false;

					// Update the query to find the next page of rows. The next page begins after
					// the ID of the last row within the current set of rows.
					iter.querySb.append(" and COLL_ID > '");
					iter.querySb.append(iter.rows.get(iter.rows.size() - 1).get(0));
					iter.querySb.append("' order by COLL_ID limit ");
					iter.querySb.append(iter.rowsPerPage);
				} else {
					iter.querySb.append(" order by COLL_ID limit ");
					iter.querySb.append(iter.rowsPerPage);
				}

				try {
					iter.rows = IRODSQuery.executeGenQuery2(iter.comm, iter.comm.proxyUserZone,
							iter.querySb.toString());
					// Terminate because collections are processed after data objects.
					if (!iter.rows.isEmpty()) {
						iter.lastObjectId = iter.rows.get(0).get(0);
						return true;
					}
				} catch (IOException | IRODSException e) {
					log.error(e.getMessage());
				}
			}

			return false;
		}

		@Override
		public CollectionEntry next() {
			var row = iter.rows.get(iter.rowIndex);
			var e = new CollectionEntry();

			if (iter.searchForCollections) {
				e.dataId = row.get(0);
				e.path = row.get(1);
				e.ctime = Long.parseLong(row.get(2));
				e.mtime = Long.parseLong(row.get(3));
				e.status = new ObjectStatus();
				e.status.setType(ObjectType.COLLECTION);

				return e;
			}

			e.dataId = row.get(0);
			e.path = String.join("/", iter.logicalPath, row.get(1));
			e.dataSize = Long.parseLong(row.get(2));
			e.checksum = row.get(3);
			e.dataMode = Integer.parseInt(row.get(4));
			e.ctime = Long.parseLong(row.get(5));
			e.mtime = Long.parseLong(row.get(6));
			e.status = new ObjectStatus();
			e.status.setType(ObjectType.DATA_OBJECT);

			return e;
		}

	}

}
