package org.irods.irods4j.low_level.protocol.packing_instructions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class Version_PI {
	
	public int status = 0;
	public String relVersion = "rods4.3.2";
	public String apiVersion = "d";
	public int reconnPort = 0;
	public String reconnAddr = "";
	public int cookie = 400;
	
	public static void main(String[] args) throws JsonProcessingException {
		XmlMapper m = new XmlMapper();
		System.out.println(m.writeValueAsString(new Version_PI()));
	}

}
