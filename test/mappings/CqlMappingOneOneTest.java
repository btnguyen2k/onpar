package mappings;

import com.github.ddth.mappings.MappingBo;
import com.github.ddth.mappings.cql.CqlDelegator;
import com.github.ddth.mappings.cql.CqlMappingOneOneDao;
import com.github.ddth.mappings.utils.MappingsUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CqlMappingOneOneTest {

    private CqlMappingOneOneDao mappingsDao;
    private CqlDelegator cqlDelegator;

    private final static String TABLE_DATA = "mapoo_data";
    private final static String NAMESPACE = "email";
    private final static String TABLE_STATS = "mappings_stats";
    private Map<String, Long> stats;

    private void initCqlDelegator() {
        if (cqlDelegator == null) {
            cqlDelegator = new CqlDelegator();
            cqlDelegator.setTableStats(TABLE_STATS)
                    .setKeyspace("onpar")
                    .setUsername("onpar")
                    .setPassword("onpar")
                    .setHostsAndPorts("127.0.0.1");
            cqlDelegator.init();
        }
    }

    private void destroyCqlDelegator() {
        if (cqlDelegator != null) {
            cqlDelegator.destroy();
            cqlDelegator = null;
        }
    }

    private void initMappingsDao() {
        if (mappingsDao == null) {
            mappingsDao = new CqlMappingOneOneDao();
            mappingsDao.setCqlDelegator(cqlDelegator).setTableData(TABLE_DATA);
            mappingsDao.init();
        }
    }

    private void destroyMappingsDao() {
        if (mappingsDao != null) {
            mappingsDao.destroy();
            mappingsDao = null;
        }
    }

    @Before
    public void setup() {
        initCqlDelegator();
        initMappingsDao();

        //setup data
        String CQL = "DROP TABLE IF EXISTS " + TABLE_DATA;
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));
        CQL = "CREATE TABLE IF NOT EXISTS " + TABLE_DATA + "(m_namespace VARCHAR," +
                "m_type VARCHAR,m_key VARCHAR,m_data BLOB," +
                "PRIMARY KEY(m_namespace, m_type, m_key))" +
                "WITH COMPACT STORAGE";
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));

        CQL = "DROP TABLE IF EXISTS " + TABLE_STATS;
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));
        CQL = "CREATE TABLE IF NOT EXISTS " + TABLE_STATS + "(m_mapping VARCHAR," +
                "m_namespace VARCHAR,m_key VARCHAR,m_value COUNTER," +
                "PRIMARY KEY(m_mapping, m_namespace, m_key))" +
                "WITH COMPACT STORAGE";
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));
    }

    @After
    public void teardown() {
        destroyMappingsDao();
        destroyCqlDelegator();
    }

    private void assertTotalItems(long expected) {
        Map<String, Long> stats = mappingsDao.getStats(NAMESPACE);
        if (expected > 0) {
            assertNotNull(stats.get("total-items"));
            assertEquals(expected, stats.get("total-items").longValue());
        } else {
            assertTrue(stats.get("total-items") == null ||
                    expected == stats.get("total-items").longValue());
        }
    }

    private void assertTargetsForObject(Set<String> expected, String object) {
        Set<String> targets = new HashSet<>();
        mappingsDao.getMappingsForObject(NAMESPACE, object)
                .forEach(bo -> targets.add(bo.getTarget()));
        assertEquals(expected, targets);
    }

    private void assertObjectsForTarget(Set<String> expected, String target) {
        Set<String> objects = new HashSet<>();
        mappingsDao.getMappingsForTarget(NAMESPACE, target)
                .forEach(bo -> objects.add(bo.getObject()));
        assertEquals(expected, objects);
    }

    @Test
    public void testEmpty() {
        assertTotalItems(0);
        assertTargetsForObject(Collections.EMPTY_SET, "object");
        assertObjectsForTarget(Collections.EMPTY_SET, "target");
    }

    @Test
    public void testMapUnmap() {
        String object1 = "one";
        String target1 = "1";
        String object2 = "two";
        String target2 = "2";

        {
            //map
            MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object1, target1);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertNull(daoResult.output);

            assertTotalItems(1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
        }

        {
            //unmap1
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object1, target2);
            assertEquals(MappingsUtils.DaoActionStatus.NOT_FOUND, daoResult.status);
            assertNotNull(daoResult.output);

            assertTotalItems(1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
            assertTargetsForObject(Collections.EMPTY_SET, object2);
            assertObjectsForTarget(Collections.EMPTY_SET, target2);
        }

        {
            //unmap2
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object2, target1);
            assertEquals(MappingsUtils.DaoActionStatus.NOT_FOUND, daoResult.status);
            assertNull(daoResult.output);

            assertTotalItems(1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
            assertTargetsForObject(Collections.EMPTY_SET, object2);
            assertObjectsForTarget(Collections.EMPTY_SET, target2);
        }

        {
            //unmap3
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object2, target2);
            assertEquals(MappingsUtils.DaoActionStatus.NOT_FOUND, daoResult.status);
            assertNull(daoResult.output);

            assertTotalItems(1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
            assertTargetsForObject(Collections.EMPTY_SET, object2);
            assertObjectsForTarget(Collections.EMPTY_SET, target2);
        }

        {
            //unmap4
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object1, target1);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertNotNull(daoResult.output);

            assertTotalItems(0);
            assertTargetsForObject(Collections.EMPTY_SET, object1);
            assertObjectsForTarget(Collections.EMPTY_SET, target1);
        }
    }

    @Test
    public void testMapRemap() {
        String object1 = "one";
        String target1 = "1";
        String object2 = "two";
        String target2 = "2";

        {
            //map
            MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object1, target1);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertNull(daoResult.output);

            assertTotalItems(1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
        }

        {
            //remap
            MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object1, target2);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertNotNull(daoResult.output);

            assertTotalItems(1);
            assertTargetsForObject(Collections.singleton(target2), object1);
            assertObjectsForTarget(Collections.singleton(object1), target2);
        }
    }

    @Test
    public void testMapStatsGet() {
        String object = "one";
        String target = "1";
        MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object, target);
        assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
        assertNull(daoResult.output);

        assertTotalItems(1);
        assertTargetsForObject(Collections.singleton(target), object);
        assertObjectsForTarget(Collections.singleton(object), target);
    }

    @Test
    public void testMapStatsGetDifferent() {
        for (int i = 1; i < 10; i++) {
            String object = "obj-" + i;
            String target = "target-" + i;
            MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object, target);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertNull(daoResult.output);

            assertTotalItems(i);
            assertTargetsForObject(Collections.singleton(target), object);
            assertObjectsForTarget(Collections.singleton(object), target);
        }
    }

    @Test
    public void testMapStatsGetSameObjTargetMultipleTimes() {
        String object = "object";
        String target = "target";
        MappingsUtils.DaoResult daoResult;

        daoResult = mappingsDao.map(NAMESPACE, object, target);
        assertEquals(daoResult.status, MappingsUtils.DaoActionStatus.SUCCESSFUL);
        assertNull(daoResult.output);

        assertTotalItems(1);
        assertTargetsForObject(Collections.singleton(target), object);
        assertObjectsForTarget(Collections.singleton(object), target);

        for (int i = 1; i < 10; i++) {
            daoResult = mappingsDao.map(NAMESPACE, object, target);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertTrue(daoResult.output instanceof MappingBo);
            assertEquals(((MappingBo) daoResult.output).getNamespace(), NAMESPACE);
            assertEquals(((MappingBo) daoResult.output).getObject(), object);
            assertEquals(((MappingBo) daoResult.output).getTarget(), target);

            assertTotalItems(1);
            assertTargetsForObject(Collections.singleton(target), object);
            assertObjectsForTarget(Collections.singleton(object), target);
        }
    }
}
