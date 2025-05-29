package org.irods.irods4j.high_level.policy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.ExecCmdOut_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.ExecMyRuleInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsParamArray_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.MsParam_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.RHostAddr_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.STR_PI;

/**
 * A class providing high-level functions for executing iRODS rule code against
 * rule engine plugins.
 * 
 * @since 0.1.0
 */
public class IRODSRules {

	/**
	 * A class used to describe inputs to {@link IRODSRules#executeRule}.
	 * 
	 * @since 0.1.0
	 */
	public static final class RuleArguments {
		public String ruleText;
		public Optional<Map<String, String>> input;
		public Optional<List<String>> output;
		public Optional<String> ruleEnginePluginInstance;
	}

	/**
	 * Queries the connected server for usable rule engine plugin instances.
	 * 
	 * @param comm A connection to a iRODS server.
	 * 
	 * @return A list of usable rule engine plugin instances.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static List<String> getAvailableRuleEnginePluginInstances(RcComm comm) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		var input = new ExecMyRuleInp_PI();
		input.RHostAddr_PI = new RHostAddr_PI();
		input.KeyValPair_PI = new KeyValPair_PI();
		input.KeyValPair_PI.ssLen = 1;
		input.KeyValPair_PI.keyWord = new ArrayList<>();
		input.KeyValPair_PI.svalue = new ArrayList<>();
		input.KeyValPair_PI.keyWord.add(IRODSKeywords.AVAILABLE);
		input.KeyValPair_PI.svalue.add("");
		input.MsParamArray_PI = new MsParamArray_PI();
		input.MsParamArray_PI.paramLen = 0;

		var output = new Reference<MsParamArray_PI>();

		var ec = IRODSApi.rcExecMyRule(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcExecMyRule error");
		}

		// The "good" output is stored in the RcComm's RErrMsg_PI.
		var repInstances = new ArrayList<String>();
		if (comm.rError.count > 0) {
			var lines = comm.rError.RErrMsg_PI.get(0).msg.split("\n");

			// TODO Open an issue to make this better - i.e. return JSON or something.
			// We start at 1 to skip the header line.
			for (var i = 1; i < lines.length; ++i) {
				repInstances.add(lines[i].trim());
			}
		}

		return repInstances;
	}

	/**
	 * Executes rule text/code against the connected server.
	 * 
	 * Targeting a specific rule engine plugin instance is recommended so that error
	 * information is returned by the server. If no rule engine plugin instance is
	 * explicitly targeted, the server will never return any error information. Use
	 * {@code getAvailableRuleEnginePluginInstances} to get the list of available
	 * rule engine plugin instances.
	 * 
	 * @param comm A connection to a iRODS server.
	 * @param args A structure which describes what to execute.
	 * 
	 * @return A map containing the values of requested variables.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static Map<String, String> executeRule(RcComm comm, RuleArguments args) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == args) {
			throw new IllegalArgumentException("Rule arguments is null");
		}

		var input = new ExecMyRuleInp_PI();

		// Unused, but required for proper execution.
		input.RHostAddr_PI = new RHostAddr_PI();

		// The rule text to execute.
		var ruleTextSb = new StringBuilder("@external rule { ");
		ruleTextSb.append(args.ruleText);
		ruleTextSb.append(" }");
		input.myRule = ruleTextSb.toString();

		// The rule engine plugin instance to execute against.
		input.KeyValPair_PI = new KeyValPair_PI();
		args.ruleEnginePluginInstance.ifPresent(instanceName -> {
			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord = new ArrayList<>();
			input.KeyValPair_PI.svalue = new ArrayList<>();
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.INSTANCE_NAME);
			input.KeyValPair_PI.svalue.add(instanceName);
		});

		// The inputs to the rule text.
		input.MsParamArray_PI = new MsParamArray_PI();
		input.MsParamArray_PI.paramLen = 0;
		args.input.ifPresent(m -> {
			m.forEach((name, value) -> {
				++input.MsParamArray_PI.paramLen;

				if (null == input.MsParamArray_PI.MsParam_PI) {
					input.MsParamArray_PI.MsParam_PI = new ArrayList<>();
				}

				var mp = new MsParam_PI();
				mp.label = name;
				mp.type = "STR_PI";
				mp.inOutStruct = new STR_PI();
				((STR_PI) mp.inOutStruct).myStr = value;
				input.MsParamArray_PI.MsParam_PI.add(mp);
			});
		});

		// The variables to return from the server.
		args.output.ifPresent(l -> input.outParamDesc = String.join("%", l));

		var output = new Reference<MsParamArray_PI>();

		var ec = IRODSApi.rcExecMyRule(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcExecMyRule error");
		}

		var results = new HashMap<String, String>();
		for (int i = 0; i < output.value.paramLen; ++i) {
			var msp = output.value.MsParam_PI.get(i);
			if ("STR_PI".equals(msp.type)) {
				results.put(msp.label, ((STR_PI) msp.inOutStruct).myStr);
			} else if ("ExecCmdOut_PI".equals(msp.type)) {
				var ruleExecOut = (ExecCmdOut_PI) msp.inOutStruct;

				// Always fall back to returning empty strings when no data is available. This
				// makes it easier for users of the library to write good code.

				var bbbuf = ruleExecOut.BinBytesBuf_PI.get(0);
				results.put("stdout", (bbbuf.buflen > 0) ? bbbuf.buf.trim() : "");

				bbbuf = ruleExecOut.BinBytesBuf_PI.get(1);
				results.put("stderr", (bbbuf.buflen > 0) ? bbbuf.buf.trim() : "");
			}
		}

		return results;
	}

}
