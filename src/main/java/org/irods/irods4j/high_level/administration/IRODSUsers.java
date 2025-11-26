package org.irods.irods4j.high_level.administration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.irods.irods4j.common.JsonUtil;
import org.irods.irods4j.common.Reference;
import org.irods.irods4j.common.Versioning;
import org.irods.irods4j.high_level.administration.IRODSZones.ZoneType;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.high_level.catalog.IRODSQuery.GenQuery1QueryArgs;
import org.irods.irods4j.low_level.api.GenQuery1Columns;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.GeneralAdminInp_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.Genquery2Input_PI;
import org.irods.irods4j.low_level.protocol.packing_instructions.UserAdminInp_PI;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * A class providing high-level data types and functions for managing users and
 * groups.
 * 
 * @since 0.1.0
 */
public class IRODSUsers {

	/**
	 * An enumeration which defines all user types supported by iRODS.
	 * 
	 * @since 0.1.0
	 */
	public enum UserType {
		RODSUSER, GROUPADMIN, RODSADMIN, RODSGROUP
	}

	/**
	 * A class designed to represent an iRODS user.
	 * 
	 * @since 0.1.0
	 */
	public static final class User implements Comparable<User> {

		public String name;
		public String zone;

		/**
		 * Initializes a {@link User} without a name or zone.
		 * 
		 * It is the developer's responsibility to make sure objects constructed via
		 * this constructor are updated with non-null values before use.
		 * 
		 * @since 0.1.0
		 */
		public User() {
		}

		/**
		 * Initializes a {@link User} with the given name and zone.
		 * 
		 * @param name The unqualified name component of the username.
		 * @param zone The zone component of the username.
		 * 
		 * @since 0.1.0
		 */
		public User(String name, Optional<String> zone) {
			this.name = name;
			this.zone = zone.orElse("");
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (null == o || getClass() != o.getClass()) {
				return false;
			}

			User other = (User) o;
			return name.equals(other.name) && zone.equals(other.zone);
		}

		@Override
		public int compareTo(User o) {
			int result = name.compareTo(o.name);
			if (0 == result) {
				return zone.compareTo(o.zone);
			}
			return result;
		}

	}

	/**
	 * A class designed to represent an iRODS group.
	 * 
	 * @since 0.1.0
	 */
	public static final class Group implements Comparable<Group> {

		public String name;

		/**
		 * Initializes a {@link Group} without a name.
		 * 
		 * It is the developer's responsibility to make sure objects constructed via
		 * this constructor are updated with non-null values before use.
		 * 
		 * @since 0.1.0
		 */
		public Group() {
		}

		/**
		 * Initializes a {@link Group} with the target name.
		 * 
		 * @param name The name of the group.
		 */
		public Group(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (null == o || getClass() != o.getClass()) {
				return false;
			}

			return name.equals(((Group) o).name);
		}

		@Override
		public int compareTo(Group o) {
			return name.compareTo(o.name);
		}

	}

	/**
	 * Defines the set of operations for manipulating user authentication names.
	 * 
	 * @since 0.1.0
	 */
	public static enum UserAuthenticationOperation {
		ADD, REMOVE
	}

	/**
	 * The base class which modifiable user properties are derived from.
	 * 
	 * @since 0.1.0
	 */
	public static class UserProperty {
	}

	/**
	 * Holds the new password for a user. Primarily used to modify a user.
	 * 
	 * @since 0.1.0
	 */
	public static final class UserPasswordProperty extends UserProperty {
		public String value;
		public String requesterPassword;
	}

	/**
	 * Holds the new type of a user. Primarily used to modify a user.
	 * 
	 * @since 0.1.0
	 */
	public static final class UserTypeProperty extends UserProperty {
		public UserType value;
	}

	/**
	 * TODO Consider removing support for this. It's probably not used by modern
	 * iRODS systems.
	 * 
	 * @since 0.1.0
	 */
	public static final class UserAuthenticationProperty extends UserProperty {
		public UserAuthenticationOperation op;
		public String value;
	}

