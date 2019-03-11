package com.jcat.cloud.fw.components.model;

import java.util.List;

/**
 * This class contains the minimal set of methods that a ECS JCAT FW component
 * needs to implement.
 * A component can be a:
 * -> wrapper for an external script,
 * -> wrapper for an operating system
 * -> service running within one of the CICs.
 * -> The CIC itself is a component of the ECS.
 * -> The hardware, blade, iDrac
 * etc ...
 *
 * The only requirement is that everything that is added into the framework
 * fulfills the following requirements (if possible):
 *
 * Easy to troubleshoot
 *  -> can detect and report if initialization of component is sucessful
 *  -> Can provide logs and information to the framework to ease the troubleshooting if needed
 *
 * Integrity
 *  -> Can remove created resources during test-case and make an integrity check that everything is still OK
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author epergat 2014-12-01
 */
public abstract class EcsComponent {
    /**
     * Enumeration used when calling the EcsComponent methods via invocation.
     */
    public enum Methods {
        deinitialize("deinitialize"), initialize("initialize"), integrityCheck("integrityCheck"), monitor("monitor"), troubleshoot(
                "troubleshoot");

        private String mMethod;

        private Methods(String method) {
            mMethod = method;
        }

        public String getMethod() {
            return mMethod;
        }
    }

    protected Boolean mInitialized = false;

    /**
     * Returns true if the component is still OK after
     * test-case has been executed.
     */
    public Boolean checkIntegrity() {
        // Default implementation
        return null;
    }

    /**
     * At test-case end the cleanup method will be
     * called to remove resources made by this component.
     */
    public Boolean deinitialize() {
        // Default implementation
        return null;
    }

    /**
     * Lazy initialize method initialize components lazily
     * This method is used when the object being created does not
     * need to be initialized right away, or part of the object
     * does not need to be initialized right away
     *
     * For non-lazy initialization, use constructor instead
     *
     */
    public Boolean lazyInitialize() {
        // Default implementation
        return null;
    }

    /**
     * If the component needs monitoring during an interval.
     */
    public void monitor() {
        // Default implementation
    }

    /**
     * If external logs are available, then give an handle to them here
     */
    public List<String> troubleshoot() {
        // Default implementation
        return null;
    }
}
