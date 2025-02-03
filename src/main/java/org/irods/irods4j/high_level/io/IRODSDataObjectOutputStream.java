package org.irods.irods4j.high_level.io;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream.OnCloseSuccess;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream.SeekDirection;
import org.irods.irods4j.input_validation.Preconditions;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;

/**
 * A buffered output stream that makes it easy to write iRODS data objects.
 * 
 * @since 0.1.0
 */
public class IRODSDataObjectOutputStream extends OutputStream {

	private static final Logger log = LogManager.getLogger();

	private IRODSDataObjectStream in = new IRODSDataObjectStream();
	private byte[] buffer;
	private int position;

	/**
	 * Initializes a new instance with a buffer size of 65536 bytes.
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectOutputStream() {
		buffer = new byte[65536];
	}

	/**
	 * Initializes a new instance with a custom buffer size.
	 * 
	 * @param bufferSize The size of the internal buffer.
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectOutputStream(int bufferSize) {
		Preconditions.greaterThanOrEqualToValue(bufferSize, 1, "Buffer size is less than 1");
		buffer = new byte[bufferSize];
	}

	/**
	 * Initializes a new instance and opens a data object for writing.
	 * <p>
	 * If not replica exists, it is created.
	 * <p>
	 * The internal buffer will have a size of 65536.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param truncate    Truncate the replica.
	 * @param append      Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectOutputStream(RcComm comm, String logicalPath, boolean truncate, boolean append)
			throws IOException, IRODSException {
		open(comm, logicalPath, truncate, append);
	}

	/**
	 * Initializes a new instance and opens a replica for writing.
	 * <p>
	 * If not replica exists, it is created.
	 * <p>
	 * The internal buffer will have a size of 65536.
	 * 
	 * @param comm             The connection to the iRODS server.
	 * @param logicalPath      The absolute logical path to a data object.
	 * @param rootResourceName The root resource which contains the target replica.
	 * @param truncate         Truncate the replica.
	 * @param append           Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectOutputStream(RcComm comm, String logicalPath, String rootResourceName, boolean truncate,
			boolean append) throws IOException, IRODSException {
		open(comm, logicalPath, rootResourceName, truncate, append);
	}

	/**
	 * Initializes a new instance and opens a specific replica for writing.
	 * <p>
	 * This operation will fail if the target replica does not exist.
	 * <p>
	 * The internal buffer will have a size of 65536.
	 * 
	 * @param comm          The connection to the iRODS server.
	 * @param logicalPath   The absolute logical path to a data object.
	 * @param replicaNumber The replica number which identifies the target replica.
	 * @param truncate      Truncate the replica.
	 * @param append        Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectOutputStream(RcComm comm, String logicalPath, long replicaNumber, boolean truncate,
			boolean append) throws IOException, IRODSException {
		open(comm, logicalPath, replicaNumber, truncate, append);
	}

	/**
	 * Initializes a new instance and opens a specific replica for parallel writing.
	 * <p>
	 * The replica is assumed to have been opened prior to this call.
	 * <p>
	 * The internal buffer will have a size of 65536.
	 * 
	 * @param comm          The connection to the iRODS server.
	 * @param replicaToken  The replica access token of the primary output stream.
	 * @param logicalPath   The absolute logical path to a data object.
	 * @param replicaNumber The replica number which identifies the target replica.
	 * @param truncate      Truncate the replica.
	 * @param append        Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectOutputStream(RcComm comm, String replicaToken, String logicalPath, long replicaNumber,
			boolean truncate, boolean append) throws IOException, IRODSException {
		open(comm, replicaToken, logicalPath, replicaNumber, truncate, append);
	}

	/**
	 * Opens a data object for writing.
	 * <p>
	 * If not replica exists, it is created.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * @param truncate    Truncate the replica.
	 * @param append      Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void open(RcComm comm, String logicalPath, boolean truncate, boolean append)
			throws IOException, IRODSException {
		initInternalBufferIfNecessary();
		var mode = (truncate ? OpenFlags.O_TRUNC : 0);
		mode |= (append ? OpenFlags.O_APPEND : 0);
		in.open(comm, logicalPath, OpenFlags.O_CREAT | OpenFlags.O_WRONLY | mode);
		position = 0;
	}

	/**
	 * Opens a replica for writing.
	 * <p>
	 * If no replica exists, it is created.
	 * 
	 * @param comm             The connection to the iRODS server.
	 * @param logicalPath      The absolute logical path to a data object.
	 * @param rootResourceName The root resource which contains the target replica.
	 * @param truncate         Truncate the replica.
	 * @param append           Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void open(RcComm comm, String logicalPath, String rootResourceName, boolean truncate, boolean append)
			throws IOException, IRODSException {
		initInternalBufferIfNecessary();
		var mode = (truncate ? OpenFlags.O_TRUNC : 0);
		mode |= (append ? OpenFlags.O_APPEND : 0);
		in.open(comm, logicalPath, rootResourceName, OpenFlags.O_CREAT | OpenFlags.O_WRONLY | mode);
		position = 0;
	}

	/**
	 * Opens a specific replica for writing.
	 * <p>
	 * This operation will fail if the target replica does not exist.
	 * 
	 * @param comm          The connection to the iRODS server.
	 * @param logicalPath   The absolute logical path to a data object.
	 * @param replicaNumber The replica number which identifies the target replica.
	 * @param truncate      Truncate the replica.
	 * @param append        Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void open(RcComm comm, String logicalPath, long replicaNumber, boolean truncate, boolean append)
			throws IOException, IRODSException {
		initInternalBufferIfNecessary();
		var mode = (truncate ? OpenFlags.O_TRUNC : 0);
		mode |= (append ? OpenFlags.O_APPEND : 0);
		in.open(comm, logicalPath, replicaNumber, OpenFlags.O_WRONLY | mode);
		position = 0;
	}

	/**
	 * Opens a specific replica for parallel writing.
	 * <p>
	 * The replica is assumed to have been opened prior to this call.
	 * 
	 * @param comm          The connection to the iRODS server.
	 * @param replicaToken  The replica access token of the primary output stream.
	 * @param logicalPath   The absolute logical path to a data object.
	 * @param replicaNumber The replica number which identifies the target replica.
	 * @param truncate      Truncate the replica.
	 * @param append        Append new data to replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void open(RcComm comm, String replicaToken, String logicalPath, long replicaNumber, boolean truncate,
			boolean append) throws IOException, IRODSException {
		initInternalBufferIfNecessary();
		var mode = (truncate ? OpenFlags.O_TRUNC : 0);
		mode |= (append ? OpenFlags.O_APPEND : 0);
		in.open(comm, replicaToken, logicalPath, replicaNumber, OpenFlags.O_WRONLY | mode);
		position = 0;
	}

	/**
	 * Checks if the stream is open.
	 * 
	 * @since 0.1.0
	 */
	public boolean isOpen() {
		return in.isOpen();
	}

