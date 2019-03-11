package com.jcat.cloud.fw.components.system.cee.services.crm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import com.jcat.cloud.fw.components.model.target.session.EcsSession;
import com.jcat.cloud.fw.components.system.cee.target.EcsCicList;

/**
 * Generic Service class. Base class to objectize CRM and RabbitMQ as Java services
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2017
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author zdagjyo - 2017-05-31 - initial version
 **/
public class GenericService {

    private static final String CRM_STATUS = "sudo crm_mon -1 ";
    private final Logger mLogger = Logger.getLogger(GenericService.class);

    protected final int mNumberOfCics;
    protected final EcsSession mSshSession;

    public GenericService(EcsSession sshSession, EcsCicList ecsCicList) {
        mSshSession = sshSession;
        mNumberOfCics = ecsCicList.size();
    }

    /**
     * Parse CRM status list with []
     *
     * @param text
     * @return
     */
    private String[] parseCrmList(String text) {
        String group = null;
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher match = pattern.matcher(text);
        if (match.find()) {
            group = match.group(1);
        }
        mLogger.debug("Groups:" + group);
        return org.apache.commons.lang.StringUtils.split(group, " ");
    }

    protected String grep(String keyword) {
        return " | grep " + keyword + " ";
    }

    /**
     * Query CRM status
     *
     * @param filters
     * @return
     */
    protected String queryCrmStatusWithFilter(String filters) {
        return mSshSession.send(CRM_STATUS + filters);
    }

    /**
     * @return a list of CICs with CRM status Online
     */
    public String[] online() {
        return parseCrmList(queryCrmStatusWithFilter(grep("Online")));
    }
}
