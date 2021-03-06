package akka.cluster.workers;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import com.github.ddth.commons.utils.DateFormatUtils;

import akka.TickMessage;
import akka.workers.CronFormat;
import play.Logger;

/**
 * Sample cluster worker that runs only on nodes with roles contain "Role1".
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since template-v2.6.r1
 */
public class SampleOnlyRole1ClusterWorker extends BaseClusterWorker {

    /**
     * Schedule to do job every 7 seconds
     */
    private CronFormat scheduling = CronFormat.parse("*/7 * *");

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean runFirstTimeRegardlessScheduling() {
        return true;
    }

    @Override
    protected CronFormat getScheduling() {
        return scheduling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getDeployRoles() {
        return Collections.singleton("Role1");
    }

    @Override
    protected void doJob(TickMessage tick) {
        Date d = new Date(tick.timestampMs);
        Logger.info("[" + DateFormatUtils.toString(d, "HH:mm:ss") + "] " + getActorPath()
                + " do job " + tick + " from " + sender().path());
    }

}
