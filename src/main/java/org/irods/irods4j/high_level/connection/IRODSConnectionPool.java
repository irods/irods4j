package org.irods.irods4j.high_level.connection;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.irods.irods4j.common.Versioning;
import org.irods.irods4j.high_level.catalog.IRODSQuery;
import org.irods.irods4j.low_level.api.IRODSException;
import org.irods.irods4j.low_level.api.IRODSApi.ConnectionOptions;
import org.irods.irods4j.low_level.api.IRODSApi.RcComm;

/**
 * A class which manages a pool of iRODS connections.
 * 
 * @since 0.1.0
 */
public class IRODSConnectionPool implements AutoCloseable {

	private static final Logger log = LogManager.getLogger();

	private ConnectionOptions connOptions;
	private ConnectionPoolOptions poolOptions;
	private int poolSize;

	private String host;
	private int port;
	private QualifiedUsername clientUser;
	private Function<RcComm, Boolean> authenticator;

	private List<ConnectionContext> pool;

	/**
	 * A class representing a connection within an {@link IRODSConnectionPool}.
	 * 
	 * @since 0.1.0
	 */
	public static final class PoolConnection implements AutoCloseable {

		private ConnectionContext ctx;

		private PoolConnection(ConnectionContext ctx) {
			this.ctx = ctx;
		}

		/**
		 * Checks if the connection to iRODS is usable.
		 * 
		 * @since 0.1.0
		 */
		public boolean isValid() {
			return ctx.conn.isConnected();
		}

		/**
		 * Returns the {@link RcComm} managed by the connection.
		 * 
		 * @since 0.1.0
		 */
		public RcComm getRcComm() {
			return ctx.conn.getRcComm();
		}

		/**
		 * Returns the connection to the pool.
		 * 
		 * @since 0.1.0
		 */
		@Override
		public void close() throws Exception {
			ctx.inUse.set(false);
		}

	}

	/**
	 * Initializes a newly created connection pool with default
	 * {@link ConnectionOptions}, default {@link ConnectionPoolOptions}, and space
	 * for the requested number of iRODS connections.
	 * 
	 * @param poolSize The number of connections the pool should manage.
	 * 
	 * @throws IllegalArgumentException If pool size is less than or equal to 0.
	 * 
	 * @since 0.1.0
	 */
	public IRODSConnectionPool(int poolSize) {
		throwIfLessThanOrEqualTo(poolSize, 0, "Connection pool size is less than or equal to 0");
		doConstructor(new ConnectionOptions(), new ConnectionPoolOptions(), poolSize);
	}

	/**
	 * Initializes a newly created connection pool with default a
	 * {@link ConnectionOptions}, a user-defined {@link ConnectionPoolOptions}, and
	 * space for the requested number of iRODS connections.
	 * 
	 * @param poolOptions Options which influence the behavior of connection pool.
	 * @param poolSize    The number of connections the pool should manage.
	 * 
	 * @throws IllegalArgumentException If pool size is less than or equal to 0.
	 * 
	 * @since 0.1.0
	 */
	public IRODSConnectionPool(ConnectionPoolOptions poolOptions, int poolSize) {
		throwIfNull(poolOptions, "Connection pool options is null");
		throwIfLessThanOrEqualTo(poolSize, 0, "Connection pool size is less than or equal to 0");
		doConstructor(new ConnectionOptions(), poolOptions, poolSize);
	}

	/**
	 * Initializes a newly created connection pool with a user-defined
	 * {@link ConnectionOptions}, default {@link ConnectionPoolOptions}, and space
	 * for the requested number of iRODS connections.
	 * 
	 * @param connOptions The connection options to use when establishing
	 *                    connections to the iRODS server.
	 * @param poolSize    The number of connections the pool should manage.
	 * 
	 * @throws IllegalArgumentException If connection options is null or the pool
	 *                                  size is less than or equal to 0.
	 * 
	 * @since 0.1.0
	 */
	public IRODSConnectionPool(ConnectionOptions connOptions, int poolSize) {
		throwIfNull(connOptions, "Connection options is null");
		throwIfLessThanOrEqualTo(poolSize, 0, "Connection pool size is less than or equal to 0");
		doConstructor(connOptions, new ConnectionPoolOptions(), poolSize);
	}

