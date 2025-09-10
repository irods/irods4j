package org.irods.irods4j.low_level.protocol.packing_instructions;

import org.irods.irods4j.low_level.util.MsParam_PI_Deserializer;
import org.irods.irods4j.low_level.util.MsParam_PI_Serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.nio.charset.StandardCharsets;

@JsonSerialize(using = MsParam_PI_Serializer.class)
@JsonDeserialize(using = MsParam_PI_Deserializer.class)
public class MsParam_PI {

	public String label;
	// The packing instruction describing how to deserialize inOutStruct.
	public String type;
	// The object to serialize/deserialize.
	public Object inOutStruct;
	public BinBytesBuf_PI BinBytesBuf_PI;

	public static void main(String[] args) throws JsonProcessingException {
		System.out.println("--- SERIALIZING OBJECT ---");

		var inoutstruct = new STR_PI();
		inoutstruct.myStr = "5";

		var mp = new MsParam_PI();
		mp.label = "*x";
		mp.type = "STR_PI";
		mp.inOutStruct = inoutstruct;
		mp.BinBytesBuf_PI = new BinBytesBuf_PI();
		mp.BinBytesBuf_PI.buf = "testing testing 1 2 3";
		mp.BinBytesBuf_PI.buflen = mp.BinBytesBuf_PI.buf.getBytes(StandardCharsets.UTF_8).length;

		var xm = new XmlMapper();
		xm.enable(SerializationFeature.INDENT_OUTPUT);
		var v = xm.writeValueAsString(mp);
		System.out.println(v);
		
		System.out.println("--- DESERIALIZING STRING ---");
		mp = xm.readValue(v, MsParam_PI.class);
		System.out.println(mp.label);
		System.out.println(mp.type);
		inoutstruct = (STR_PI) mp.inOutStruct;
		System.out.println(inoutstruct.myStr);
		System.out.println(mp.BinBytesBuf_PI.buf);
	}

}