	/**
	 * Returns the native handle to the stream.
	 * 
	 * @since 0.1.0
	 */
	public int getNativeHandle() {
		return in.getNativeHandle();
	}

	/**
	 * Returns the replica number of the open replica.
	 * 
	 * @since 0.1.0
	 */
	public long getReplicaNumber() {
		return in.getReplicaNumber();
	}

	/**
	 * Returns the replica token associated with the stream.
	 * 
	 * @since 0.1.0
	 */
	public String getReplicaToken() {
		return in.getReplicaToken();
	}

	/**
	 * Changes the position to write to.
	 * 
	 * @param offset    The number of bytes to move.
	 * @param direction Describes how the offset is to be interpreted.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void seek(int offset, SeekDirection direction) throws IOException, IRODSException {
		flushBuffer();
		in.seek(offset, direction);
	}

	@Override
	public void write(int b) throws IOException {
		if (buffer.length == position) {
			try {
				flushBuffer();
			} catch (IRODSException e) {
				throw new IOException(e);
			}
		}

		buffer[position++] = (byte) b;
	}

	public void flushBuffer() throws IOException, IRODSException {
		if (0 == position) {
			return;
		}

		// TODO This is the correct way to flush the buffer.
//        var remaining = position;
//        while (remaining > 0) {
//            // The write() will throw an exception if there's an error.
//            var bytesWritten = in.write(buffer, remaining);
//            remaining -= bytesWritten;
//        }

		// TODO While it's possible for the write() to write a subset of the bytes, no
		// one has ever reported that happening. So for now, this is good enough. Open
		// an issue for this.
		in.write(buffer, position);
		position = 0;
	}

	/**
	 * Closes the stream with additional instructions.
	 * <p>
	 * This option is primarily for use with parallel writes.
	 * 
	 * @param closeInstructions Instructions for the server.
	 * 
	 * @since 0.1.0
	 */
	public void close(OnCloseSuccess closeInstructions) {
		try {
			flushBuffer();
		} catch (IOException | IRODSException e) {
			log.error(e.getMessage());
		}

		try {
			in.close(closeInstructions);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void close() {
		close(null);
	}

	private void initInternalBufferIfNecessary() {
		if (null == buffer) {
			buffer = new byte[65536];
		}
	}

}
