package com.github.ddth.mappings.cql;

import com.datastax.driver.core.*;
import com.github.ddth.cql.CqlUtils;
import com.github.ddth.cql.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class CqlDelegator {

    private final Logger LOGGER = LoggerFactory.getLogger(CqlDelegator.class);

    private SessionManager sessionManager;
    private boolean myOwnSessionManager = false;
    private String keyspace, username, password;
    private String hostsAndPorts;
    private ConsistencyLevel consistencyLevelRead = ConsistencyLevel.LOCAL_ONE;
    private ConsistencyLevel consistencyLevelWrite = ConsistencyLevel.LOCAL_ONE;

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public CqlDelegator setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public CqlDelegator setKeyspace(String keyspace) {
        this.keyspace = keyspace;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public CqlDelegator setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public CqlDelegator setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getHostsAndPorts() {
        return hostsAndPorts;
    }

    public CqlDelegator setHostsAndPorts(String hostsAndPorts) {
        this.hostsAndPorts = hostsAndPorts;
        return this;
    }

    public ConsistencyLevel getConsistencyLevelRead() {
        return consistencyLevelRead;
    }

    public CqlDelegator setConsistencyLevelRead(ConsistencyLevel consistencyLevelRead) {
        this.consistencyLevelRead = consistencyLevelRead;
        return this;
    }

    public ConsistencyLevel getConsistencyLevelWrite() {
        return consistencyLevelWrite;
    }

    public void setConsistencyLevelWrite(ConsistencyLevel consistencyLevelWrite) {
        this.consistencyLevelWrite = consistencyLevelWrite;
    }

    public CqlDelegator init() {
        if (sessionManager == null) {
            sessionManager = new SessionManager();
            sessionManager.init();
            myOwnSessionManager = true;
        }

        return this;
    }

    public void destroy() {
        if (sessionManager != null && myOwnSessionManager) {
            try {
                sessionManager.destroy();
            } catch (Exception e) {
                LOGGER.warn(e.getMessage(), e);
            } finally {
                sessionManager = null;
            }
        }
    }

    /**
     * Obtain a {@link Session} instance.
     *
     * @return
     */
    public Session getSession() {
        return sessionManager.getSession(hostsAndPorts, username, password, keyspace);
    }

    /**
     * Prepare a CQL statement.
     *
     * @param cql
     * @return
     */
    public PreparedStatement prepareStatement(String cql) {
        return CqlUtils.prepareStatement(getSession(), cql);
    }

    /**
     * Fetch one row.
     *
     * @param stm
     * @param params
     * @return
     */
    public Row seletOneRow(PreparedStatement stm, Object... params) {
        return CqlUtils.executeOne(getSession(), stm, getConsistencyLevelRead(), params);
    }

    /**
     * Execute update statement.
     *
     * @param pstm
     * @param params
     * @return
     */
    public ResultSet update(PreparedStatement pstm, Object... params) {
        return CqlUtils.execute(getSession(), pstm, getConsistencyLevelWrite(), params);
    }

    /**
     * Execute a batch of statements.
     *
     * @param stms
     * @return
     */
    public ResultSet executeBatch(Statement... stms) {
        return CqlUtils.executeBatch(getSession(), getConsistencyLevelWrite(),
                BatchStatement.Type.LOGGED, stms);
    }

}
