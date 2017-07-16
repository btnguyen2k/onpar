package mappings;

import com.github.ddth.mappings.IMappingDao;
import com.github.ddth.mappings.MappingBo;
import com.github.ddth.mappings.cql.CqlMappingManyManyDao;
import com.github.ddth.mappings.utils.MappingsUtils;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * Created by thanhnb on 7/16/17.
 */
public abstract class BaseMappingManyManyTest extends TestCase {

    public BaseMappingManyManyTest(String testName) {
        super(testName);
    }

    protected abstract IMappingDao initDaoInstance();

    protected abstract void destroyDaoInstance(IMappingDao mappingsDao);

    @Before
    public void setUp() throws Exception {
        mappingsDao = initDaoInstance();
    }

    @After
    public void tearDown() {
        if (mappingsDao != null) {
            destroyDaoInstance(mappingsDao);
        }
    }

    protected final static String NAMESPACE = "enrollment";
    protected IMappingDao mappingsDao;

    private void assertTotalItems(long expectedObjs, long expectedTargets) {
        Map<String, Long> stats = mappingsDao.getStats(NAMESPACE);
        if (expectedObjs > 0) {
            assertNotNull(stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_OBJS));
            assertEquals(expectedObjs,
                    stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_OBJS).longValue());
        } else {
            assertTrue(stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_OBJS) == null ||
                    0 == stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_OBJS).longValue());
        }
        if (expectedTargets > 0) {
            assertNotNull(stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_TARGETS));
            assertEquals(expectedTargets,
                    stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_TARGETS).longValue());
        } else {
            assertTrue(stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_TARGETS) == null ||
                    0 == stats.get(CqlMappingManyManyDao.STATS_KEY_TOTAL_TARGETS).longValue());
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
        assertTotalItems(0, 0);
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
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(0, existing.size());

            assertTotalItems(1, 1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
        }

        {
            //unmap1
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object1, target2);
            assertEquals(MappingsUtils.DaoActionStatus.NOT_FOUND, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(1, existing.size());

            assertTotalItems(1, 1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
            assertTargetsForObject(Collections.EMPTY_SET, object2);
            assertObjectsForTarget(Collections.EMPTY_SET, target2);
        }

        {
            //unmap2
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object2, target1);
            assertEquals(MappingsUtils.DaoActionStatus.NOT_FOUND, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(0, existing.size());

            assertTotalItems(1, 1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
            assertTargetsForObject(Collections.EMPTY_SET, object2);
            assertObjectsForTarget(Collections.EMPTY_SET, target2);
        }

        {
            //unmap3
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object2, target2);
            assertEquals(MappingsUtils.DaoActionStatus.NOT_FOUND, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(0, existing.size());

            assertTotalItems(1, 1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
            assertTargetsForObject(Collections.EMPTY_SET, object2);
            assertObjectsForTarget(Collections.EMPTY_SET, target2);
        }

        {
            //unmap4
            MappingsUtils.DaoResult daoResult = mappingsDao.unmap(NAMESPACE, object1, target1);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(1, existing.size());

            assertTotalItems(0, 0);
            assertTargetsForObject(Collections.EMPTY_SET, object1);
            assertObjectsForTarget(Collections.EMPTY_SET, target1);
        }
    }

    @Test
    public void testMapMulti() {
        String object1 = "one";
        String target1 = "1";
        String object2 = "two";
        String target2 = "2";

        {
            //map
            MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object1, target1);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(0, existing.size());

            assertTotalItems(1, 1);
            assertTargetsForObject(Collections.singleton(target1), object1);
            assertObjectsForTarget(Collections.singleton(object1), target1);
        }

        {
            //remap
            MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object1, target2);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(1, existing.size());

            assertTotalItems(1, 2);
            assertTargetsForObject(new HashSet<>(Arrays.asList(target1, target2)), object1);
            assertObjectsForTarget(Collections.singleton(object1), target2);
        }
    }

    @Test
    public void testMapStatsGet() {
        String object = "one";
        String target = "1";
        MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object, target);
        assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
        assertTrue(daoResult.output instanceof Collection);
        Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
        assertEquals(0, existing.size());

        assertTotalItems(1, 1);
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
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(0, existing.size());

            assertTotalItems(i, i);
            assertTargetsForObject(Collections.singleton(target), object);
            assertObjectsForTarget(Collections.singleton(object), target);
        }
    }

    @Test
    public void testMapStatsGetMultiple() {
        Set<String> targets = new HashSet<>();
        for (int i = 1; i < 10; i++) {
            String object = "obj";
            String target = "target-" + i;
            MappingsUtils.DaoResult daoResult = mappingsDao.map(NAMESPACE, object, target);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(i - 1, existing.size());

            assertTotalItems(1, i);
            targets.add(target);
            assertTargetsForObject(targets, object);
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
        assertTrue(daoResult.output instanceof Collection);
        Collection<MappingBo> existing = (Collection<MappingBo>) daoResult.output;
        assertEquals(0, existing.size());

        assertTotalItems(1, 1);
        assertTargetsForObject(Collections.singleton(target), object);
        assertObjectsForTarget(Collections.singleton(object), target);

        for (int i = 1; i < 10; i++) {
            daoResult = mappingsDao.map(NAMESPACE, object, target);
            assertEquals(MappingsUtils.DaoActionStatus.SUCCESSFUL, daoResult.status);
            assertTrue(daoResult.output instanceof Collection);
            existing = (Collection<MappingBo>) daoResult.output;
            assertEquals(1, existing.size());

            MappingBo bo = existing.iterator().next();
            assertEquals(bo.getNamespace(), NAMESPACE);
            assertEquals(bo.getObject(), object);
            assertEquals(bo.getTarget(), target);

            assertTotalItems(1, 1);
            assertTargetsForObject(Collections.singleton(target), object);
            assertObjectsForTarget(Collections.singleton(object), target);
        }
    }
}
