package com.github.ddth.mappings.cql;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.github.ddth.cql.CqlUtils;
import com.github.ddth.mappings.AbstractMappingManyManyDao;
import com.github.ddth.mappings.MappingBo;
import com.github.ddth.mappings.utils.MappingsUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.*;

/**
 * CQL-implementation of n-n mapping.
 *
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class CqlMappingManyManyDao extends AbstractMappingManyManyDao {

    private CqlDelegator cqlDelegator;

    private String tableData = "mapmm_data";

    public CqlDelegator getCqlDelegator() {
        return cqlDelegator;
    }

    public CqlMappingManyManyDao setCqlDelegator(CqlDelegator cqlDelegator) {
        this.cqlDelegator = cqlDelegator;
        return this;
    }

    public String getTableData() {
        return tableData;
    }

    public CqlMappingManyManyDao setTableData(String tableData) {
        this.tableData = tableData;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CqlMappingManyManyDao init() {
        super.init();

        pstmDeleteDataSingle = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_DELETE_DATA_SINGLE, tableData));
        pstmDeleteDataSingleIfExists = cqlDelegator
                .prepareStatement(
                        MessageFormat.format(CQL_DELETE_DATA_SINGLE_IF_EXISTS, tableData));

        pstmInsertData = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_INSERT_DATA, tableData));
        pstmInsertDataIfNotExists = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_INSERT_DATA_IF_NOT_EXISTS, tableData));

        pstmSeleteDataSingle = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_SELECT_DATA_SINGLE, tableData));
        pstmSeleteDataMultiple = cqlDelegator
                .prepareStatement(MessageFormat.format(CQL_SELECT_DATA_MULTIPLE, tableData));

        return this;
    }

    private PreparedStatement pstmDeleteDataSingle, pstmDeleteDataSingleIfExists;
    private PreparedStatement pstmInsertData, pstmInsertDataIfNotExists;
    private PreparedStatement pstmSeleteDataSingle, pstmSeleteDataMultiple;

    private final static String COL_NAMESPACE = "m_namespace";
    private final static String COL_TYPE = "m_type";
    private final static String COL_KEY = "m_key";
    private final static String COL_VALUE = "m_value";
    private final static String COL_DATA = "m_data";
    private final static String[] _COL_ALL = {COL_NAMESPACE, COL_TYPE, COL_KEY, COL_VALUE,
            COL_DATA};
    private final static String[] _WHERE_DATA_SINGLE = {COL_NAMESPACE + "=?", COL_TYPE + "=?",
            COL_KEY + "=?", COL_VALUE + "=?"};
    private final static String[] _WHERE_DATA_MULTIPLE = {COL_NAMESPACE + "=?", COL_TYPE + "=?",
            COL_KEY + "=?"};

    private final static String CQL_DELETE_DATA_SINGLE = "DELETE FROM {0} WHERE " + StringUtils.join
            (_WHERE_DATA_SINGLE, " AND ");
    private final static String CQL_DELETE_DATA_SINGLE_IF_EXISTS = "DELETE FROM {0} WHERE " +
            StringUtils.join(_WHERE_DATA_SINGLE, " AND ") + " IF EXISTS";

    private final static String CQL_INSERT_DATA =
            "INSERT INTO {0} (" + StringUtils.join(_COL_ALL, ",") + ") VALUES (" +
                    StringUtils.repeat("?", ",", _COL_ALL.length) + ")";
    private final static String CQL_INSERT_DATA_IF_NOT_EXISTS =
            "INSERT INTO {0} (" + StringUtils.join(_COL_ALL, ",") + ") VALUES (" +
                    StringUtils.repeat("?", ",", _COL_ALL.length) + ") IF NOT EXISTS";
    private final static String CQL_SELECT_DATA_SINGLE = "SELECT " + StringUtils.join(_COL_ALL, ",")
            + " FROM {0} WHERE " + StringUtils.join(_WHERE_DATA_SINGLE, " AND ");
    private final static String CQL_SELECT_DATA_MULTIPLE = "SELECT " + StringUtils.join(_COL_ALL,
            ",") + " FROM {0} WHERE " + StringUtils.join(_WHERE_DATA_MULTIPLE, " AND ");


    public final static String DATA_TYPE_OBJ_TARGET = "obj:target";
    public final static String DATA_TYPE_TARGET_OBJ = "target:obj";
    public final static String STATS_MAPPING = "mappings-mm";
    public final static String STATS_KEY_TOTAL_OBJS = "total-objs";
    public final static String STATS_KEY_TOTAL_TARGETS = "total-targets";

    private MappingBo newMappingBo(Row row) {
        if (row != null) {
            String namespace = row.getString(COL_NAMESPACE);
            String type = row.getString(COL_TYPE);
            String[] tokens = MappingsUtils.seDecode(row.getBytes(COL_DATA));
            if (StringUtils.equalsIgnoreCase(type, DATA_TYPE_OBJ_TARGET)) {
                String obj = row.getString(COL_KEY);
                String target = row.getString(COL_VALUE);
                long timestamp = Long.parseLong(tokens[0]);
                return MappingBo.newInstance(namespace, obj, target, timestamp);
            } else if (StringUtils.equalsIgnoreCase(type, DATA_TYPE_TARGET_OBJ)) {
                String target = row.getString(COL_KEY);
                String obj = row.getString(COL_VALUE);
                long timestamp = Long.parseLong(tokens[0]);
                return MappingBo.newInstance(namespace, obj, target, timestamp);
            }
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
    protected Collection<MappingBo> storageGetMappingsObjTargets(String namespace, String obj) {
        Collection<MappingBo> result = new HashSet<>();
        ResultSet rs = cqlDelegator
                .select(pstmSeleteDataMultiple, namespace, DATA_TYPE_OBJ_TARGET, obj);
        rs.forEach(row -> result.add(newMappingBo(row)));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<MappingBo> storageGetMappingsTargetObjs(String namespace, String target) {
        Collection<MappingBo> result = new HashSet<>();
        ResultSet rs = cqlDelegator
                .select(pstmSeleteDataMultiple, namespace, DATA_TYPE_TARGET_OBJ, target);
        rs.forEach(row -> result.add(newMappingBo(row)));
        return result;
    }

    /**
     * Save mapping {@code object <-> target} to storage.
     *
     * <ul>
     * <li>Logged batch operation: store mappings {@code object -> target} & {@code target ->
     * object}.</li>
     * <li>If {@code existingOT} is empty, increase stats count {@link #STATS_KEY_TOTAL_OBJS}</li>
     * <li>If {@code existingTO} is empty, increase stats count
     * {@link #STATS_KEY_TOTAL_TARGETS}</li>
     * </ul>
     *
     * @param mappingToAdd
     * @param existingOT
     * @param existingTO
     * @return
     */
    @Override
    protected MappingsUtils.DaoResult storageMap(MappingBo mappingToAdd,
            Collection<MappingBo> existingOT, Collection<MappingBo> existingTO) {
        long now = System.currentTimeMillis();
        ByteBuffer data = MappingsUtils.seEncodeAsByteBuffer(String.valueOf(now));
        final String namespace = mappingToAdd.getNamespace();
        final String obj = mappingToAdd.getObject();
        final String target = mappingToAdd.getTarget();

        List<Statement> stmList = new ArrayList<>();
        stmList.add(CqlUtils.bindValues(pstmInsertData, namespace, DATA_TYPE_OBJ_TARGET, obj,
                target, data));
        stmList.add(CqlUtils.bindValues(pstmInsertData, namespace, DATA_TYPE_TARGET_OBJ, target,
                obj, data));
        ResultSet rs = cqlDelegator.executeBatch(stmList.toArray(new Statement[0]));
        if (rs.wasApplied()) {
            if (existingOT == null || existingOT.size() == 0) {
                storageUpdateStats(namespace, STATS_KEY_TOTAL_OBJS, 1);
            }
            if (existingTO == null || existingTO.size() == 0) {
                storageUpdateStats(namespace, STATS_KEY_TOTAL_TARGETS, 1);
            }
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.SUCCESSFUL,
                    existingOT);
        } else {
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.ERROR, existingOT);
        }
    }

    /**
     * Remove mapping {@code object <-> target} from storage.
     *
     * <ul>
     * <li>Logged batch operation: remove mappings {@code object -> target} & {@code
     * target -> object}.</li>
     * <li>Decrease stats count {@link #STATS_KEY_TOTAL_OBJS} if object was the last one.</li>
     * <li>Decrease stats count {@link #STATS_KEY_TOTAL_TARGETS} if target was the last one.</li>
     * </ul>
     *
     * @param mappingToRemove
     * @param existingOT
     * @param existingTO
     * @return
     */
    @Override
    protected MappingsUtils.DaoResult storageUnmap(MappingBo mappingToRemove,
            Collection<MappingBo> existingOT, Collection<MappingBo> existingTO) {
        final String namespace = mappingToRemove.getNamespace();
        final String obj = mappingToRemove.getObject();
        final String target = mappingToRemove.getTarget();
        Statement stmDeleteObjTarget = CqlUtils.bindValues(pstmDeleteDataSingle, namespace,
                DATA_TYPE_OBJ_TARGET, obj, target);
        Statement stmDeleteTargetObj = CqlUtils.bindValues(pstmDeleteDataSingle, namespace,
                DATA_TYPE_TARGET_OBJ, target, obj);
        ResultSet rs = cqlDelegator.executeBatch(stmDeleteObjTarget, stmDeleteTargetObj);
        if (rs.wasApplied()) {
            if (existingOT == null || existingOT.size() == 0 ||
                    existingOT.contains(mappingToRemove)) {
                storageUpdateStats(namespace, STATS_KEY_TOTAL_OBJS, -1);
            }
            if (existingTO == null || existingTO.size() == 0 ||
                    existingTO.contains(mappingToRemove)) {
                storageUpdateStats(namespace, STATS_KEY_TOTAL_TARGETS, -1);
            }
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.SUCCESSFUL,
                    existingOT);
        } else {
            return new MappingsUtils.DaoResult(MappingsUtils.DaoActionStatus.NOT_FOUND, existingOT);
        }
    }

    private void storageUpdateStats(String namespace, String key, long value) {
        cqlDelegator.updateStats(STATS_MAPPING, namespace, key, value);
    }
}
