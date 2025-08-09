package org.irods.irods4j.high_level.connection;

import java.io.IOException;
import java.util.Optional;

import org.irods.irods4j.authentication.AuthPlugin;
import org.irods.irods4j.low_level.api.IRODSApi;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.ConnectionOptions;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;
import org.irods.irods4j.low_level.protocol.packing_instructions.RErrMsg_PI;

/**
 * A class designed to ease the management of a connection to an iRODS server.
 * 
 * @since 0.1.0
 */
public class IRODSConnection implements AutoCloseable {

	private String host;
	private int port;

	private QualifiedUsername clientUser;
	private QualifiedUsername proxyUser;

	private ConnectionOptions connOptions;
	private RcComm comm;

	/**
	 * Initializes a newly created iRODS connection using default connection
	 * options.
	 * 
	 * See {@link ConnectionOptions} for details regarding the default connection
	 * options.
	 * 
	 * No connection is established at this time.
	 * 
	 * @since 0.1.0
	 */
	public IRODSConnection() {
		connOptions = new ConnectionOptions();
	}

	/**
	 * Initializes a newly created iRODS connection using custom connection options.
	 * 
	 * No connection is established at this time.
	 * 
	 * @since 0.1.0
	 */
	public IRODSConnection(ConnectionOptions options) {
		if (null == options) {
			throw new IllegalArgumentException("Connection options is null");
		}
		connOptions = options.copy();
	}

	/**
	 * Connects to an iRODS server.
	 * 
	 * This operation only establishes a connection. No authentication is performed.
	 * 
	 * @param host       The hostname or IP address of the iRODS server.
	 * @param port       The port number of the iRODS server.
	 * @param clientUser The iRODS user to connect as.
	 * 
	 * @throws IllegalArgumentException If any of the constructor requirements is
	 *                                  violated.
	 * @throws IRODSException           If the iRODS server experiences an error.
	 * 
	 * @since 0.1.0
	 */
	public void connect(String host, int port, QualifiedUsername clientUser) throws Exception {
		throwIfInvalidHost(host);
		throwIfInvalidPortNumber(port);
		throwIfInvalidClientUser(clientUser);

		this.host = host;
		this.port = port;
		this.clientUser = clientUser;

		doConnect();
	}

	/**
	 * Connects to an iRODS server.
	 * 
	 * The connection will represent a proxied connection. The proxy user provides
	 * the connection to the server. All API operations honor the permissions of the
	 * client user.
	 * 
	 * This form of {@code connect} is most useful in situations where rodsadmin
	 * credentials are available and the application is presenting as a server.
	 * 
	 * This operation only establishes a connection. No authentication is performed.
	 * 
	 * @param host       The hostname or IP address of the iRODS server.
	 * @param port       The port number of the iRODS server.
	 * @param proxyUser  The iRODS user acting as the proxy.
	 * @param clientUser The iRODS user being proxied.
	 * 
	 * @throws IllegalArgumentException If any of the constructor requirements is
	 *                                  violated.
	 * @throws IRODSException           If the iRODS server experiences an error.
	 * 
	 * @since 0.1.0
	 */
	public void connect(String host, int port, QualifiedUsername proxyUser, QualifiedUsername clientUser)
			throws Exception {
		throwIfInvalidHost(host);
		throwIfInvalidPortNumber(port);
		throwIfInvalidProxyUser(proxyUser);
		throwIfInvalidClientUser(clientUser);

		this.host = host;
		this.port = port;
		this.clientUser = clientUser;
		this.proxyUser = proxyUser;

		doConnect();
	}

	/**
	 * Reconnects to an iRODS server using existing connection information.
	 * 
	 * This method is only useful following a call to {@code disconnect}.
	 * 
	 * Invoking this method before {@code connect} is undefined.
	 * 
	 * @throws IllegalArgumentException If any of the requirements is violated.
	 * @throws IRODSException           If the iRODS server experiences an error.
	 * @throws Exception                If an error occurs.
	 * 
	 * @since 0.1.0
	 */
	public void connect() throws Exception {
		doConnect();
	}

	/**
	 * Performs authentication.
	 * 
	 * @param authPlugin The authentication plugin to use.
	 * @param password   The password of the proxy user (if defined), or the client
	 *                   user.
	 * 
	 * @throws Exception If an error occurs.
	 * 
	 * @since 0.1.0
	 */
	public void authenticate(AuthPlugin authPlugin, String password) throws Exception {
		IRODSApi.rcAuthenticateClient(comm, authPlugin, password);
	}

	/**
	 * Returns {@code true} if the connection is active.
	 * 
	 * @since 0.1.0
	 */
	public boolean isConnected() {
		return (null != comm) && comm.socket.isConnected();
	}

	/**
	 * Returns a handle to the underlying {@link RcComm}.
	 * 
	 * @throws IllegalStateException If no connection has been established with the
	 *                               server.
	 * 
	 * @since 0.1.0
	 */
	public RcComm getRcComm() {
		if (!isConnected()) {
			throw new IllegalStateException("No active connection to server");
		}
		return comm;
	}

	/**
	 * Returns the underlying handle and releases ownership.
	 * 
	 * {@code getRcComm} will throw an exception following this call.
	 * 
	 * @since 0.1.0
	 */
	public RcComm release() {
		var connection = getRcComm();
		comm = null;
		return connection;
	}

	/**
	 * Disconnects from an iRODS server.
	 * 
	 * Use of the class instance
	 * 
	 * @throws IOException If a network error occurs.
	 * 
	 * @since 0.1.0
	 */
	public void disconnect() throws IOException {
		if (null == comm) {
			return;
		}

		try {
			IRODSApi.rcDisconnect(comm);
		} finally {
			comm = null;
		}
	}

	/**
	 * Enables try-with-resources pattern.
	 * 
	 * Prefer {@code disconnect}.
	 * 
	 * @since 0.1.0
	 */
	@Override
	public void close() throws Exception {
		disconnect();
	}

	private static void throwIfInvalidHost(String host) {
		if (null == host || host.isEmpty()) {
			throw new IllegalArgumentException("Host is null or empty");
		}
	}

	private static void throwIfInvalidPortNumber(int port) {
		if (port <= 0) {
			throw new IllegalArgumentException("Port is less than or equal to 0");
		}
	}

	private static void throwIfInvalidProxyUser(QualifiedUsername user) {
		if (null == user) {
			throw new IllegalArgumentException("Proxy user is null");
		}
	}

	private static void throwIfInvalidClientUser(QualifiedUsername user) {
		if (null == user) {
			throw new IllegalArgumentException("Client user is null");
		}
	}

	private void doConnect() throws Exception {
		disconnect();

		Optional<String> proxyUserName = Optional.empty();
		Optional<String> proxyUserZone = Optional.empty();
		if (null != proxyUser) {
			proxyUserName = Optional.of(proxyUser.getName());
			proxyUserZone = Optional.of(proxyUser.getZone());
		}

		var options = Optional.of(connOptions);
		var errInfo = Optional.of(new RErrMsg_PI());

		comm = IRODSApi.rcConnect(host, port, clientUser.getName(), clientUser.getZone(), proxyUserName, proxyUserZone,
				options, errInfo);

		if (null == comm || errInfo.get().status < 0) {
			throw new IRODSException(errInfo.get().status, "rcConnect error");
		}
	}

}
