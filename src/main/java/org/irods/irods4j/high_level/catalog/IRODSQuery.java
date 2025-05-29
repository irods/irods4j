package org.irods.irods4j.high_level.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.low_level.api.GenQuery1AggregationFunctions;
import org.irods.irods4j.low_level.api.GenQuery1Limits;
import org.irods.irods4j.low_level.api.GenQuery1SortOptions;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSErrorCodes;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.GenQueryInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.GenQueryOut_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.Genquery2Input_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.InxIvalPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.InxValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.SpecificQueryInp_PI;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * A class providing high-level functions for querying the iRODS catalog.
 * 
 * @since 0.1.0
 */
public class IRODSQuery {

	/**
	 * TODO
	 * 
	 * @since 0.1.0
	 */
	public static final class GenQuery1QueryArgs {

		private GenQueryInp_PI input = new GenQueryInp_PI();

		/**
		 * TODO
		 * 
		 * @since 0.1.0
		 */
		public GenQuery1QueryArgs() {
			init();
		}

		/**
		 * TODO
		 * 
		 * @param rows
		 */
		public void setMaxRowsPerPage(int rows) {
			if (rows <= 0) {
				throw new IllegalArgumentException("Max rows per page is less than or equal to 0");
			}
			input.maxRows = rows;
		}

		/**
		 * TODO
		 * 
		 * @param offset
		 */
		public void setRowOffset(int offset) {
			if (offset <= 0) {
				throw new IllegalArgumentException("Row offset is less than or equal to 0");
			}
			input.partialStartIndex = offset;
		}

		/**
		 * TODO
		 * 
		 * @since 0.1.0
		 */
		public void enableReturnTotalRowCount() {
			input.options |= 0x20; // RETURN_TOTAL_ROW_COUNT
		}

		/**
		 * TODO
		 * 
		 * @since 0.1.0
		 */
		public void enableNoDistinct() {
			input.options |= 0x40; // NO_DISTINCT
		}

		/**
		 * TODO
		 * 
		 * @since 0.1.0
		 */
		public void enableQuotaQuery() {
			input.options |= 0x80; // QUOTA_QUERY
		}

		/**
		 * TODO
		 * 
		 * @since 0.1.0
		 */
		public void enableAutoClose() {
			input.options |= 0x100; // AUTO_CLOSE
		}

		/**
		 * TODO
		 * 
		 * @since 0.1.0
		 */
		public void enableUpperCaseWhere() {
			input.options |= 0x200; // UPPER_CASE_WHERE
		}

		/**
		 * TODO
		 * 
		 * @param name
		 * @param value
		 * 
		 * @since 0.1.0
		 */
		public void addApiOption(String name, String value) {
			if (null == name || name.isEmpty()) {
				throw new IllegalArgumentException("Option name is null or empty");
			}

			if (null == value || value.isEmpty()) {
				throw new IllegalArgumentException("Option value is null or empty");
			}

			++input.KeyValPair_PI.ssLen;
			input.KeyValPair_PI.keyWord.add(name);
			input.KeyValPair_PI.svalue.add(value);
		}

		/**
		 * TODO
		 * 
		 * @param columnId
		 * 
		 * @since 0.1.0
		 */
		public void addColumnToSelectClause(int columnId) {
			if (columnId <= 0) {
				throw new IllegalArgumentException("Column Id is less than or equal to 0");
			}

			if (input.InxIvalPair_PI.iiLen == GenQuery1Limits.MAX_SQL_ATTR) {
				throw new IllegalStateException("Max number of columns in SELECT clause has been reached");
			}

			++input.InxIvalPair_PI.iiLen;
			input.InxIvalPair_PI.inx.add(columnId);
			input.InxIvalPair_PI.ivalue.add(0);
		}

		/**
		 * TODO
		 * 
		 * @param columnId
		 * @param sortOption
		 * 
		 * @since 0.1.0
		 */
		public void addColumnToSelectClause(int columnId, GenQuery1SortOptions sortOption) {
			if (columnId <= 0) {
				throw new IllegalArgumentException("Column Id is less than or equal to 0");
			}

			if (input.InxIvalPair_PI.iiLen == GenQuery1Limits.MAX_SQL_ATTR) {
				throw new IllegalStateException("Max number of columns in SELECT clause has been reached");
			}

			if (null == sortOption) {
				throw new IllegalArgumentException("Sort option is null");
			}

			++input.InxIvalPair_PI.iiLen;
			input.InxIvalPair_PI.inx.add(columnId);

			var opt = 0;
			switch (sortOption) {
			case NONE:
				break;
			case ORDER_BY:
				opt = 0x400;
				break;
			case ORDER_BY_DESC:
				opt = 0x800;
				break;
			}

			input.InxIvalPair_PI.ivalue.add(opt);
		}

