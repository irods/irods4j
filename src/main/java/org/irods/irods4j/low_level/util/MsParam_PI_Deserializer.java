package org.irods.irods4j.low_level.util;

import java.io.IOException;

import org.irods.irods4j.low_level.protocol.packing_instructions.BinBytesBuf_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsParam_PI;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class MsParam_PI_Deserializer extends JsonDeserializer<MsParam_PI> {

	@Override
	public MsParam_PI deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		var v = new MsParam_PI();
		var node = ctxt.readTree(p);

		var targetNode = node.at("/label");
		if (targetNode.isMissingNode()) {
			throw new IOException("MsParam_PI deserialization error: Missing [label]");
		}
		var p1 = targetNode.traverse();
		p1.nextToken();
		v.label = ctxt.readValue(p1, String.class);

		targetNode = node.at("/type");
		if (targetNode.isMissingNode()) {
			throw new IOException("MsParam_PI deserialization error: Missing [type]");
		}
		p1 = targetNode.traverse();
		p1.nextToken();
		v.type = ctxt.readValue(p1, String.class);

		targetNode = node.at("/" + v.type);
		if (targetNode.isMissingNode()) {
			throw new IOException("MsParam_PI deserialization error: Missing [" + v.type + "]");
		}
		p1 = targetNode.traverse();
		p1.nextToken();
		try {
			var clazz = Class.forName("org.irods.irods4j.low_level.protocol.packing_instructions." + v.type);
			v.inOutStruct = ctxt.readValue(p1, clazz);
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}

		targetNode = node.at("/BinBytesBuf_PI");
		if (!targetNode.isMissingNode()) {
			p1 = targetNode.traverse();
			p1.nextToken();
			v.BinBytesBuf_PI = ctxt.readValue(p1, BinBytesBuf_PI.class);
		}

		return v;
	}

}
