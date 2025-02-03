package org.irods.irods4j.high_level.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.api.IRODSApi.ByteArrayReference;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.DataObjInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.FileLseekOut_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.OpenedDataObjInp_PI;

/**
 * 
 */
public class IRODSDataObjectStream implements AutoCloseable {

	private static final Logger log = LogManager.getLogger();

	private RcComm comm;
	private int fd = -1;
	private long replicaNumber = -1;
	private String replicaToken;

	/**
	 * 
	 */
	public static class OnCloseSuccess {
		public boolean updateSize = true;
		public boolean updateStatus = true;
		public boolean computeChecksum;
		public boolean sendNotifications;
		public boolean preserveReplicaStateTable;
	}

	/**
	 * 
	 */
	public static enum SeekDirection {
		BEGIN, CURRENT, END
	}

	/**
	 * 
	 * @param comm
	 */
	public IRODSDataObjectStream() {
	}

	/**
	 * 
	 * @param logicalPath
	 * @param openMode
	 * @throws IOException
	 * @throws IRODSException
	 */
	public void open(RcComm comm, String logicalPath, int openMode) throws IOException, IRODSException {
		Optional<Long> replicaNumber = Optional.empty();
		Optional<String> rootResource = Optional.empty();
		Optional<String> replicaToken = Optional.empty();
		openImpl(comm, logicalPath, openMode, replicaNumber, rootResource, replicaToken);
	}

	/**
	 * 
	 * @param logicalPath
	 * @param replicaNumber
	 * @param openMode
	 * @throws IRODSException
	 * @throws IOException
	 */
	public void open(RcComm comm, String logicalPath, long replicaNumber, int openMode)
			throws IOException, IRODSException {
		Optional<String> rootResource = Optional.empty();
		Optional<String> replicaToken = Optional.empty();
		openImpl(comm, logicalPath, openMode, Optional.of(replicaNumber), rootResource, replicaToken);
	}

	/**
	 * 
	 * @param logicalPath
	 * @param rootResource
	 * @param openMode
	 * @throws IRODSException
	 * @throws IOException
	 */
	public void open(RcComm comm, String logicalPath, String rootResource, int openMode)
			throws IOException, IRODSException {
		Optional<Long> replicaNumber = Optional.empty();
		Optional<String> replicaToken = Optional.empty();
		openImpl(comm, logicalPath, openMode, replicaNumber, Optional.of(rootResource), replicaToken);
	}

	/**
	 * 
	 * @param replicaToken
	 * @param logicalPath
	 * @param replicaNumber
	 * @param openMode
	 * @throws IRODSException
	 * @throws IOException
	 */
	public void open(RcComm comm, String replicaToken, String logicalPath, long replicaNumber, int openMode)
			throws IOException, IRODSException {
		Optional<String> rootResource = Optional.empty();
		openImpl(comm, logicalPath, openMode, Optional.of(replicaNumber), rootResource, Optional.of(replicaToken));
	}

	// TODO Consider removing this constructor. The root resource could be a random resource. A resource hierarchy would be better.
//	/**
//	 * 
//	 * @param replicaToken
//	 * @param logicalPath
//	 * @param rootResource
//	 * @param openMode
//	 * @throws IRODSException
//	 * @throws IOException
//	 */
//	public void open(RcComm comm, String replicaToken, String logicalPath, String rootResource, int openMode)
//			throws IOException, IRODSException {
//		Optional<Long> replicaNumber = Optional.empty();
//		openImpl(comm, logicalPath, openMode, replicaNumber, Optional.of(rootResource), Optional.of(replicaToken));
//	}

	/**
	 * 
	 */
	@Override
	public void close() throws Exception {
		close(null);
	}

	/**
	 * 
	 * @param onCloseSuccess
	 * @throws IOException
	 */
	public void close(OnCloseSuccess onCloseSuccess) throws IOException {
		if (-1 == fd) {
			return;
		}

		var input = new HashMap<String, Object>();
		input.put("fd", fd);

		if (null != onCloseSuccess) {
			input.put("update_size", onCloseSuccess.updateSize);
			input.put("update_status", onCloseSuccess.updateStatus);
			input.put("compute_checksum", onCloseSuccess.computeChecksum);
			input.put("send_notifications", onCloseSuccess.sendNotifications);
			input.put("preserve_replica_state_table", onCloseSuccess.preserveReplicaStateTable);
		}

		var ec = IRODSApi.rcReplicaClose(comm, JsonUtil.toJsonString(input));
		if (ec < 0) {
			log.error("rcReplicaClose error");
		}

		fd = -1;
		replicaNumber = -1;
		replicaToken = null;
	}

