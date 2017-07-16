package com.github.ddth.mappings.cql;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.github.ddth.cql.CqlUtils;
import com.github.ddth.mappings.AbstractMappingOneOneDao;
import com.github.ddth.mappings.MappingBo;
import com.github.ddth.mappings.utils.MappingsUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CQL-implementation of 1-1 mapping.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class CqlMappingOneOneDao extends AbstractMappingOneOneDao {

    private CqlDelegator cqlDelegator;

    private String tableData = "mapoo";

    public CqlDelegator getCqlDelegator() {
        return cqlDelegator;
    }

    public CqlMappingOneOneDao setCqlDelegator(CqlDelegator cqlDelegator) {
        this.cqlDelegator = cqlDelegator;
        return this;
    }

    public String getTableData() {
        return tableData;
    }

    public CqlMappingOneOneDao setTableData(String tableData) {
        this.tableData = tableData;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CqlMappingOneOneDao init() {
        super.init();


        pstmDeleteData = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_DELETE_DATA, tableData));
        pstmDeleteDataIfExists = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_DELETE_DATA_IF_EXISTS, tableData));

        pstmInsertData = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_INSERT_DATA, tableData));
        pstmInsertDataIfNotExists = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_INSERT_DATA_IF_NOT_EXISTS, tableData));

        pstmSeleteData = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_SELECT_DATA, tableData));

        return this;
    }

    private PreparedStatement pstmDeleteData, pstmDeleteDataIfExists;
    private PreparedStatement pstmInsertData, pstmInsertDataIfNotExists;
    private PreparedStatement pstmSeleteData;

    private final static String COL_DATA_NAMESPACE = "m_namespace";
    private final static String COL_DATA_TYPE = "m_type";
    private final static String COL_DATA_KEY = "m_key";
    private final static String COL_DATA_DATA = "m_data";
    private final static String[] _COL_DATA_ALL = {COL_DATA_NAMESPACE, COL_DATA_TYPE, COL_DATA_KEY,
            COL_DATA_DATA};
    private final static String[] _WHERE_DATA = {COL_DATA_NAMESPACE + "=?", COL_DATA_TYPE + "=?",
            COL_DATA_KEY + "=?"};

    private final static String CQL_DELETE_DATA = "DELETE FROM {0} WHERE " + StringUtils.join
            (_WHERE_DATA, " AND ");
    private final static String CQL_DELETE_DATA_IF_EXISTS = "DELETE FROM {0} WHERE " + StringUtils
            .join(_WHERE_DATA, " AND ") + " IF EXISTS";
    private final static String CQL_INSERT_DATA =
            "INSERT INTO {0} (" + StringUtils.join(_COL_DATA_ALL, ",")
                    + ") VALUES (" + StringUtils.repeat("?", ",", _COL_DATA_ALL.length) + ")";
    private final static String CQL_INSERT_DATA_IF_NOT_EXISTS =
            "INSERT INTO {0} (" + StringUtils.join(_COL_DATA_ALL, ",")
                    + ") VALUES (" + StringUtils.repeat("?", ",", _COL_DATA_ALL.length) +
                    ") IF NOT EXISTS";
    private final static String CQL_SELECT_DATA = "SELECT * FROM {0} WHERE " + StringUtils.join
            (_WHERE_DATA, " AND ");


    public final static String DATA_TYPE_OBJ_TARGET = "obj:target";
    public final static String DATA_TYPE_TARGET_OBJ = "target:obj";
    public final static String STATS_MAPPING = "mappings-oo";
    public final static String STATS_KEY_TOTAL_ITEMS = "total-items";

    private MappingBo newMappingBo(Row row) {
        if (row == null) {
            return null;
        }
        String namespace = row.getString(COL_DATA_NAMESPACE);
        String type = row.getString(COL_DATA_TYPE);
        String[] tokens = MappingsUtils.seDecode(row.getBytes(COL_DATA_DATA));
        if (StringUtils.equalsIgnoreCase(type, DATA_TYPE_OBJ_TARGET)) {
            String obj = row.getString(COL_DATA_KEY);
            String target = tokens[0];
            long timestamp = Long.parseLong(tokens[1]);
            return MappingBo.newInstance(namespace, obj, target, timestamp);
        } else if (StringUtils.equalsIgnoreCase(type, DATA_TYPE_TARGET_OBJ)) {
            String target = row.getString(COL_DATA_KEY);
            String obj = tokens[0];
            long timestamp = Long.parseLong(tokens[1]);
            return MappingBo.newInstance(namespace, obj, target, timestamp);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Long> getStats(String namespace) {
        return cqlDelegator.getAllStats(STATS_MAPPING, namespace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MappingBo storageGetMappingObjTarget(String namespace, String obj) {
        Row row = cqlDelegator.selectOneRow(pstmSeleteData, namespace, DATA_TYPE_OBJ_TARGET, obj);
        return newMappingBo(row);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MappingBo storageGetMappingTargetObj(String namespace, String target) {
        Row row = cqlDelegator
                .selectOneRow(pstmSeleteData, namespace, DATA_TYPE_TARGET_OBJ, target);
        return newMappingBo(row);
    }

    /**
     * {@inheritDoc}
     *
     * <ul>
     * <li>Remove existing {@code old-target <- object}, if any.</li>
     * <li>Store mapping {@code object -> target}, will override any existing {@code object ->
     * old-target}.</li>
     * <li>Store mapping {@code target <- object}.</li>
     * <li>All operations above are within a logged batch.</li>
     * <li>Finally, update stats if needed.</li>
     * </ul>
     */
    @Override
    protected MappingsUtils.DaoResult storageMap(String namespace, String obj, String target,
            MappingBo existingOT, MappingBo existingTO) {
        long now = System.currentTimeMillis();
        ByteBuffer targetTime = MappingsUtils.seEncodeAsByteBuffer(target, String.valueOf(now));
        ByteBuffer objTime = MappingsUtils.seEncodeAsByteBuffer(obj, String.valueOf(now));

        List<Statement> stmList = new ArrayList<>();
        if (existingTO != null) {
            stmList.add(CqlUtils.bindValues(pstmDeleteData, namespace, DATA_TYPE_TARGET_OBJ,
                    existingTO.getTarget()));
        }
        stmList.add(CqlUtils.bindValues(pstmInsertData, namespace, DATA_TYPE_OBJ_TARGET, obj,
                targetTime));
        stmList.add(CqlUtils.bindValues(pstmInsertData, namespace, DATA_TYPE_TARGET_OBJ, target,
                objTime));
        ResultSet rs = cqlDelegator.executeBatch(stmList.toArray(new Statement[0]));
        if (rs.wasApplied()) {
            if (existingOT == null) {
                storageUpdateStats(namespace, STATS_KEY_TOTAL_ITEMS, 1);
            }
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.SUCCESSFUL);
        } else {
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.ERROR);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <ul>
     * <li>Remove mapping {@code object -> target}.</li>
     * <li>Remove mapping {@code target <- object}.</li>
     * <li>All operations above are within a logged batch.</li>
     * <li>Finally, update stats if needed.</li>
     * </ul>
     */
    @Override
    protected MappingsUtils.DaoResult storageUnmap(String namespace, String obj, String target) {
        Statement stmDeleteObjTarget = CqlUtils.bindValues(pstmDeleteDataIfExists, namespace,
                DATA_TYPE_OBJ_TARGET, obj);
        Statement stmDeleteTargetObj = CqlUtils.bindValues(pstmDeleteDataIfExists, namespace,
                DATA_TYPE_TARGET_OBJ, target);
        ResultSet rs = cqlDelegator.executeBatch(stmDeleteObjTarget, stmDeleteTargetObj);
        if (rs.wasApplied()) {
            storageUpdateStats(namespace, STATS_KEY_TOTAL_ITEMS, -1);
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.SUCCESSFUL);
        } else {
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.NOT_FOUND);
        }
    }

    private void storageUpdateStats(String namespace, String key, long value) {
        cqlDelegator.updateStats(STATS_MAPPING, namespace, key, value);
    }
}
