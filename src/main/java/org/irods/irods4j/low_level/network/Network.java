package org.irods.irods4j.low_level.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.XmlUtil;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsgHeader_PI;

public class Network {

	private static final Logger log = LogManager.getLogger();
	
	public static void write(OutputStream out, MsgHeader_PI msgHeader) throws IOException {
		var msg = XmlUtil.toXmlString(msgHeader);

		var bbuf = ByteBuffer.allocate(4);
		bbuf.order(ByteOrder.BIG_ENDIAN);
		bbuf.putInt(msg.getBytes(StandardCharsets.UTF_8).length);

		out.write(bbuf.array());
		out.write(msg.getBytes());

		if (log.isDebugEnabled()) {
			log.debug("Wrote {} bytes", msg.getBytes(StandardCharsets.UTF_8).length);
			log.debug("Message:\n{}", msg);
		}
	}

	public static <T> void writeXml(OutputStream out, T object) throws IOException {
		var msg = XmlUtil.toXmlString(object);
		out.write(msg.getBytes());
		log.debug("Wrote:\n{}", msg);
	}

	public static <T> void writeJson(OutputStream out, T object) throws IOException {
		var msg = JsonUtil.toJsonString(object);
		out.write(msg.getBytes());
		log.debug("Wrote:\n{}", msg);
	}

	public static MsgHeader_PI readMsgHeader_PI(InputStream in) throws IOException {
		var bbuf = ByteBuffer.allocate(4);
		bbuf.order(ByteOrder.BIG_ENDIAN);

		var msgHeaderLengthBytes = in.readNBytes(4);
		bbuf.put(msgHeaderLengthBytes);
		bbuf.flip(); // Flip so we can read from the buffer.
		var msgHeaderLength = bbuf.getInt();

		var msgHeaderBytes = in.readNBytes(msgHeaderLength);
		if (log.isDebugEnabled()) {
			log.debug("Received:\n{}", new String(msgHeaderBytes, StandardCharsets.UTF_8));
		}
		return XmlUtil.fromBytes(msgHeaderBytes, MsgHeader_PI.class);
	}

	public static <T> T readObject(InputStream in, int size, Class<T> clazz) throws IOException {
		var bytes = in.readNBytes(size);
		if (log.isDebugEnabled()) {
			log.debug("Received:\n{}", new String(bytes, StandardCharsets.UTF_8));
		}
		return XmlUtil.fromBytes(bytes, clazz);
	}

	public static byte[] readBytes(InputStream in, int size) throws IOException {
		var bytes = in.readNBytes(size);
		if (log.isDebugEnabled()) {
			log.debug("Received:\n{}", new String(bytes, StandardCharsets.UTF_8));
		}
		return bytes;
	}

	public static void readBytes(InputStream in, byte[] buffer, int size) throws IOException {
		in.readNBytes(buffer, 0, size);
	}

	public static void writeBytes(OutputStream out, byte[] bytes) throws IOException {
		out.write(bytes);
	}

	public static void writeBytes(OutputStream out, byte[] bytes, int count) throws IOException {
		out.write(bytes, 0, count);
	}

}
