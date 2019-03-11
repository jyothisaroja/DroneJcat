package com.jcat.cloud.fw.components.system.cee.openstack.heat;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openstack4j.api.Builders;
import org.openstack4j.api.heat.StackService;
import org.openstack4j.api.types.Facing;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.StackCreate;

import com.google.inject.Inject;
import com.jcat.cloud.fw.common.exceptions.EcsOpenStackException;
import com.jcat.cloud.fw.common.logging.EcsLogger;
import com.jcat.cloud.fw.common.logging.elements.EcsAction;
import com.jcat.cloud.fw.common.logging.elements.Verdict;
import com.jcat.cloud.fw.common.parameters.Timeout;
import com.jcat.cloud.fw.common.utils.LoopHelper;
import com.jcat.cloud.fw.components.model.EcsComponent;
import com.jcat.cloud.fw.components.system.cee.openstack.utils.ControllerUtil;
import com.jcat.cloud.fw.infrastructure.os4j.OpenStack4jEcs;

/**
 * This class contains methods related with openstack heat.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2017-11-16 - initial version
 * @author zmousar - 2018-01-03 - Added overloaded method createHeatStack
 */
public final class HeatController extends EcsComponent {

    // enum representing the status of heat stack
    public enum StackStatus {
        CREATE_COMPLETE("CREATE_COMPLETE"), CREATE_FAILED("CREATE_FAILED"), CREATE_IN_PROGRESS("CREATE_IN_PROGRESS");

        private String mStatus;

        StackStatus(String status) {
            mStatus = status;
        }