	/**
	 * 
	 * @param offset
	 * @param direction
	 * @return
	 * @throws IOException
	 * @throws IRODSException
	 */
	public long seek(int offset, SeekDirection direction) throws IOException, IRODSException {
		throwIfInvalidL1Descriptor(fd);

		var input = new OpenedDataObjInp_PI();
		input.l1descInx = fd;
		input.offset = offset;

		switch (direction) {
		case BEGIN:
			input.whence = 0; // SEEK_SET
			break;

		case CURRENT:
			input.whence = 1; // SEEK_CUR
			break;

		case END:
			input.whence = 2; // SEEK_END
			break;
		}

		var output = new Reference<FileLseekOut_PI>();

		var ec = IRODSApi.rcDataObjLseek(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcDataObjLseek error");
		}

		return output.value.offset;
	}

	/**
	 * 
	 * @param buffer
	 * @param count
	 * @return
	 * @throws IOException
	 * @throws IRODSException
	 */
	public int read(ByteArrayReference buffer, int count) throws IOException, IRODSException {
		throwIfInvalidL1Descriptor(fd);
		throwIfInvalidBufferSize(buffer.data.length, count);

		var input = new OpenedDataObjInp_PI();
		input.l1descInx = fd;
		input.len = count;

		var bytesRead = IRODSApi.rcDataObjRead(comm, input, buffer);
		if (bytesRead < 0) {
			throw new IRODSException(bytesRead, "rcDataObjRead error");
		}

		return bytesRead;
	}

	/**
	 * 
	 * @param buffer
	 * @param count
	 * @return
	 * @throws IOException
	 * @throws IRODSException
	 */
	public int write(byte[] buffer, int count) throws IOException, IRODSException {
		throwIfInvalidL1Descriptor(fd);
		throwIfInvalidBufferSize(buffer.length, count);

		var input = new OpenedDataObjInp_PI();
		input.l1descInx = fd;
		input.len = count;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.ssLen = 0;

		var bytesWritten = IRODSApi.rcDataObjWrite(comm, input, buffer);
		if (bytesWritten < 0) {
			throw new IRODSException(bytesWritten, "rcDataObjWrite error");
		}

		return bytesWritten;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isOpen() {
		return fd >= 3;
	}

	/**
	 * 
	 * @return
	 */
	public int getNativeHandle() {
		throwIfInvalidL1Descriptor(fd);
		return fd;
	}

	/**
	 * 
	 * @return
	 */
	public long getReplicaNumber() {
		throwIfInvalidL1Descriptor(fd);
		return replicaNumber;
	}

	/**
	 * 
	 * @return
	 */
	public String getReplicaToken() {
		throwIfInvalidL1Descriptor(fd);
		return replicaToken;
	}

	private void openImpl(RcComm comm, String logicalPath, int openMode, Optional<Long> replicaNumber,
			Optional<String> rootResource, Optional<String> replicaToken) throws IOException, IRODSException {
		if (null == logicalPath || logicalPath.isEmpty()) {
			throw new IllegalArgumentException("Logical path is null or empty");
		}

		var input = new DataObjInp_PI();
		input.objPath = logicalPath;
		input.dataSize = -1;
		input.createMode = 0600;
		input.openFlags = openMode;
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.ssLen = 0;
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();

		replicaNumber.ifPresent(v -> {
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPL_NUM);
			input.KeyValPair_PI.svalue.add(v.toString());
			this.replicaNumber = v;
		});

		rootResource.ifPresent(v -> {
			if (replicaNumber.isPresent()) {
				throw new IllegalStateException("Replica number and root resource cannot be set at the same time");
			}
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.RESC_NAME);
			input.KeyValPair_PI.svalue.add(v);
		});

		replicaToken.ifPresent(v -> {
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.REPLICA_TOKEN);
			input.KeyValPair_PI.svalue.add(v);
			this.replicaToken = v;
		});

		var output = new Reference<String>();

		var ec = IRODSApi.rcReplicaOpen(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcReplicaOpen error");
		}

		fd = ec; // Remember the L1 descriptor.

		// We only maintain a reference to the RcComm if the call to rcReplicaOpen
		// succeeded. It serves no purpose if the open() operation failed.
		this.comm = comm;

		// Capture information from the L1 descriptor JSON payload.
		// This information eases parallel transfer initialization.

		var jm = JsonUtil.getJsonMapper();
		var l1descInfo = jm.readTree(output.value);
		var doi = l1descInfo.get("data_object_info");

		if (-1 == this.replicaNumber) {
			this.replicaNumber = doi.get("replica_number").asLong();
		}

		if (null == this.replicaToken) {
			this.replicaToken = l1descInfo.get("replica_token").asText();
		}
	}

	private static void throwIfInvalidL1Descriptor(int fd) {
		if (-1 == fd) {
			throw new IllegalStateException("Stream not open");
		}
	}

	private static void throwIfInvalidBufferSize(int bufferSize, int count) {
		if (count < 0 || count > bufferSize) {
			throw new IndexOutOfBoundsException("Byte count is less than 0 or exceeds buffer size");
		}
	}

}