		/**
		 * TODO
		 * 
		 * @param columnId
		 * @param aggFn
		 * 
		 * @since 0.1.0
		 */
		public void addColumnToSelectClause(int columnId, GenQuery1AggregationFunctions aggFn) {
			if (columnId <= 0) {
				throw new IllegalArgumentException("Column Id is less than or equal to 0");
			}

			if (input.InxIvalPair_PI.iiLen == GenQuery1Limits.MAX_SQL_ATTR) {
				throw new IllegalStateException("Max number of columns in SELECT clause has been reached");
			}

			if (null == aggFn) {
				throw new IllegalArgumentException("Aggregation function is null");
			}

			++input.InxIvalPair_PI.iiLen;
			input.InxIvalPair_PI.inx.add(columnId);

			var opt = 0;
			switch (aggFn) {
			case NONE:
				break;
			case SELECT_MIN:
				opt = 2;
				break;
			case SELECT_MAX:
				opt = 3;
				break;
			case SELECT_SUM:
				opt = 4;
				break;
			case SELECT_AVG:
				opt = 5;
				break;
			case SELECT_COUNT:
				opt = 6;
				break;
			}

			input.InxIvalPair_PI.ivalue.add(opt);
		}

		/**
		 * TODO
		 * 
		 * @param columnId
		 * @param condition
		 * 
		 * @since 0.1.0
		 */
		public void addConditionToWhereClause(int columnId, String condition) {
			if (columnId <= 0) {
				throw new IllegalArgumentException("Column Id is less than or equal to 0");
			}

			if (null == condition || condition.isEmpty()) {
				throw new IllegalArgumentException("Condition string is null or empty");
			}

			++input.InxValPair_PI.isLen;
			input.InxValPair_PI.inx.add(columnId);
			input.InxValPair_PI.svalue.add(condition);
		}

		/**
		 * TODO
		 * 
		 * @return
		 * 
		 * @since 0.1.0
		 */
		public GenQueryInp_PI getGenQueryInp_PI() {
			return input;
		}

		/**
		 * TODO
		 * 
		 * @since 0.1.0
		 */
		public void init() {
			input = new GenQueryInp_PI();
			input.maxRows = GenQuery1Limits.MAX_SQL_ROWS;

			input.KeyValPair_PI = new KeyValPair_PI();
			input.KeyValPair_PI.keyWord = new ArrayList<>();
			input.KeyValPair_PI.svalue = new ArrayList<>();

			// Projection list
			input.InxIvalPair_PI = new InxIvalPair_PI();
			input.InxIvalPair_PI.inx = new ArrayList<>();
			input.InxIvalPair_PI.ivalue = new ArrayList<>();

			// Condition list
			input.InxValPair_PI = new InxValPair_PI();
			input.InxValPair_PI.inx = new ArrayList<>();
			input.InxValPair_PI.svalue = new ArrayList<>();
		}

	}