        public static StackStatus withStatus(String statckStatus) {
            StackStatus[] statuses = StackStatus.values();
            for (StackStatus status : statuses) {
                if (status.toString().equals(statckStatus)) {
                    return status;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return mStatus;
        }
    }

    /**
     * Logger instance
     */
    private final EcsLogger mLogger = EcsLogger.getLogger(HeatController.class);

    /**
     * Keep track of all created stacks by this instance
     * use CopyOnWriteArraySet due to concurrent access to the set(iterate through it and remove item at the same time)
     */
    private final List<String> mCreatedStackIds = new CopyOnWriteArrayList<String>();

    /**
     * List to keep track of stacks which were created (but didn't reach status CREATE_COMPLETE)
     */
    private final List<String> mInitialAndUnsuccessfullyCreatedStackIds = new CopyOnWriteArrayList<String>();

    private final OpenStack4jEcs mOpenStack4jEcs;

    /**
     * Hide Utility Class Constructor: Utility classes should not have a public
     * or default constructor.
     */
    @Inject
    private HeatController(OpenStack4jEcs openStack4jEcs) {
        mOpenStack4jEcs = openStack4jEcs;
    }

    /**
     * Delete all the Stacks from list (used in cleanup)
     * Any exception raised while deleting stack is handled so as not to fail the testcase
     *
     * @param stackIdList - List - A list of stack IDs
     */
    private void deleteAllStacksFromList(List<String> stackIdList) {
        if (stackIdList != null) {
            mLogger.debug("Try to delete all Stacks from list: " + stackIdList);
            for (String stackId : stackIdList) {
                try {
                    deleteHeatStack(stackId, getStackName(stackId));
                } catch (Exception ex) {
                    mLogger.error(String.format("Failed to delete Stack(id:%s), exception was: %s\n", stackId, ex)
                            + ex.getStackTrace());
                }
            }
        }
    }

    private StackService stackService() {
        return ControllerUtil
                .checkRestServiceNotNull(mOpenStack4jEcs.getClient(Facing.PUBLIC).heat(), StackService.class).stacks();
    }

    /**
     * Checks if stack instance reaches specified status within default timeout limit. Exception will be thrown if the
     * stack hasn't reached expected status within that timeout.
     *
     * @param stackId - String - id of the stack
     * @param desiredStatus - enum StackStatus - desired status of the stack
     */
    private void waitForHeatStackStatus(String stackId, StackStatus desiredStatus) {
        mLogger.info(EcsAction.STATUS_CHANGING, HeatController.class, "Stack, id:" + stackId + ", Timeout: "
                + Timeout.CREATE_HEAT_STACK.getTimeoutInSeconds() + "seconds. Target status: " + desiredStatus);
        final String errorMessage = String.format("Stack %s did not reach status: %s", stackId, desiredStatus);
        LoopHelper<StackStatus> loopHelper = new LoopHelper<StackStatus>(Timeout.CREATE_HEAT_STACK, errorMessage,
                desiredStatus, () -> {
                    Stack stack = stackService().getStackByName(getStackName(stackId));
                    StackStatus currentStatus = StackStatus.withStatus(stack.getStatus());
                    mLogger.debug(String.format("Current stack status:" + currentStatus));
                    if (currentStatus != null && currentStatus.equals("CREATE_FAILED")) {
                        throw new EcsOpenStackException(
                                "Stack creation failed due to reason: " + stack.getStackStatusReason());
                    }
                    return currentStatus;
                });
        loopHelper.setIterationDelay(30);
        loopHelper.run();
        mLogger.info(Verdict.STATUS_CHANGED, desiredStatus, HeatController.class, "Stack, id:" + stackId);
    }

    /**
     * Checks if stack instance has been deleted within default timeout limit. Exception will be thrown if the
     * stack is not deleted within that timeout.
     *
     * @param stackName - String - name of the stack
     */
    private void waitForStackDeleted(String stackName) {
        mLogger.info(EcsAction.DELETING, HeatController.class,
                "stack " + stackName + " in " + Timeout.DELETE_HEAT_STACK.getTimeoutInSeconds() + " seconds");
        new LoopHelper<Boolean>(Timeout.DELETE_HEAT_STACK, "Stack " + stackName + " was still found after deletion",
                Boolean.TRUE, () -> {
                    Stack stackToBeDeleted = stackService().getStackByName(stackName);
                    if (null != stackToBeDeleted) {
                        mLogger.debug("Stack '" + stackToBeDeleted.getId() + "' still exists.");
                        mLogger.debug("Stack Detail: '" + stackToBeDeleted);
                        return false;
                    }
                    return true;
                }).run();
        mLogger.info(Verdict.DELETED, HeatController.class, "stack " + stackName);
    }

    /**
     * Cleanup all allocated resources made in the Heat controller.
     */
    public void cleanup() {
        mLogger.info(EcsAction.STARTING, "Clean up", HeatController.class, "");
        deleteAllStacksFromList(mCreatedStackIds);
        deleteAllStacksFromList(mInitialAndUnsuccessfullyCreatedStackIds);
        mLogger.info(Verdict.DONE, "Clean up", HeatController.class, "");
    }

    /**
     * Creates a Heat Stack instance with specified stack-name and template(yaml) file.
     *
     * @param stackName  - String - name of the stack
     * @param templateFileName - String - name of the template file (.yaml) to launch the stack with
     * @return mStackId  - id of the created stack
     */
    public String createHeatStack(String stackName, String templateFileName) {
        return createHeatStack(stackName, templateFileName, null);
    }

    /**
     * Creates a heat stack instance with specified parameters.
     *
     * @param stackName - String - name of the stack
     * @param templateFileName - String - name of the template file (.yaml) to launch the stack with
     * @param envFileName - String - name of the environment file(.env) to specify the environment for
     *                      VMs that are going to be launched by heat stack
     * @return String - id of the created stack
     */
     public String createHeatStack(String stackName, String templateFileName, String envFileName) {
        mLogger.info(EcsAction.CREATING, HeatController.class, "stack " + stackName);
        StackCreate stackToCreate;
        if (envFileName == null) {
            stackToCreate = Builders.stack().name(stackName).templateFromFile(templateFileName).build();
        } else {
            stackToCreate = Builders.stack().name(stackName).templateFromFile(templateFileName)
                    .environmentFromFile(envFileName).build();
        }
        Stack stack = stackService().create(stackToCreate);

        if (stack == null) {
            throw new EcsOpenStackException("Failed to create stack " + stackName);
        }
        String stackId = stack.getId();
        mInitialAndUnsuccessfullyCreatedStackIds.add(stackId);
        waitForHeatStackStatus(stackId, StackStatus.withStatus("CREATE_COMPLETE"));
        mLogger.info(Verdict.CREATED, HeatController.class, "stack, id:" + stackId);
        mCreatedStackIds.add(stackId);
        Iterator<String> iter = mInitialAndUnsuccessfullyCreatedStackIds.iterator();
        while (iter.hasNext()) {
            String currStackId = iter.next();
            if (currStackId.equals(stackId)) {
                mInitialAndUnsuccessfullyCreatedStackIds.remove(stackId);
            }
        }
        return stackId;
    }

    /**
     * Deletes the heat stack instance with specified parameters.
     *
     * @param stackName - String - name of the stack to be deleted
     * @param stackId - String - id of the stack to be deleted
     */
    public void deleteHeatStack(String stackName, String stackId) {
        stackService().delete(stackName, stackId);
        waitForStackDeleted(stackName);
        if (mCreatedStackIds.contains(stackId)) {
            mCreatedStackIds.remove(stackId);
        }
        if (mInitialAndUnsuccessfullyCreatedStackIds.contains(stackId)) {
            mInitialAndUnsuccessfullyCreatedStackIds.remove(stackId);
        }
    }

    /**
     * Checks if the specified heat stack exists.
     *
     * @param stackId - String - id of the stack
     * @return boolean - true if stack exists, false otherwise
     */
    public boolean doesHeatStackExist(String stackId) {
        return getStackName(stackId) != null;
    }

    /**
     * Retrieves the stack name by stack id.
     *
     * @param stackId - String - id of the stack
     * @return String - name of the stack with specified id
     */
    public String getStackName(String stackId) {
        List<? extends Stack> stackList = stackService().list();
        for (Stack stack : stackList) {
            if (stack.getId().equals(stackId)) {
                return stack.getName();
            }
        }
        mLogger.warn("No stack exists with id: " + stackId);
        return null;
    }

    /**
     * Retrieves the status of the stack with specified parameters.
     *
     * @param stackName - String - name of the stack
     * @param stackId - String - id of the stack
     * @return StackStatus - status of the stack
     */
    public StackStatus getStackStatus(String stackName, String stackId) {
        String status = stackService().getDetails(stackName, stackId).getStatus();
        return StackStatus.withStatus(status);
    }
}
