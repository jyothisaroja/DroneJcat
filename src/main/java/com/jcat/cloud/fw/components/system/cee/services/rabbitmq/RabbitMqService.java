package com.jcat.cloud.fw.components.system.cee.services.rabbitmq;

import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;
import org.apache.log4j.Logger;

import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.services.crm.GenericService;

/**
 * This class contains available functionality concerning
 *  Rabbit MQ service that is within the CIC.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo 2017-02-08 initial version
 *
 */
public class RabbitMqService extends GenericService {

    private static final String RABBIT_CLUSTER_CMD = "rabbitmqctl cluster_status";
    private final Logger mLogger = Logger.getLogger(RabbitMqService.class);

    public RabbitMqService(EcsSession sshSession, EcsCicList ecsCicList) {
        super(sshSession, ecsCicList);
    }

    /**
     * Checks if Rabbit MQ cluster is up
     *
     * @return boolean
     */
    public boolean isRabbitClusterUp() {
        String output = mSshSession.send(RABBIT_CLUSTER_CMD);
        if (mNumberOfCics == 1) {
            return !output.contains("Error") && !output.contains("not running");
        }
        return (output.contains("cic-1") && output.contains("cic-2") && output.contains("cic-3"));
    }

    /**
     * Verifies that RabbitMQ service is up and running
     */
    public void verifyRabbitMqIsUp() {
        if (!isRabbitClusterUp()) {
            waitForRabbitClusterToBeUp();
        }
    }

    /**
     * Waits for RabbitMQ service to be up and running
     */
    public void waitForRabbitClusterToBeUp() {
        mLogger.info("Waiting for RabbitMq cluster to be up");
        LoopHelper<Boolean> loopHelper = new LoopHelper<Boolean>(Timeout.RABBIT_READY,
                "could not verify that RabbitMq service is up", Boolean.TRUE, () -> {
                    return isRabbitClusterUp();
                });
        loopHelper.setIterationDelay(10);
        loopHelper.run();
        mLogger.info("RabbitMq cluster is up");
    }
}