	/**
	 * Converts a string to a {@link UserType} enumeration.
	 * 
	 * @param v The string to convert.
	 * 
	 * @return The enum representation of the string.
	 * 
	 * @since 0.1.0
	 */
	public static UserType toUserType(String v) {
		if ("rodsuser".equals(v)) {
			return UserType.RODSUSER;
		}

		if ("groupadmin".equals(v)) {
			return UserType.GROUPADMIN;
		}

		if ("rodsadmin".equals(v)) {
			return UserType.RODSADMIN;
		}

		if ("rodsgroup".equals(v)) {
			return UserType.RODSGROUP;
		}

		throw new IllegalArgumentException("User type not supported: " + v);
	}

	/**
	 * Converts a {@link UserType} enumeration to a string.
	 * 
	 * @param v The enumeration to convert.
	 * 
	 * @return The string representation of the enumeration.
	 * 
	 * @since 0.1.0
	 */
	public static String toString(UserType v) {
		switch (v) {
		case RODSUSER:
			return "rodsuser";
		case GROUPADMIN:
			return "groupadmin";
		case RODSADMIN:
			return "rodsadmin";
		}

		throw new IllegalArgumentException("User type not supported");
	}

	/**
	 * Generates the unique name of a user in the local zone.
	 * 
	 * @param comm The connection to the iRODS server.
	 * @param user The user to produce the unique name for.
	 * 
	 * @return A string representing the unique name of the user within the local
	 *         zone.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static String localUniqueName(RcComm comm, User user) throws IOException, IRODSException {
		if (user.zone.isEmpty()) {
			return user.name;
		}

		StringBuilder qualifiedName = new StringBuilder(user.name);

		if (!user.zone.equals(Common.getLocalZone(comm))) {
			qualifiedName.append('#');
			qualifiedName.append(user.zone);
		}

		return qualifiedName.toString();
	}

	/**
	 * Adds a new user to the local zone.
	 * 
	 * @param comm     The connection to the iRODS server.
	 * @param user     The user to add.
	 * @param userType The type of the user.
	 * @param zoneType The zone that is responsible for the user.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addUser(RcComm comm, User user, UserType userType, ZoneType zoneType)
			throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

		if (null == userType) {
			throw new IllegalArgumentException("User type is null");
		}

		if (null == zoneType) {
			throw new IllegalArgumentException("Zone type is null");
		}

		String name = localUniqueName(comm, user);

		String zone = null;
		if (ZoneType.LOCAL == zoneType) {
			zone = Common.getLocalZone(comm);
		}

		User currentUser = new User(comm.clientUsername, Optional.of(comm.clientUserZone));
		Optional<UserType> currentUserType = type(comm, currentUser);

		// We can assume the current user's type will never be empty
		// because the user is constructed using information from the RcComm.
		if (currentUserType.get() == UserType.GROUPADMIN) {
			UserAdminInp_PI input = new UserAdminInp_PI();
			input.arg0 = "mkuser";
			input.arg1 = name;
			// TODO No arg2?
			input.arg3 = zone;

			int ec = IRODSApi.rcUserAdmin(comm, input);
			if (ec < 0) {
				throw new IRODSException(ec, "rcUserAdmin error");
			}

			return;
		}

		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "add";
		input.arg1 = "user";
		input.arg2 = name;
		input.arg3 = toString(userType);
		input.arg4 = zone;

		int ec = IRODSApi.rcGeneralAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGeneralAdmin error");
		}
	}

	/**
	 * Removes a user from the local zone.
	 * 
	 * @param comm The connection to the iRODS server.
	 * @param user The user to remove.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeUser(RcComm comm, User user) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "rm";
		input.arg1 = "user";
		input.arg2 = localUniqueName(comm, user);
		input.arg3 = user.zone;

		int ec = IRODSApi.rcGeneralAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGeneralAdmin error");
		}
	}

	/**
	 * Modifies a property of a user.
	 * 
	 * @param comm     The connection to the iRODS server.
	 * @param user     The user to modify.
	 * @param property An object containing the required information for applying
	 *                 the change.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * @throws NoSuchAlgorithmException
	 * 
	 * @since 0.1.0
	 */
	public static void modifyUser(RcComm comm, User user, UserProperty property)
			throws IOException, IRODSException, NoSuchAlgorithmException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

		if (null == property) {
			throw new IllegalArgumentException("User property is null");
		}

		String name = localUniqueName(comm, user);

		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "modify";
		input.arg1 = "user";
		input.arg2 = name;

