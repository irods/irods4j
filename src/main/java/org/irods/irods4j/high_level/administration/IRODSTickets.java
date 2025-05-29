package org.irods.irods4j.high_level.administration;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;

import org.irods.irods4j.high_level.common.AdminTag;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSKeywords;
import org.irods.irods4j.low_level.protocol.packing_instructions.KeyValPair_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.TicketAdminInp_PI;

public class IRODSTickets {

	public static enum TicketType {
		READ, WRITE
	}

	/**
	 * Instructs the server to execute operations using rodsadmin level privileges.
	 * 
	 * @since 0.1.0
	 */
	public static final AdminTag asAdmin = AdminTag.instance;

	public static void createTicket(RcComm comm, String ticketName, TicketType ticketType, String logicalPath)
			throws IOException, IRODSException {
		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == ticketType) {
			throw new IllegalArgumentException("Ticket type is null");
		}

		if (null == logicalPath || logicalPath.isEmpty()) {
			throw new IllegalArgumentException("Logical path is null or empty");
		}

		String type = null;
		if (TicketType.READ == ticketType) {
			type = "read";
		} else if (TicketType.WRITE == ticketType) {
			type = "write";
		}

		execTicketOp(comm, false, "create", ticketName, type, logicalPath, ticketName, "");
	}

	public static void createTicket(AdminTag adminTag, RcComm comm, String ticketName, TicketType ticketType,
			String logicalPath) throws IOException, IRODSException {
		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == ticketType) {
			throw new IllegalArgumentException("Ticket type is null");
		}

		if (null == logicalPath || logicalPath.isEmpty()) {
			throw new IllegalArgumentException("Logical path is null or empty");
		}

		String type = null;
		if (TicketType.READ == ticketType) {
			type = "read";
		} else if (TicketType.WRITE == ticketType) {
			type = "write";
		}

		execTicketOp(comm, true, "create", ticketName, type, logicalPath, ticketName, "");
	}

	public static String createTicket(RcComm comm, TicketType ticketType, String logicalPath)
			throws IOException, IRODSException {
		var ticketName = genTicketName();
		createTicket(comm, ticketName, ticketType, logicalPath);
		return ticketName;
	}

	public static String createTicket(AdminTag adminTag, RcComm comm, TicketType ticketType, String logicalPath)
			throws IOException, IRODSException {
		var ticketName = genTicketName();
		createTicket(adminTag, comm, ticketName, ticketType, logicalPath);
		return ticketName;
	}

	public static void deleteTicket(RcComm comm, String ticketName) throws IOException, IRODSException {
		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}
		execTicketOp(comm, false, "delete", ticketName, "", "", "", "");
	}

	public static void deleteTicket(AdminTag adminTag, RcComm comm, String ticketName)
			throws IOException, IRODSException {
		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}
		execTicketOp(comm, true, "delete", ticketName, "", "", "", "");
	}

	public static void deleteTicket(RcComm comm, int ticketId) throws IOException, IRODSException {
		deleteTicket(comm, Integer.toString(ticketId));
	}

	public static void deleteTicket(AdminTag adminTag, RcComm comm, int ticketId) throws IOException, IRODSException {
		deleteTicket(asAdmin, comm, Integer.toString(ticketId));
	}

	public static class TicketConstraint {
	}

	public static class UserConstraint extends TicketConstraint {
		public String value;
	}

	public static class GroupConstraint extends TicketConstraint {
		public String value;
	}

	public static class HostConstraint extends TicketConstraint {
		public String value;
	}

	public static class UseCountConstraint extends TicketConstraint {
		public int value = -1;
	}

	public static class WriteCountToDataObjectConstraint extends TicketConstraint {
		public int value = -1;
	}

	public static class WriteByteCountConstraint extends TicketConstraint {
		public int value = -1;
	}

	public static void addTicketConstraint(RcComm comm, String ticketName, TicketConstraint constraint)
			throws IOException, IRODSException {
		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == constraint) {
			throw new IllegalArgumentException("Ticket constraint is null");
		}

		String et = null; // user, group, or host
		String cv = null; // constraint value

		if (constraint instanceof UserConstraint c) {
			et = "user";
			cv = c.value;
		} else if (constraint instanceof GroupConstraint c) {
			et = "group";
			cv = c.value;
		} else if (constraint instanceof HostConstraint c) {
			et = "host";
			cv = c.value;
		} else {
			throw new IllegalArgumentException("Constraint not supported");
		}

		execTicketOp(comm, false, "mod", ticketName, "add", et, cv, "");
	}

	public static void addTicketConstraint(AdminTag adminTag, RcComm comm, String ticketName,
			TicketConstraint constraint) throws IOException, IRODSException {
		if (null == adminTag) {
			throw new IllegalArgumentException("Admin tag is null");
		}

		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == constraint) {
			throw new IllegalArgumentException("Ticket constraint is null");
		}

		String et = null; // user, group, or host
		String cv = null; // constraint value

		if (constraint instanceof UserConstraint c) {
			et = "user";
			cv = c.value;
		} else if (constraint instanceof GroupConstraint c) {
			et = "group";
			cv = c.value;
		} else if (constraint instanceof HostConstraint c) {
			et = "host";
			cv = c.value;
		} else {
			throw new IllegalArgumentException("Constraint not supported");
		}

		execTicketOp(comm, true, "mod", ticketName, "add", et, cv, "");
	}

	public static void addTicketConstraint(RcComm comm, int ticketId, TicketConstraint constraint)
			throws IOException, IRODSException {
		addTicketConstraint(comm, Integer.toString(ticketId), constraint);
	}

	public static void addTicketConstraint(AdminTag adminTag, RcComm comm, int ticketId, TicketConstraint constraint)
			throws IOException, IRODSException {
		addTicketConstraint(adminTag, comm, Integer.toString(ticketId), constraint);
	}

	public static void setTicketConstraint(RcComm comm, String ticketName, TicketConstraint constraint)
			throws IOException, IRODSException {
		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == constraint) {
			throw new IllegalArgumentException("Ticket constraint is null");
		}

		String property = null;
		int cv = -1;

		if (constraint instanceof UseCountConstraint c) {
			property = "uses";
			cv = c.value;
		} else if (constraint instanceof WriteCountToDataObjectConstraint c) {
			property = "write-file";
			cv = c.value;
		} else if (constraint instanceof WriteByteCountConstraint c) {
			property = "write-bytes";
			cv = c.value;
		} else {
			throw new IllegalArgumentException("Constraint not supported");
		}

		if (cv < 0) {
			throw new IllegalArgumentException("Constraint value is less than 0");
		}

		execTicketOp(comm, false, "mod", ticketName, property, Integer.toString(cv), "", "");
	}

	public static void setTicketConstraint(AdminTag adminTag, RcComm comm, String ticketName,
			TicketConstraint constraint) throws IOException, IRODSException {
		if (null == adminTag) {
			throw new IllegalArgumentException("Admin tag is null");
		}

		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == constraint) {
			throw new IllegalArgumentException("Ticket constraint is null");
		}

		String property = null;
		int cv = -1;

		if (constraint instanceof UseCountConstraint c) {
			property = "uses";
			cv = c.value;
		} else if (constraint instanceof WriteCountToDataObjectConstraint c) {
			property = "write-file";
			cv = c.value;
		} else if (constraint instanceof WriteByteCountConstraint c) {
			property = "write-bytes";
			cv = c.value;
		} else {
			throw new IllegalArgumentException("Constraint not supported");
		}

		if (cv < 0) {
			throw new IllegalArgumentException("Constraint value is less than 0");
		}

		execTicketOp(comm, true, "mod", ticketName, property, Integer.toString(cv), "", "");
	}

	public static void setTicketConstraint(RcComm comm, int ticketId, TicketConstraint constraint)
			throws IOException, IRODSException {
		setTicketConstraint(comm, Integer.toString(ticketId), constraint);
	}

	public static void setTicketConstraint(AdminTag adminTag, RcComm comm, int ticketId, TicketConstraint constraint)
			throws IOException, IRODSException {
		setTicketConstraint(adminTag, comm, Integer.toString(ticketId), constraint);
	}

	public static void removeTicketConstraint(RcComm comm, String ticketName, TicketConstraint constraint)
			throws IOException, IRODSException {
		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == constraint) {
			throw new IllegalArgumentException("Ticket constraint is null");
		}

		if (constraint instanceof UserConstraint c) {
			execTicketOp(comm, false, "mod", ticketName, "remove", "user", c.value, "");
		} else if (constraint instanceof GroupConstraint c) {
			execTicketOp(comm, false, "mod", ticketName, "remove", "group", c.value, "");
		} else if (constraint instanceof HostConstraint c) {
			execTicketOp(comm, false, "mod", ticketName, "remove", "host", c.value, "");
		} else if (constraint instanceof UseCountConstraint c) {
			execTicketOp(comm, false, "mod", ticketName, "users", "0", "", "");
		} else if (constraint instanceof WriteCountToDataObjectConstraint c) {
			execTicketOp(comm, false, "mod", ticketName, "write-file", "0", "", "");
		} else if (constraint instanceof WriteByteCountConstraint c) {
			execTicketOp(comm, false, "mod", ticketName, "write-bytes", "0", "", "");
		} else {
			throw new IllegalArgumentException("Constraint not supported");
		}
	}

	public static void removeTicketConstraint(AdminTag adminTag, RcComm comm, String ticketName,
			TicketConstraint constraint) throws IOException, IRODSException {
		if (null == adminTag) {
			throw new IllegalArgumentException("Admin tag is null");
		}

		if (null == ticketName || ticketName.isEmpty()) {
			throw new IllegalArgumentException("Ticket name is null or empty");
		}

		if (null == constraint) {
			throw new IllegalArgumentException("Ticket constraint is null");
		}

		if (constraint instanceof UserConstraint c) {
			execTicketOp(comm, true, "mod", ticketName, "remove", "user", c.value, "");
		} else if (constraint instanceof GroupConstraint c) {
			execTicketOp(comm, true, "mod", ticketName, "remove", "group", c.value, "");
		} else if (constraint instanceof HostConstraint c) {
			execTicketOp(comm, true, "mod", ticketName, "remove", "host", c.value, "");
		} else if (constraint instanceof UseCountConstraint c) {
			execTicketOp(comm, true, "mod", ticketName, "users", "0", "", "");
		} else if (constraint instanceof WriteCountToDataObjectConstraint c) {
			execTicketOp(comm, true, "mod", ticketName, "write-file", "0", "", "");
		} else if (constraint instanceof WriteByteCountConstraint c) {
			execTicketOp(comm, true, "mod", ticketName, "write-bytes", "0", "", "");
		} else {
			throw new IllegalArgumentException("Constraint not supported");
		}
	}

	public static void removeTicketConstraint(RcComm comm, int ticketId, TicketConstraint constraint)
			throws IOException, IRODSException {
		removeTicketConstraint(comm, Integer.toString(ticketId), constraint);
	}

	public static void removeTicketConstraint(AdminTag adminTag, RcComm comm, int ticketId, TicketConstraint constraint)
			throws IOException, IRODSException {
		removeTicketConstraint(adminTag, comm, Integer.toString(ticketId), constraint);
	}

	private static void execTicketOp(RcComm comm, boolean runAsAdmin, String cmd, String ticketNameOrId, String arg1,
			String arg2, String arg3, String arg4) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		var input = new TicketAdminInp_PI();
		input.arg1 = cmd;
		input.arg2 = ticketNameOrId;
		input.arg3 = arg1;
		input.arg4 = arg2;
		input.arg5 = arg3;
		input.arg6 = arg4;
		input.KeyValPair_PI = new KeyValPair_PI();

		if (runAsAdmin) {
			input.KeyValPair_PI.ssLen = 1;
			input.KeyValPair_PI.keyWord = new ArrayList<>();
			input.KeyValPair_PI.svalue = new ArrayList<>();
			input.KeyValPair_PI.keyWord.add(IRODSKeywords.ADMIN);
			input.KeyValPair_PI.svalue.add("");
		}

		var ec = IRODSApi.rcTicketAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcTicketAdmin error");
		}
	}

	private static String genTicketName() {
		final var chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		var rnd = new SecureRandom();
		var tsb = new StringBuilder();

		for (int i = 0; i < 15; ++i) {
			tsb.append(chars.charAt(rnd.nextInt(chars.length())));
		}

		return tsb.toString();
	}

}
