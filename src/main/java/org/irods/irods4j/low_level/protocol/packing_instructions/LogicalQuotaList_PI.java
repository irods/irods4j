package org.irods.irods4j.low_level.protocol.packing_instructions;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;

@JsonRootName("logicalQuotaList_PI")
public class LogicalQuotaList_PI {

	public int len;

	@JacksonXmlElementWrapper(useWrapping = false)
	public List<LogicalQuota_PI> logicalQuota_PI;

}