		if (property instanceof UserPasswordProperty) {
			UserPasswordProperty p = (UserPasswordProperty) property;
			input.arg3 = "password";
			// TODO Don't rely on obfuscation. Allow clients to send plaintext passwords in
			// the clear. Open an issue in irods/irods for this - assuming we don't have an
			// issue already.
			input.arg4 = obfuscatePassword(p, comm.hashAlgorithm);
		} else if (property instanceof UserTypeProperty) {
			UserTypeProperty p = (UserTypeProperty) property;
			input.arg3 = "type";
			input.arg4 = toString(p.value);
		} else if (property instanceof UserAuthenticationProperty) {
			UserAuthenticationProperty p = (UserAuthenticationProperty) property;
			input.arg4 = p.value;

			if (UserAuthenticationOperation.ADD == p.op) {
				input.arg3 = "addAuth";
			} else if (UserAuthenticationOperation.REMOVE == p.op) {
				input.arg3 = "rmAuth";
			}
		}

		int ec = IRODSApi.rcGeneralAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGeneralAdmin error");
		}
	}

	/**
	 * Adds a new group to the local zone.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group to add.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addGroup(RcComm comm, Group group) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == group) {
			throw new IllegalArgumentException("Group is null");
		}

		String zone = Common.getLocalZone(comm);
		User currentUser = new User(comm.clientUsername, Optional.of(comm.clientUserZone));
		Optional<UserType> currentUserType = type(comm, currentUser);

		// We can assume the current user's type will never be empty
		// because the user is constructed using information from the RcComm.
		if (currentUserType.get() == UserType.GROUPADMIN) {
			UserAdminInp_PI input = new UserAdminInp_PI();
			input.arg0 = "mkgroup";
			input.arg1 = group.name;
			input.arg2 = "rodsgroup";
			input.arg3 = zone;

			int ec = IRODSApi.rcUserAdmin(comm, input);
			if (ec < 0) {
				throw new IRODSException(ec, "rcUserAdmin error");
			}

			return;
		}

		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "add";
		input.arg2 = group.name;
		input.arg4 = zone;

		// For 4.3.4 and later, the API should add a "group" without providing a user type.
		// Any earlier version only knows how to add a "user" of type "rodsgroup" (which is
		// a group).
		if (Versioning.compareVersions(comm.relVersion.substring(4), "4.3.3") > 0) {
			input.arg1 = "group";
		}
		else {
			input.arg1 = "user";
			input.arg3 = "rodsgroup";
		}

		int ec = IRODSApi.rcGeneralAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGeneralAdmin error");
		}
	}

	/**
	 * Removes a group from the local zone.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group to remove.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeGroup(RcComm comm, Group group) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == group) {
			throw new IllegalArgumentException("Group is null");
		}

		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "rm";
		input.arg1 = "group";
		input.arg2 = group.name;
		input.arg3 = Common.getLocalZone(comm);

		int ec = IRODSApi.rcGeneralAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGeneralAdmin error");
		}
	}

	/**
	 * Adds a user to a group.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group to add the user to.
	 * @param user  The user to add to the group.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void addUserToGroup(RcComm comm, Group group, User user) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == group) {
			throw new IllegalArgumentException("Group is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

//		var name = localUniqueName(comm, user);
		User currentUser = new User(comm.clientUsername, Optional.of(comm.clientUserZone));
		Optional<UserType> currentUserType = type(comm, currentUser);

		// We can assume the current user's type will never be empty
		// because the user is constructed using information from the RcComm.
		if (currentUserType.get() == UserType.GROUPADMIN) {
			UserAdminInp_PI input = new UserAdminInp_PI();
			input.arg0 = "modify";
			input.arg1 = "group";
			input.arg2 = group.name;
			input.arg3 = "add";
			input.arg4 = user.name;
			input.arg5 = user.zone;

			int ec = IRODSApi.rcUserAdmin(comm, input);
			if (ec < 0) {
				throw new IRODSException(ec, "rcUserAdmin error");
			}

			return;
		}

		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "modify";
		input.arg1 = "group";
		input.arg2 = group.name;
		input.arg3 = "add";
		input.arg4 = user.name;
		input.arg5 = user.zone;

		int ec = IRODSApi.rcGeneralAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGeneralAdmin error");
		}
	}

	/**
	 * Removes a user from a group.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group that contains the user.
	 * @param user  The user to remove from the group.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static void removeUserFromGroup(RcComm comm, Group group, User user) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == group) {
			throw new IllegalArgumentException("Group is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

//		var name = localUniqueName(comm, user);
		User currentUser = new User(comm.clientUsername, Optional.of(comm.clientUserZone));
		Optional<UserType> currentUserType = type(comm, currentUser);

		// We can assume the current user's type will never be empty
		// because the user is constructed using information from the RcComm.
		if (currentUserType.get() == UserType.GROUPADMIN) {
			UserAdminInp_PI input = new UserAdminInp_PI();
			input.arg0 = "modify";
			input.arg1 = "group";
			input.arg2 = group.name;
			input.arg3 = "remove";
			input.arg4 = user.name;
			input.arg5 = user.zone;

			int ec = IRODSApi.rcUserAdmin(comm, input);
			if (ec < 0) {
				throw new IRODSException(ec, "rcUserAdmin error");
			}

			return;
		}

		GeneralAdminInp_PI input = new GeneralAdminInp_PI();
		input.arg0 = "modify";
		input.arg1 = "group";
		input.arg2 = group.name;
		input.arg3 = "remove";
		input.arg4 = user.name;
		input.arg5 = user.zone;

		int ec = IRODSApi.rcGeneralAdmin(comm, input);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGeneralAdmin error");
		}
	}

	/**
	 * Returns all users in the local zone.
	 * 
	 * The size of the list will be clamped to 100000 elements. If that is too small
	 * for your needs, consider using GenQuery directly.
	 * 
	 * @param comm The connection to the iRODS server.
	 * 
	 * @return A list of users.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static List<User> users(RcComm comm) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		Genquery2Input_PI input = new Genquery2Input_PI();
		input.query_string = "select USER_NAME, USER_ZONE where USER_TYPE != 'rodsgroup' limit 100000";

		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		ArrayList<User> users = new ArrayList<User>();
		TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
		};
		List<List<String>> rows = JsonUtil.fromJsonString(output.value, typeRef);
		if (!rows.isEmpty()) {
			rows.forEach(row -> users.add(new User(row.get(0), Optional.of(row.get(1)))));
		}

		return users;
	}

	/**
	 * Returns all users in a group.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group to check.
	 * 
	 * @return A list of users.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static List<User> users(RcComm comm, Group group) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == group) {
			throw new IllegalArgumentException("Group is null");
		}

		GenQuery1QueryArgs input = new GenQuery1QueryArgs();
		// select USER_NAME, USER_ZONE ...
		input.addColumnToSelectClause(GenQuery1Columns.COL_USER_NAME);
		input.addColumnToSelectClause(GenQuery1Columns.COL_USER_ZONE);
		// where USER_TYPE != 'rodsgroup' and USER_GROUP_NAME = '<name>'
		input.addConditionToWhereClause(GenQuery1Columns.COL_USER_TYPE, "!= 'rodsgroup'");
		input.addConditionToWhereClause(GenQuery1Columns.COL_USER_GROUP_NAME, String.format("= '%s'", group.name));

		ArrayList<User> users = new ArrayList<User>();

		IRODSQuery.executeGenQuery1(comm, input, row -> {
			users.add(new User(row.get(0), Optional.of(row.get(1))));
			return true;
		});

		return users;
	}

	/**
	 * Returns all groups in the local zone.
	 * 
	 * The size of the list will be clamped to 100000 elements. If that is too small
	 * for your needs, consider using GenQuery directly.
	 * 
	 * @param comm The connection to the iRODS server.
	 * 
	 * @return A list of groups.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static List<Group> groups(RcComm comm) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		Genquery2Input_PI input = new Genquery2Input_PI();
		input.query_string = "select USER_NAME where USER_TYPE = 'rodsgroup' limit 100000";

		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		ArrayList<Group> groups = new ArrayList<Group>();
		TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
		};
		List<List<String>> rows = JsonUtil.fromJsonString(output.value, typeRef);
		rows.forEach(row -> groups.add(new Group(row.get(0))));

		return groups;
	}

	/**
	 * Returns all groups a user is a member of.
	 * 
	 * @param comm The connection to the iRODS server.
	 * @param user The user to check for.
	 * 
	 * @return A list of groups.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static List<Group> groups(RcComm comm, User user) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

		ArrayList<Group> groups = new ArrayList<Group>();

		List<String> bindArgs = Arrays.asList(localUniqueName(comm, user));
		IRODSQuery.executeSpecificQuery(comm, "listGroupsForUser", bindArgs, row -> {
			groups.add(new Group(row.get(1)));
			return true;
		});

		return groups;
	}

	/**
	 * Checks if a user exists in the catalog.
	 * 
	 * @param comm The connection to the iRODS server.
	 * @param user The user to find.
	 * 
	 * @return A boolean indicating whether the user exists.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean exists(RcComm comm, User user) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

		Genquery2Input_PI input = new Genquery2Input_PI();
		input.query_string = String.format(
				"select USER_ID where USER_TYPE != 'rodsgroup' and USER_NAME = '%s' and USER_ZONE = '%s'", user.name,
				(user.zone.isEmpty() ? Common.getLocalZone(comm) : user.zone));

		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
		};
		List<List<String>> rows = JsonUtil.fromJsonString(output.value, typeRef);
		return !rows.isEmpty();
	}

	/**
	 * Checks if a group exists in the catalog.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group to find.
	 * 
	 * @return A boolean indicating whether the group exists.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean exists(RcComm comm, Group group) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == group) {
			throw new IllegalArgumentException("Group is null");
		}

		Genquery2Input_PI input = new Genquery2Input_PI();
		input.query_string = String.format("select GROUP_ID where USER_TYPE = 'rodsgroup' and USER_NAME = '%s'",
				group.name);

		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
		};
		List<List<String>> rows = JsonUtil.fromJsonString(output.value, typeRef);
		return !rows.isEmpty();
	}

	/**
	 * Returns the ID of a user.
	 * 
	 * @param comm The connection to the iRODS server.
	 * @param user The user to find.
	 * 
	 * @return An optional which will contain the user's ID if the user exists.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static Optional<String> id(RcComm comm, User user) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

		Genquery2Input_PI input = new Genquery2Input_PI();
		input.query_string = String.format(
				"select USER_ID where USER_TYPE != 'rodsgroup' and USER_NAME = '%s' and USER_ZONE = '%s'", user.name,
				(user.zone.isEmpty() ? Common.getLocalZone(comm) : user.zone));

		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
		};
		List<List<String>> rows = JsonUtil.fromJsonString(output.value, typeRef);
		if (rows.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(rows.get(0).get(0));
	}

	/**
	 * Returns the ID of a group.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group to find.
	 * 
	 * @return An optional which will contain the group's ID if the group exists.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static Optional<String> id(RcComm comm, Group group) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == group) {
			throw new IllegalArgumentException("Group is null");
		}

		Genquery2Input_PI input = new Genquery2Input_PI();
		input.query_string = String.format("select GROUP_ID where USER_TYPE = 'rodsgroup' and USER_NAME = '%s'",
				group.name);

		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
		};
		List<List<String>> rows = JsonUtil.fromJsonString(output.value, typeRef);
		if (rows.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(rows.get(0).get(0));
	}

	/**
	 * Returns the type of a user.
	 * 
	 * @param comm The connection to the iRODS server.
	 * @param user The user to find.
	 * 
	 * @return An optional which will contain the user's type if the user exists.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static Optional<UserType> type(RcComm comm, User user) throws IOException, IRODSException {
		if (null == comm) {
			throw new IllegalArgumentException("RcComm is null");
		}

		if (null == user) {
			throw new IllegalArgumentException("User is null");
		}

		Genquery2Input_PI input = new Genquery2Input_PI();
		input.query_string = String.format(
				"select USER_TYPE where USER_TYPE != 'rodsgroup' and USER_NAME = '%s' and USER_ZONE = '%s'", user.name,
				(user.zone.isEmpty() ? Common.getLocalZone(comm) : user.zone));

		Reference<String> output = new Reference<String>();

		int ec = IRODSApi.rcGenQuery2(comm, input, output);
		if (ec < 0) {
			throw new IRODSException(ec, "rcGenQuery2 error");
		}

		TypeReference<List<List<String>>> typeRef = new TypeReference<List<List<String>>>() {
		};
		List<List<String>> rows = JsonUtil.fromJsonString(output.value, typeRef);
		if (rows.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(toUserType(rows.get(0).get(0)));
	}

	/**
	 * Checks if a user is a member of a group.
	 * 
	 * @param comm  The connection to the iRODS server.
	 * @param group The group to check.
	 * @param user  The user to look for.
	 * 
	 * @return A boolean indicating whether the user is a member of the group.
	 * 
	 * @throws IOException
	 * @throws IRODSException
	 * 
	 * @since 0.1.0
	 */
	public static boolean userIsMemberOfGroup(RcComm comm, Group group, User user) throws IOException, IRODSException {
		GenQuery1QueryArgs input = new GenQuery1QueryArgs();
		// select USER_ID ...
		input.addColumnToSelectClause(GenQuery1Columns.COL_USER_ID);
		// where USER_TYPE != 'rodsgroup' ...
		input.addConditionToWhereClause(GenQuery1Columns.COL_USER_TYPE, "!= 'rodsgroup'");
		// and USER_NAME = '<name>' ...
		input.addConditionToWhereClause(GenQuery1Columns.COL_USER_NAME, String.format("= '%s'", user.name));
		// and USER_ZONE = '<zone>' ...
		String zone = user.zone.isEmpty() ? Common.getLocalZone(comm) : user.zone;
		input.addConditionToWhereClause(GenQuery1Columns.COL_USER_ZONE, String.format("= '%s'", zone));
		// and USER_GROUP_NAME = '<group>'
		input.addConditionToWhereClause(GenQuery1Columns.COL_USER_GROUP_NAME, String.format("= '%s'", group.name));

		ArrayList<String> userIds = new ArrayList<String>();

		IRODSQuery.executeGenQuery1(comm, input, row -> {
			userIds.add(row.get(0));
			return true;
		});

		return !userIds.isEmpty();
	}

	private static String obfuscatePassword(UserPasswordProperty p, String hashAlgo) throws NoSuchAlgorithmException {
		final int MAX_PASSWORD_LEN = 50;

		StringBuilder plainTextPasswordSb = new StringBuilder();
		plainTextPasswordSb.append(p.value);
		plainTextPasswordSb.setLength(MAX_PASSWORD_LEN + 10);

		int count = MAX_PASSWORD_LEN - 10 - p.value.getBytes(StandardCharsets.UTF_8).length;
		if (count > 15) {
			// The random sequence of characters is used for padding and must match
			// what is defined on the server-side.
			plainTextPasswordSb.append("1gCBizHWbwIYyWLoysGzTe6SyzqFKMniZX05faZHWAwQKXf6Fs".substring(0, count));
		}

		StringBuilder keySb = new StringBuilder();
		if (p.requesterPassword.getBytes(StandardCharsets.UTF_8).length >= MAX_PASSWORD_LEN) {
			throw new IllegalArgumentException("Requester password exceeds max key size: " + MAX_PASSWORD_LEN);
		}
		keySb.append(p.requesterPassword.substring(0, Math.min(p.requesterPassword.getBytes(StandardCharsets.UTF_8).length, MAX_PASSWORD_LEN)));

		return obfEncodeByKey(plainTextPasswordSb.toString(), keySb.toString(), hashAlgo);
	}

	// This function is a port of obfEncodeByKey() in
	// irods/irods/lib/core/src/obf.cpp.
	private static String obfEncodeByKey(String data, String key, String hashAlgo) throws NoSuchAlgorithmException {
//		var keyBuf = new int[100 + 1]; // +1 for null terminating byte.
//		var keyBytes = key.getBytes(StandardCharsets.UTF_8);
//		var length = Math.min(keyBuf.length - 1, key.getBytes(StandardCharsets.UTF_8).length);
//		for (int x = 0; x < length; ++x) {
//			keyBuf[x] = (int) (keyBytes[x] & 0xff);
//		}
		int[] keyBuf = new int[100];
		byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
		int length = Math.min(keyBuf.length, key.getBytes(StandardCharsets.UTF_8).length);
		for (int x = 0; x < length; ++x) {
			keyBuf[x] = keyBytes[x] & 0xff;
		}

		// TODO Must also support SHA1.
		// Get the MD5 digest of the key to get some bytes with many different values.
//		var hexKey = obfMakeOneWayHash("md5", keyBuf, keyBuf.length - 1);
//		var buffer = new int[64 + 1]; // Each digest is 16 bytes, 4 of them.
		byte[] hexKey = obfMakeOneWayHash(hashAlgo, keyBuf, keyBuf.length);
		int[] buffer = new int[64]; // Each digest is 16 bytes, 4 of them.
		copyIntArrayToByteArray(buffer, 0, hexKey, hexKey.length);

		// Hash of the hash.
		byte[] v = obfMakeOneWayHash(hashAlgo, buffer, 16);
		copyIntArrayToByteArray(buffer, 16, v, v.length);

		// Hash of two hashes.
		v = obfMakeOneWayHash(hashAlgo, buffer, 32);
		copyIntArrayToByteArray(buffer, 32, v, v.length);

		// Hash of two hashes.
		v = obfMakeOneWayHash(hashAlgo, buffer, 32);
		copyIntArrayToByteArray(buffer, 48, v, v.length);

		final int MAX_PASSWORD_LEN = 50;

		StringBuilder inSb = new StringBuilder(data);
		inSb.setLength(MAX_PASSWORD_LEN + 10);

		StringBuilder outSb = new StringBuilder();
		outSb.setLength(MAX_PASSWORD_LEN + 100);

		// TODO
//		if (HASH_ALGO_SHA1 == defaultHashAlgo) {
//			inSb.setCharAt(cpOut++, 's');
//			inSb.setCharAt(cpOut++, 'h');
//			inSb.setCharAt(cpOut++, 'a');
//			inSb.setCharAt(cpOut++, '1');
//		}

		int[] wheel = new int[26 + 26 + 10 + 15];
		int j = 0;
		for (int i = 0; i < 10; ++i) {
			wheel[j++] = (int) '0' + i;
		}
		for (int i = 0; i < 26; ++i) {
			wheel[j++] = (int) 'A' + i;
		}
		for (int i = 0; i < 26; ++i) {
			wheel[j++] = (int) 'a' + i;
		}
		for (int i = 0; i < 15; ++i) {
			wheel[j++] = (int) '!' + i;
		}

		int cpKey = 0;
		int pc = 0; // Previous character.
		for (int cpIn = 0, cpOut = 0;; ++cpIn) {
			int k = buffer[cpKey++];
			if (cpKey > 60) {
				cpKey = 0;
			}

			boolean found = false;
			for (int i = 0; i < wheel.length; ++i) {
				if ((int) inSb.charAt(cpIn) == wheel[i]) {
					j = i + k + pc;
					j %= wheel.length;
					outSb.setCharAt(cpOut++, (char) (wheel[j] & 0xff));

					// "cipherBlockChaining" is not supported by this implementation. Based on the C
					// implementation, it is only enabled through use of functions which are not
					// supported by this implementation, therefore it is irrelevant.
//					if (cipherBlockChaining) {
//						pc = cpOut - 1;
//					}

					found = true;
					break;
				}
			}

			if (!found) {
				if (inSb.charAt(cpIn) == 0) {
					outSb.setLength(cpOut);
					return outSb.toString();
				} else {
					outSb.setCharAt(cpOut++, inSb.charAt(cpIn));
				}
			}
		}
	}

	private static byte[] obfMakeOneWayHash(String hashAlgo, int[] buffer, int bufferLength)
			throws NoSuchAlgorithmException {
		// Convert int array to a unsigned byte array.
		byte[] bufUnsigned = new byte[bufferLength];
		for (int x = 0; x < bufferLength; ++x) {
			bufUnsigned[x] = (byte) (buffer[x] & 0xff);
		}

		// Hash the buffer.
		MessageDigest hasher = MessageDigest.getInstance(hashAlgo);
		return hasher.digest(bufUnsigned);
	}

	private static void copyIntArrayToByteArray(int[] dst, int dstOffset, byte[] src, int count) {
		for (int i = 0; i < src.length; ++i) {
			dst[i + dstOffset] = src[i] & 0xff;
		}
	}

}