	/**
	 * Returns the column mappings supported by the GenQuery2 API.
	 * 
	 * @param comm A connection to an iRODS server.
	 * 
	 * @return A set of mappings between the GenQuery2 columns and database columns.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static Map<String, Map<String, String>> getColumnMappings(RcComm comm) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		var input = new Genquery2Input_PI();
		input.column_mappings = 1;

		var output = new Reference<String>();

		var ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		return JsonUtil.fromJsonString(output.value, new TypeReference<Map<String, Map<String, String>>>() {
		});
	}

	/**
	 * Parses a GenQuery2 string and returns the generated SQL.
	 * 
	 * No SQL is executed by this function.
	 * 
	 * @param comm  A connection to an iRODS server.
	 * @param query The GenQuery2 string to execute.
	 * 
	 * @return The SQL generated from the GenQuery2 string.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static String getGeneratedSQL(RcComm comm, String query) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == query || query.isEmpty()) {
			throw new IllegalArgumentException("Query string is null or empty");
		}

		var input = new Genquery2Input_PI();
		input.query_string = query;
		input.sql_only = 1;

		var output = new Reference<String>();

		var ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		return output.value;
	}

	/**
	 * Executes a GenQuery2 query and returns the query results.
	 * 
	 * The query is executed against the zone which the connected iRODS server
	 * resides.
	 * 
	 * @param comm  A connection to an iRODS server.
	 * @param query The GenQuery2 string to execute.
	 * 
	 * @return The resultset as a list of rows.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static List<List<String>> executeGenQuery2(RcComm comm, String query) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == query || query.isEmpty()) {
			throw new IllegalArgumentException("Query string is null or empty");
		}

		var input = new Genquery2Input_PI();
		input.query_string = query;

		var output = new Reference<String>();

		var ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		return JsonUtil.fromJsonString(output.value, new TypeReference<List<List<String>>>() {
		});
	}

	/**
	 * Executes a GenQuery2 query and returns the query results.
	 * 
	 * The query is executed against the zone specified.
	 * 
	 * @param comm  A connection to an iRODS server.
	 * @param zone  The zone to execute the query against.
	 * @param query The GenQuery2 string to execute.
	 * 
	 * @return The resultset as a list of rows.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static List<List<String>> executeGenQuery2(RcComm comm, String zone, String query)
			throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == zone || zone.isEmpty()) {
			throw new IllegalArgumentException("Zone is null or empty");
		}

		if (null == query || query.isEmpty()) {
			throw new IllegalArgumentException("Query string is null or empty");
		}

		var input = new Genquery2Input_PI();
		input.query_string = query;
		input.zone = zone;

		var output = new Reference<String>();

		var ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		return JsonUtil.fromJsonString(output.value, new TypeReference<List<List<String>>>() {
		});
	}

	/**
	 * Executes a GenQuery1 query.
	 * 
	 * This function may result in multiple API calls to the server. To continue
	 * processing rows, return true from the {@code rowHandler}. To end processing
	 * of the rows early, return false from the {@code rowHandler}.
	 * 
	 * @param comm       A connection to an iRODS server.
	 * @param queryArgs  The structure describing the query to execute.
	 * @param rowHandler The callback used to process a single row.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static void executeGenQuery1(RcComm comm, GenQuery1QueryArgs queryArgs,
			Function<List<String>, Boolean> rowHandler) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == queryArgs) {
			throw new IllegalArgumentException("Query arguments is null");
		}

		var input = queryArgs.getGenQueryInp_PI();
		var output = new Reference<GenQueryOut_PI>();

		while (true) {
			var ec = IRODSApi.rcGenQuery(comm, input, output);
			if (ec < 0) {
				if (IRODSErrorCodes.CAT_NO_ROWS_FOUND == ec) {
					break;
				}
				throw new IRODSException(ec, "rcGenQuery error");
			}

			var row = new ArrayList<String>();
			for (var r = 0; r < output.value.rowCnt; ++r) {
				for (var c = 0; c < output.value.attriCnt; ++c) {
					// Get an attribute list. Each SqlResult_PI represents a column containing one
					// piece of of information for each row.
					var sqlResult = output.value.SqlResult_PI.get(c);
					row.add(sqlResult.value.get(r));
				}

				// A return value of false instructs the implementation to stop iterating over
				// the results. This gives the caller the opportunity to exit the loop early.
				if (!rowHandler.apply(Collections.unmodifiableList(row))) {
					return;
				}

				row.clear();
			}

			// There's no more data, exit the loop.
			if (output.value.continueInx <= 0) {
				break;
			}

			// Move to the next page.
			input.continueInx = output.value.continueInx;
		}
	}

	/**
	 * Executes a SpecificQuery.
	 * 
	 * The query is executed against the zone specified.
	 * 
	 * This function may result in multiple API calls to the server. To continue
	 * processing rows, return true from the {@code rowHandler}. To end processing
	 * of the rows early, return false from the {@code rowHandler}.
	 * 
	 * @param comm              A connection to an iRODS server.
	 * @param zone              The zone to execute the query against.
	 * @param specificQueryName The name of the SpecificQuery to execute.
	 * @param bindArgs          The list of bind arguments.
	 * @param rowHandler        The callback used to process a single row.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static void executeSpecificQuery(RcComm comm, String zone, String specificQueryName, List<String> bindArgs,
			Function<List<String>, Boolean> rowHandler) throws IOException, IRODSException {
		executeSpecificQueryImpl(comm, Optional.of(zone), specificQueryName, bindArgs, rowHandler);
	}

	/**
	 * Executes a SpecificQuery.
	 * 
	 * This function may result in multiple API calls to the server. To continue
	 * processing rows, return true from the {@code rowHandler}. To end processing
	 * of the rows early, return false from the {@code rowHandler}.
	 * 
	 * @param comm              A connection to an iRODS server.
	 * @param specificQueryName The name of the SpecificQuery to execute.
	 * @param bindArgs          The list of bind arguments.
	 * @param rowHandler        The callback used to process a single row.
	 * 
	 * @throws IOException    If a network error occurs.
	 * @throws IRODSException If the iRODS API operation fails.
	 * 
	 * @since 0.1.0
	 */
	public static void executeSpecificQuery(RcComm comm, String specificQueryName, List<String> bindArgs,
			Function<List<String>, Boolean> rowHandler) throws IOException, IRODSException {
		executeSpecificQueryImpl(comm, Optional.empty(), specificQueryName, bindArgs, rowHandler);
	}

