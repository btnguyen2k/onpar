package mappings;

import com.github.ddth.mappings.IMappingDao;
import com.github.ddth.mappings.cql.CqlDelegator;
import com.github.ddth.mappings.cql.CqlMappingManyManyDao;
import com.github.ddth.mappings.cql.CqlMappingOneOneDao;
import junit.framework.Test;
import junit.framework.TestSuite;

public class CqlMappingManyManyTest extends BaseMappingManyManyTest {

    public CqlMappingManyManyTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(CqlMappingManyManyTest.class);
    }

    protected final static String TABLE_DATA = "mapmm_data";
    protected final static String TABLE_STATS = "mappings_stats";

    protected IMappingDao initDaoInstance() {
        CqlDelegator cqlDelegator = new CqlDelegator();
        cqlDelegator.setTableStats(TABLE_STATS)
                .setKeyspace("onpar")
                .setUsername("onpar")
                .setPassword("onpar")
                .setHostsAndPorts("127.0.0.1");
        cqlDelegator.init();

        //setup data
        String CQL = "DROP TABLE IF EXISTS " + TABLE_DATA;
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));
        CQL = "CREATE TABLE IF NOT EXISTS " + TABLE_DATA + "(m_namespace VARCHAR," +
                "m_type VARCHAR,m_key VARCHAR,m_value VARCHAR,m_data BLOB," +
                "PRIMARY KEY(m_namespace,m_type,m_key,m_value))" +
                "WITH COMPACT STORAGE";
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));

        CQL = "DROP TABLE IF EXISTS " + TABLE_STATS;
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));
        CQL = "CREATE TABLE IF NOT EXISTS " + TABLE_STATS + "(m_mapping VARCHAR," +
                "m_namespace VARCHAR,m_key VARCHAR,m_value COUNTER," +
                "PRIMARY KEY(m_mapping,m_namespace,m_key))" +
                "WITH COMPACT STORAGE";
        cqlDelegator.update(cqlDelegator.prepareStatement(CQL));

        CqlMappingManyManyDao mappingsDao = new CqlMappingManyManyDao();
        mappingsDao.setCqlDelegator(cqlDelegator).setTableData(TABLE_DATA);
        mappingsDao.init();

        return mappingsDao;
    }

    protected void destroyDaoInstance(IMappingDao mappingsDao) {
        if (mappingsDao instanceof CqlMappingOneOneDao) {
            CqlMappingOneOneDao cqlMappingsDao = (CqlMappingOneOneDao) mappingsDao;
            cqlMappingsDao.getCqlDelegator().destroy();
            cqlMappingsDao.destroy();
        }
    }

}
