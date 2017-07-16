package com.github.ddth.mappings.cql;

import com.datastax.driver.core.*;
import com.github.ddth.cql.CqlUtils;
import com.github.ddth.cql.SessionManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class CqlDelegator {

    private final Logger LOGGER = LoggerFactory.getLogger(CqlDelegator.class);

    private String tableStats = "mappings_stats";

    private SessionManager sessionManager;
    private boolean myOwnSessionManager = false;
    private String keyspace, username, password;
    private String hostsAndPorts;
    private ConsistencyLevel consistencyLevelRead = ConsistencyLevel.LOCAL_ONE;
    private ConsistencyLevel consistencyLevelWrite = ConsistencyLevel.LOCAL_ONE;

    public String getTableStats() {
        return tableStats;
    }

    public CqlDelegator setTableStats(String tableStats) {
        this.tableStats = tableStats;
        return this;
    }

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

    private final static String COL_MAPPING = "m_mapping";
    private final static String COL_NAMESPACE = "m_namespace";
    private final static String COL_KEY = "m_key";
    private final static String COL_VALUE = "m_value";
    private final static String[] _COLS_ALL = {COL_MAPPING, COL_NAMESPACE,
            COL_KEY, COL_VALUE};
    private final static String[] _WHERE_STATS = {COL_MAPPING + "=?", COL_NAMESPACE +
            "=?", COL_KEY + "=?"};
    private final static String CQL_UPDATE_STATS =
            "UPDATE {0} SET " + COL_VALUE + "=" + COL_VALUE + "+? WHERE " +
                    StringUtils.join(_WHERE_STATS, " AND ");
    private final static String CQL_SELECT_ALL_STATS =
            "SELECT " + StringUtils.join(_COLS_ALL, ",") +
                    " FROM {0} WHERE " + COL_MAPPING + "=? AND " + COL_NAMESPACE + "=?";
    private PreparedStatement pstmUpdateStats, pstmGetAllStats;

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
        return prepareStatement(getSession(), cql);
    }

    /**
     * Prepare a CQL statement.
     *
     * @param session
     * @param cql
     * @return
     */
    public PreparedStatement prepareStatement(Session session, String cql) {
        return CqlUtils.prepareStatement(session, cql);
    }

    /**
     * Fetch one row.
     *
     * @param pstm
     * @param params
     * @return
     */
    public Row selectOneRow(PreparedStatement pstm, Object... params) {
        return selectOneRow(getSession(), pstm, params);
    }

    /**
     * Fetch one row.
     *
     * @param session
     * @param pstm
     * @param params
     * @return
     */
    public Row selectOneRow(Session session, PreparedStatement pstm, Object... params) {
        return CqlUtils.executeOne(session, pstm, getConsistencyLevelRead(), params);
    }

    /**
     * Fetch rows.
     *
     * @param pstm
     * @param params
     * @return
     */
    public ResultSet select(PreparedStatement pstm, Object... params) {
        return select(getSession(), pstm, params);
    }

    /**
     * Fetch rows.
     *
     * @param session
     * @param pstm
     * @param params
     * @return
     */
    public ResultSet select(Session session, PreparedStatement pstm, Object... params) {
        return CqlUtils.execute(session, pstm, getConsistencyLevelRead(), params);
    }

    /**
     * Execute update statement.
     *
     * @param pstm
     * @param params
     * @return
     */
    public ResultSet update(PreparedStatement pstm, Object... params) {
        return update(getSession(), pstm, params);
    }

    /**
     * Execute update statement.
     *
     * @param session
     * @param pstm
     * @param params
     * @return
     */
    public ResultSet update(Session session, PreparedStatement pstm, Object... params) {
        return CqlUtils.execute(session, pstm, getConsistencyLevelWrite(), params);
    }

    /**
     * Execute a batch of statements.
     *
     * @param stms
     * @return
     */
    public ResultSet executeBatch(Statement... stms) {
        return executeBatch(getSession(), stms);
    }

    public ResultSet executeBatch(Session session, Statement... stms) {
        return CqlUtils.executeBatch(session, getConsistencyLevelWrite(),
                BatchStatement.Type.LOGGED, stms);
    }

    /**
     * Update mapping stats.
     *
     * @param mapping
     * @param namespace
     * @param key
     * @param value
     * @return
     */
    public ResultSet updateStats(String mapping, String namespace, String key, long value) {
        Session session = getSession();
        if (pstmUpdateStats == null) {
            pstmUpdateStats = prepareStatement(session, MessageFormat.format(CQL_UPDATE_STATS,
                    tableStats));
        }
        return update(session, pstmUpdateStats, value, mapping, namespace, key);
    }

    /**
     * Get all stats of a mapping namespace.
     *
     * @param mapping
     * @param namespace
     * @return
     */
    public Map<String, Long> getAllStats(String mapping, String namespace) {
        Session session = getSession();
        if (pstmGetAllStats == null) {
            pstmGetAllStats = prepareStatement(session, MessageFormat.format(CQL_SELECT_ALL_STATS,
                    tableStats));
        }
        Map<String, Long> result = new HashMap<>();
        ResultSet rs = select(session, pstmGetAllStats, mapping, namespace);
        rs.forEach(row -> result.put(row.getString(COL_KEY), row.getLong(COL_VALUE)));
        return result;
    }
}