	/**
	 * Initializes a newly created connection pool.
	 * 
	 * @param connOptions The connection options to use when establishing
	 *                    connections to the iRODS server.
	 * @param poolOptions Options which influence the behavior of connection pool.
	 * @param poolSize    The number of connections the pool should manage.
	 * 
	 * @throws IllegalArgumentException If any constructor argument is null or out
	 *                                  of range.
	 * 
	 * @since 0.1.0
	 */
	public IRODSConnectionPool(ConnectionOptions connOptions, ConnectionPoolOptions poolOptions, int poolSize) {
		throwIfNull(connOptions, "Connection options is null");
		throwIfLessThanOrEqualTo(poolSize, 0, "Connection pool size is less than or equal to 0");
		doConstructor(connOptions, poolOptions, poolSize);
	}

	/**
	 * Sets the options to use when connecting to the iRODS server.
	 * 
	 * This only takes affect when starting the connection pool.
	 * 
	 * @param connOptions The connection options to use when establishing
	 *                    connections to the iRODS server.
	 *
	 * @throws IllegalArgumentException If the connection options are null.
	 * 
	 * @since 0.1.0
	 */
	public void setConnectionOptions(ConnectionOptions connOptions) {
		throwIfNull(connOptions, "Connection options is null");
		this.connOptions = connOptions;
	}

	/**
	 * Sets the number of connections the pool should manage.
	 * 
	 * This only takes affect when starting the connection pool.
	 * 
	 * @param poolSize The number of connections the pool should manage.
	 * 
	 * @throws IllegalArgumentException If the pool size is less than or equal to 0.
	 * 
	 * @since 0.1.0
	 */
	public void setPoolSize(int poolSize) {
		throwIfLessThanOrEqualTo(poolSize, 0, "Connection pool size is less than or equal to 0");
		this.poolSize = poolSize;
	}

	/**
	 * Returns the number of connections managed by the pool.
	 * 
	 * @since 0.1.0
	 */
	public int getPoolSize() {
		return pool.size();
	}

	/**
	 * Synchronously establishes one or more connections to an iRODS server and
	 * authenticates each one using the provided authentication callback.
	 * 
	 * @param host          The hostname or IP of the iRODS server to connect to.
	 * @param port          The port number of the iRODS server to connect to.
	 * @param clientUser    The iRODS user to connect as.
	 * @param authenticator The callback to use for authentication.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @throws IllegalArgumentException If any of the construction arguments is null
	 *                                  or empty.
	 * 
	 * @since 0.1.0
	 */
	public void start(String host, int port, QualifiedUsername clientUser, Function<RcComm, Boolean> authenticator)
			throws IOException, IRODSException {
		throwIfInvalidHost(host);
		throwIfInvalidPortNumber(port);
		throwIfInvalidClientUser(clientUser);

		this.host = host;
		this.port = port;
		this.clientUser = clientUser;
		this.authenticator = authenticator;

		doStart(Optional.empty());
	}

	/**
	 * Asynchronously establishes one or more connections to an iRODS server using
	 * the provided {@link ExecutorService} and authenticates each one using the
	 * provided authentication callback.
	 * 
	 * @param executor      The {@link ExecutorService} to improved connection
	 *                      startup performance.
	 * @param host          The hostname or IP of the iRODS server to connect to.
	 * @param port          The port number of the iRODS server to connect to.
	 * @param clientUser    The iRODS user to connect as.
	 * @param authenticator The callback to use for authentication.
	 * 
	 * @throws IRODSException
	 * @throws IOException
	 * 
	 * @throws IllegalArgumentException If any of the construction arguments is null
	 *                                  or empty.
	 * 
	 * @since 0.1.0
	 */
	public void start(ExecutorService executor, String host, int port, QualifiedUsername clientUser,
			Function<RcComm, Boolean> authenticator) throws IOException, IRODSException {
		throwIfNull(executor, "Executor service is null");
		throwIfInvalidHost(host);
		throwIfInvalidPortNumber(port);
		throwIfInvalidClientUser(clientUser);

		this.host = host;
		this.port = port;
		this.clientUser = clientUser;
		this.authenticator = authenticator;

		doStart(Optional.of(executor));
	}

