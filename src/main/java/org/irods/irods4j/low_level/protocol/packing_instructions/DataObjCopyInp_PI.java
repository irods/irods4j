package org.irods.irods4j.low_level.protocol.packing_instructions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

public class DataObjCopyInp_PI {

	@JacksonXmlElementWrapper(useWrapping = false)
	public DataObjInp_PI[] DataObjInp_PI = new DataObjInp_PI[2];

	public static void main(String[] args) throws JsonProcessingException {
		XmlMapper xm = new XmlMapper();
		DataObjCopyInp_PI v = new DataObjCopyInp_PI();
		System.out.println(xm.writeValueAsString(v));
	}

}