	private static void executeSpecificQueryImpl(RcComm comm, Optional<String> zone, String specificQueryName,
			List<String> bindArgs, Function<List<String>, Boolean> rowHandler) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == specificQueryName || specificQueryName.isEmpty()) {
			throw new IllegalArgumentException("SpecificQuery name is null or empty");
		}

		if (null == bindArgs) {
			throw new IllegalArgumentException("Bind arguments is null");
		}

		if (bindArgs.size() > 10) {
			throw new IllegalArgumentException("Max number of bind arguments is greater than 10");
		}

		var input = new SpecificQueryInp_PI();
		input.sql = specificQueryName;
		input.maxRows = GenQuery1Limits.MAX_SQL_ROWS;
		input.KeyValPair_PI = new KeyValPair_PI();

		zone.ifPresent(value -> {
			if (null == value || value.isEmpty()) {
				throw new IllegalArgumentException("Zone is null or empty");
			}

			input.KeyValPair_PI.ssLen = 1;
			input.KeyValPair_PI.keyWord = new ArrayList<>();
			input.KeyValPair_PI.svalue = new ArrayList<>();
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.ZONE);
			input.KeyValPair_PI.svalue.add(value);
		});

		for (int i = 0; i < 10 && i < bindArgs.size(); ++i) {
			switch (i) {
			case 0:
				input.arg1 = bindArgs.get(i);
				break;
			case 1:
				input.arg2 = bindArgs.get(i);
				break;
			case 2:
				input.arg3 = bindArgs.get(i);
				break;
			case 3:
				input.arg4 = bindArgs.get(i);
				break;
			case 4:
				input.arg5 = bindArgs.get(i);
				break;
			case 5:
				input.arg6 = bindArgs.get(i);
				break;
			case 6:
				input.arg7 = bindArgs.get(i);
				break;
			case 7:
				input.arg8 = bindArgs.get(i);
				break;
			case 8:
				input.arg9 = bindArgs.get(i);
				break;
			case 9:
				input.arg10 = bindArgs.get(i);
				break;
			}
		}

		var output = new Reference<GenQueryOut_PI>();

		while (true) {
			var ec = IRODSApi.rcSpecificQuery(comm, input, output);
			if (ec < 0) {
				if (IRODSErrorCodes.CAT_NO_ROWS_FOUND == ec) {
					break;
				}
				throw new IRODSException(ec, "rcGenQuery2 error");
			}

			var row = new ArrayList<String>();
			for (var r = 0; r < output.value.rowCnt; ++r) {
				for (var c = 0; c < output.value.attriCnt; ++c) {
					// Get an attribute list. Each SqlResult_PI represents a column containing one
					// piece of of information for each row.
					var sqlResult = output.value.SqlResult_PI.get(c);
					row.add(sqlResult.value.get(r));
				}

				// A return value of false instructs the implementation to stop iterating over
				// the results. This gives the caller the opportunity to exit the loop early.
				if (!rowHandler.apply(Collections.unmodifiableList(row))) {
					return;
				}

				row.clear();
			}

			// There's no more data, exit the loop.
			if (output.value.continueInx <= 0) {
				break;
			}

			// Move to the next page.
			input.continueInx = output.value.continueInx;
		}
	}

}