	/**
	 * Shuts down all pooled connections to the iRODS server.
	 * 
	 * All pooled connections should be returned to the pool before invoking this
	 * function.
	 * 
	 * @since 0.1.0
	 */
	public void stop() {
		pool.forEach(ctx -> {
			try {
				if (null != ctx.conn) {
					ctx.conn.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Equivalent to {@link IRODSConnectionPool#stop()}.
	 * 
	 * @since 0.1.0
	 */
	@Override
	public void close() throws Exception {
		stop();
	}

	/**
	 * Returns a usable connection from the pool.
	 * 
	 * @since 0.1.0
	 */
	public PoolConnection getConnection() {
		for (int i = 0;; i = (i + 1) % pool.size()) {
			var ctx = pool.get(i);

			if (ctx.lock.tryLock()) {
				try {
					if (!ctx.inUse.get()) {
						ctx.inUse.set(true);
						refreshConnection(ctx);

						if (poolOptions.numberOfRetrievalsBeforeConnectionRefresh.isPresent()) {
							++ctx.retrievalCount;
						}

						return new PoolConnection(ctx);
					}
				} finally {
					ctx.lock.unlock();
				}
			}
		}
	}

	private static void throwIfNull(Object object, String msg) {
		if (null == object) {
			throw new IllegalArgumentException(msg);
		}
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

	private static void throwIfInvalidClientUser(QualifiedUsername user) {
		if (null == user) {
			throw new IllegalArgumentException("Client user is null");
		}
	}

	private static void throwIfLessThanOrEqualTo(int value, int lowerBound, String msg) {
		if (value <= lowerBound) {
			throw new IllegalArgumentException(msg);
		}
	}

	private static final class ConnectionContext {
		Lock lock = new ReentrantLock();
		AtomicBoolean inUse = new AtomicBoolean();
		IRODSConnection conn;
		long ctime;
		String latestRescMTime;
		int rescCount;
		int retrievalCount;
	}

	private void doConstructor(ConnectionOptions connOptions, ConnectionPoolOptions poolOptions, int poolSize) {
		poolOptions.numberOfRetrievalsBeforeConnectionRefresh.ifPresent(v -> {
			throwIfLessThanOrEqualTo(v, 0,
					"Connection pool option [numberOfRetrievalsBeforeConnectionRefresh] is less than or equal to 0");
		});

		poolOptions.numberOfSecondsBeforeConnectionRefresh.ifPresent(v -> {
			throwIfLessThanOrEqualTo(v, 0,
					"Connection pool option [numberOfSecondsBeforeConnectionRefresh] is less than or equal to 0");
		});

		this.connOptions = connOptions;
		this.poolOptions = poolOptions;
		this.poolSize = poolSize;
		pool = new ArrayList<>(poolSize);
	}

	private void doStart(Optional<ExecutorService> executor) throws IOException, IRODSException {
		pool.clear();
		for (int i = 0; i < poolSize; ++i) {
			pool.add(new ConnectionContext());
		}

		// Signals which indicate whether an unrecoverable error occurred while
		// establishing a connection or authenticating with the server.
		var connectFailed = new AtomicBoolean();
		var authFailed = new AtomicBoolean();

		// Connect to the iRODS server and authenticate.
		if (executor.isPresent()) {
			var futures = new ArrayList<Future<?>>();
			pool.forEach(ctx -> {
				futures.add(executor.get().submit(() -> {
					var conn = new IRODSConnection(connOptions);
					try {
						conn.connect(host, port, clientUser);
					} catch (Exception e) {
						connectFailed.set(true);
						return;
					}

					if (!authenticator.apply(conn.getRcComm())) {
						authFailed.set(true);
						try {
							conn.disconnect();
						} catch (IOException e) {
							log.debug(e.getMessage());
						}
						return;
					}

					ctx.conn = conn;
					log.debug("Connection established with iRODS server [host={}, port={}].", host, port);
				}));
			});

			// Wait for all tasks to finish.
			for (var f : futures) {
				try {
					f.get();
				} catch (InterruptedException | ExecutionException e) {
				}
			}
		} else {
			for (var i = 0; i < pool.size(); ++i) {
				var conn = new IRODSConnection(connOptions);
				try {
					conn.connect(host, port, clientUser);
				} catch (Exception e) {
					connectFailed.set(true);
					log.error(e.getMessage());
					break;
				}

				if (!authenticator.apply(conn.getRcComm())) {
					authFailed.set(true);
					try {
						conn.disconnect();
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
					break;
				}

				pool.get(i).conn = conn;
				log.debug("Connection established with iRODS server [host={}, port={}].", host, port);
			}
		}

		if (connectFailed.get()) {
			throw new IllegalStateException(String.format("Connection error [host=%s, port=%d]", host, port));
		}

		if (authFailed.get()) {
			throw new IllegalStateException(String.format("Authentication error"));
		}

		// Use one connection to capture the time of the latest resource modification.
		// This will be used to track when a connection should be refreshed. This helps
		// with long-running agents.
		var comm = pool.get(0).conn.getRcComm();
		var queryString = Versioning.compareVersions(comm.relVersion.substring(4), "4.3.4") > 0
				? "select RESC_MODIFY_TIME, RESC_MODIFY_TIME_MILLIS order by RESC_MODIFY_TIME desc, RESC_MODIFY_TIME_MILLIS desc limit 1"
				: "select no distinct RESC_MODIFY_TIME, RESC_MODIFY_TIME_MILLIS order by RESC_MODIFY_TIME desc, RESC_MODIFY_TIME_MILLIS desc limit 1";
		var latestRescMtimeRow = IRODSQuery.executeGenQuery2(comm, clientUser.getZone(), queryString).get(0);
		var latestRescMtime = String.format("%s.%s", latestRescMtimeRow.get(0), latestRescMtimeRow.get(1));
		var rescCountRow = IRODSQuery.executeGenQuery2(comm, clientUser.getZone(), "select count(RESC_ID) limit 1")
				.get(0);
		var rescCount = Integer.parseInt(rescCountRow.get(0));
		pool.forEach(ctx -> {
			ctx.latestRescMTime = latestRescMtime;
			ctx.rescCount = rescCount;
		});
	}

	private void refreshConnection(ConnectionContext ctx) {
		if (!isConnectionReadyForUse(ctx)) {
			try {
				createNewConnection(ctx);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}

	private boolean isConnectionReadyForUse(ConnectionContext ctx) {
		if (!ctx.conn.isConnected()) {
			return false;
		}

		if (poolOptions.numberOfRetrievalsBeforeConnectionRefresh.isPresent()) {
			if (ctx.retrievalCount >= poolOptions.numberOfRetrievalsBeforeConnectionRefresh.get()) {
				return false;
			}
		}

		if (poolOptions.numberOfSecondsBeforeConnectionRefresh.isPresent()) {
			var elapsed = Instant.now().getEpochSecond() - ctx.ctime;
			if (elapsed >= poolOptions.numberOfSecondsBeforeConnectionRefresh.get()) {
				return false;
			}
		}

		try {
			if (poolOptions.refreshConnectionsWhenResourceChangesDetected) {
				// Capture whether the resources are in sync, or not. It's important that each
				// resource property be updated to avoid unnecessary refreshes of the
				// connection.
				var inSync = true;

				var comm = ctx.conn.getRcComm();
				var zone = clientUser.getZone();

				var rescCountRow = IRODSQuery.executeGenQuery2(comm, zone, "select count(RESC_ID)").get(0);
				log.debug("Resource count = {}", rescCountRow.get(0));
				var rescCount = Integer.parseInt(rescCountRow.get(0));
				if (rescCount != ctx.rescCount) {
					inSync = false;
					ctx.rescCount = rescCount;
				}

				if (rescCount > 0) {
					var queryString = Versioning.compareVersions(comm.relVersion.substring(4), "4.3.4") > 0
							? "select RESC_MODIFY_TIME, RESC_MODIFY_TIME_MILLIS order by RESC_MODIFY_TIME desc, RESC_MODIFY_TIME_MILLIS desc limit 1"
							: "select no distinct RESC_MODIFY_TIME, RESC_MODIFY_TIME_MILLIS order by RESC_MODIFY_TIME desc, RESC_MODIFY_TIME_MILLIS desc limit 1";
					var latestRescMtimeRow = IRODSQuery.executeGenQuery2(comm, zone, queryString).get(0);
					var latestRescMtime = String.format("%s.%s", latestRescMtimeRow.get(0), latestRescMtimeRow.get(1));
					if (latestRescMtime.equals(ctx.latestRescMTime)) {
						inSync = false;
						ctx.latestRescMTime = latestRescMtime;
					}
				}

				// Only return false if it was determined that the connection needed to be
				// synchronized with the catalog.
				//
				// This if-block may seem unnecessary, but it serves an important role in
				// keeping this implementation future-proof. The existence of this if-block
				// means new options can be added after this option block without risk of
				// introducing bugs.
				if (!inSync) {
					return inSync;
				}
			} else {
				// Check if the connection is still valid. This query will always succeed unless
				// there's an issue in which case an exception will be thrown.
				IRODSQuery.executeGenQuery2(ctx.conn.getRcComm(), "select ZONE_NAME where ZONE_TYPE = 'local'");
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}

		return true;
	}

	private void createNewConnection(ConnectionContext ctx) throws Exception {
		ctx.conn.disconnect();

		var newConn = new IRODSConnection(connOptions);
		newConn.connect(host, port, clientUser);

		if (!authenticator.apply(newConn.getRcComm())) {
			return;
		}

		if (poolOptions.numberOfSecondsBeforeConnectionRefresh.isPresent()) {
			ctx.ctime = Instant.now().getEpochSecond();
		}

		if (poolOptions.numberOfRetrievalsBeforeConnectionRefresh.isPresent()) {
			ctx.retrievalCount = 0;
		}

		ctx.conn = newConn;
	}

}
