package org.irods.irods4j.high_level.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.high_level.io.IRODSDataObjectStream.SeekDirection;
import org.irods.irods4j.input_validation.Preconditions;
import org.irods.irods4j.low_level.api.IRODSApi.ByteArrayReference;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI.OpenFlags;

/**
 * A buffered input stream that makes it easy to read iRODS data objects.
 * 
 * @since 0.1.0
 */
public class IRODSDataObjectInputStream extends InputStream {

	private static final Logger log = LogManager.getLogger();

	private IRODSDataObjectStream in = new IRODSDataObjectStream();
	private ByteArrayReference byteArrRef = new ByteArrayReference();
	private byte[] buffer;
	private int bytesInBuffer;
	private int position;

	/**
	 * Initializes a new instance with a buffer size of 65536 bytes.
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectInputStream() {
		buffer = new byte[65536];
		bytesInBuffer = buffer.length;
		byteArrRef.data = buffer;
	}

	/**
	 * Initializes a new instance with a custom buffer size.
	 * 
	 * @param bufferSize The size of the internal buffer.
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectInputStream(int bufferSize) {
		Preconditions.greaterThanOrEqualToValue(bufferSize, 1, "Buffer size is less than 1");
		buffer = new byte[bufferSize];
		this.bytesInBuffer = bufferSize;
		byteArrRef.data = buffer;
	}

	/**
	 * Initializes a new instance and opens a data object for reading.
	 * <p>
	 * The internal buffer will have a size of 65536.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectInputStream(RcComm comm, String logicalPath) throws IOException, IRODSException {
		open(comm, logicalPath);
	}

	/**
	 * Initializes a new instance and opens a replica for reading.
	 * <p>
	 * The internal buffer will have a size of 65536.
	 * 
	 * @param comm             The connection to the iRODS server.
	 * @param logicalPath      The absolute logical path to a data object.
	 * @param rootResourceName The root resource which contains the target replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectInputStream(RcComm comm, String logicalPath, String rootResourceName)
			throws IOException, IRODSException {
		open(comm, logicalPath, rootResourceName);
	}

	/**
	 * Initializes a new instance and opens a specific replica for reading.
	 * <p>
	 * The internal buffer will have a size of 65536.
	 * 
	 * @param comm          The connection to the iRODS server.
	 * @param logicalPath   The absolute logical path to a data object.
	 * @param replicaNumber The replica number which identifies the target replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public IRODSDataObjectInputStream(RcComm comm, String logicalPath, long replicaNumber)
			throws IOException, IRODSException {
		open(comm, logicalPath, replicaNumber);
	}

	/**
	 * Opens a data object for reading.
	 * 
	 * @param comm        The connection to the iRODS server.
	 * @param logicalPath The absolute logical path to a data object.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void open(RcComm comm, String logicalPath) throws IOException, IRODSException {
		initInternalBufferIfNecessary();
		in.open(comm, logicalPath, OpenFlags.O_RDONLY);
		position = bytesInBuffer;
	}

	/**
	 * Opens a replica for reading.
	 * 
	 * @param comm             The connection to the iRODS server.
	 * @param logicalPath      The absolute logical path to a data object.
	 * @param rootResourceName The root resource which contains the target replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void open(RcComm comm, String logicalPath, String rootResourceName) throws IOException, IRODSException {
		initInternalBufferIfNecessary();
		in.open(comm, logicalPath, rootResourceName, OpenFlags.O_RDONLY);
		position = bytesInBuffer;
	}

	/**
	 * Opens a specific replica for reading.
	 * 
	 * @param comm          The connection to the iRODS server.
	 * @param logicalPath   The absolute logical path to a data object.
	 * @param replicaNumber The replica number which identifies the target replica.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public void open(RcComm comm, String logicalPath, long replicaNumber) throws IOException, IRODSException {
		initInternalBufferIfNecessary();
		in.open(comm, logicalPath, replicaNumber, OpenFlags.O_RDONLY);
		position = bytesInBuffer;
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
	 * Changes the position to read from.
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
		// Force the next read operation to fetch new data. This guarantees the client
		// is working with up-to-date data, especially after the seek operation
		// succeeds.
		position = bytesInBuffer;
		in.seek(offset, direction);
	}

	@Override
	public int read() throws IOException {
		// If we've read all the contents of the buffer, fill it with new data.
		if (bytesInBuffer == position) {
			try {
				var bytesRead = in.read(byteArrRef, buffer.length);
				if (0 == bytesRead) {
					return -1;
				}

				bytesInBuffer = bytesRead;
				position = 0;
			} catch (IOException | IRODSException e) {
				throw new IOException(e);
			}
		}

		return buffer[position++];
	}

	@Override
	public void close() {
		try {
			in.close();
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private void initInternalBufferIfNecessary() {
		if (null != buffer) {
			return;
		}

		buffer = new byte[65536];
		bytesInBuffer = buffer.length;
		byteArrRef.data = buffer;
	}

}
