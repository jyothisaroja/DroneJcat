package com.jcat.cloud.fw.components.system.cee.target;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * This class parses the content of "config.yaml" and extracts the following parameters:
 *
 * - idam user name tagged as "system admin"
 * - ...
 *
 * <b>Copyright:</b> Copyright (c) 2016
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ezhgyin - 2016-03-08 - initial version
 * @author zdagjyo - 2017-08-28 - add method getIdamSystemAdminUser
 * @author zmousar - 2018-02-13 - add methods getStorageProtectionDomain, getProtectionDomainName, getProvisioningTypeName, getStoragePassword
 *         and getStoragePoolName
 */

public class ConfigYamlParser extends BaseYamlParser {

    private static final String TAG_ERICSSON = "ericsson";
    private static final String TAG_IDAM = "idam";
    private static final String TAG_USERLIST = "userlist";
    private static final String TAG_USERS = "users";
    private static final String TAG_IDAM_TAG = "idam_tag";
    private static final String TAG_OPENSTACK_ACCESS = "openstack_access";
    private static final String ADMIN = "admin";
    private static final String TAG_PASSWD = "passwd";
    private static final String TAG_BLADE = "blade";
    private static final String TAG_SHELF = "shelf";
    private static final String TAG_STORAGE = "storage";
    private static final String TAG_SCALEIO = "scaleio";
    private static final String TAG_POOLS = "pools";
    private static final String TAG_PROTECTION_DOMAIN = "protection_domain";
    private static final String TAG_PROVISIONING_TYPE = "provisioning_type";
    private static final String TAG_TYPES = "types";
    private static final String TAG_NAME = "name";
    private static final String TAG_PASSWORD = "password";
    private static final String TAG_ROLES = "roles";
    private static final String TAG_SDS = "sds";
    private String mIdamBackupAdminUser = "";

    /**
     * Constructor
     *
     * @param yamlFile
     */
    public ConfigYamlParser(File yamlFile) {
        super(yamlFile);
    }

    /**
     * Retrieve protectionDomain data for the specified protection_domain name
     *
     * @param protectionDomainName - name of protection_domain configured in scaleio-blade
     * @return protectionDomain data object
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getStorageProtectionDomain(String protectionDomainName) {
        if (protectionDomainName != null) {
            Map<String, Object> configMap = getYamlConfig();
            Map<String, Object> ericssonData = (Map<String, Object>) configMap.get(TAG_ERICSSON);
            Map<String, Object> storageData = (Map<String, Object>) ericssonData.get(TAG_STORAGE);
            Map<String, Object> scaleioData = (Map<String, Object>) storageData.get(TAG_SCALEIO);
            String protectionDomains = TAG_PROTECTION_DOMAIN + "s";
            List<Map<String, Object>> protectionDomainList = (List<Map<String, Object>>) scaleioData
                    .get(protectionDomains);
            for (Map<String, Object> protectionDomain : protectionDomainList) {
                if (!protectionDomain.isEmpty() && protectionDomain.containsKey(TAG_NAME)
                        && protectionDomain.get(TAG_NAME).equals(protectionDomainName)) {
                    return protectionDomain;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public String getIdamBackupAdminUser() {
        if (mIdamBackupAdminUser.isEmpty()) {
            Map<String, Object> configMap = getYamlConfig();
            Map<String, Object> ericssonData = (Map<String, Object>) configMap.get(TAG_ERICSSON);
            Map<String, Object> idamData = (Map<String, Object>) ericssonData.get(TAG_IDAM);
            Map<String, Object> users = (Map<String, Object>) idamData.get(TAG_USERS);
            List<String> userList = (List<String>) idamData.get(TAG_USERLIST);
            if (userList != null) {
                for (int i = 0; i <= userList.size() - 1; i++) {
                    Map<String, Object> usersP = (Map<String, Object>) users.get(userList.get(i));
                    Object idamTag = usersP.get(TAG_IDAM_TAG);
                    if (idamTag.toString().contains(ADMIN)) {
                        mIdamBackupAdminUser = userList.get(i);
                        // INFO NOTE!! Here we assume that there is only one user with idam tag admin and also that this
                        // is the correct user.
                        // Right now this is the case and it returns the ceebackup user but it could be a problem in the
                        // future if users/roles change.
                    }
                }
            }
        }
        return mIdamBackupAdminUser;
    }

    /**
     * Returns the system admin user
     *
     * @return String
     */
    @SuppressWarnings("unchecked")
    public String getIdamSystemAdminUser() {
        String adminUser = null;
        Map<String, Object> configMap = getYamlConfig();
        Map<String, Object> ericssonData = (Map<String, Object>) configMap.get(TAG_ERICSSON);
        Map<String, Object> idamData = (Map<String, Object>) ericssonData.get(TAG_IDAM);
        Map<String, Object> users = (Map<String, Object>) idamData.get(TAG_USERS);
        List<String> userList = (List<String>) idamData.get(TAG_USERLIST);
        if (userList != null) {
            for (int i = 0; i <= userList.size() - 1; i++) {
                Map<String, Object> usersP = (Map<String, Object>) users.get(userList.get(i));
                Object idamTag = usersP.get(TAG_OPENSTACK_ACCESS);
                if (idamTag != null) {
                    adminUser = userList.get(i);
                }
            }
        }
        return adminUser;
    }

    /**
     * Get the protection_domain name of scaleio-blade
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getProtectionDomainName() {
        Map<String, Object> configMap = getYamlConfig();
        Map<String, Object> ericssonData = (Map<String, Object>) configMap.get(TAG_ERICSSON);
        List<Map<String, Object>> shelfList = (List<Map<String, Object>>) ericssonData.get(TAG_SHELF);
        for (Map<String, Object> shelfData : shelfList) {
            List<Map<String, Object>> bladeList = (List<Map<String, Object>>) shelfData.get(TAG_BLADE);
            for (Map<String, Object> blade : bladeList) {
                if (blade.containsKey(TAG_SCALEIO)) {
                    Map<String, Object> scaleioData = (Map<String, Object>) blade.get(TAG_SCALEIO);
                    Map<String, Object> rolesData = (LinkedHashMap<String, Object>) scaleioData.get(TAG_ROLES);
                    List<Map<String, Object>> sdsList = (List<Map<String, Object>>) rolesData.get(TAG_SDS);
                    for (Map<String, Object> sdsData : sdsList) {
                        if (sdsData.containsKey(TAG_PROTECTION_DOMAIN)) {
                            return (String) sdsData.get(TAG_PROTECTION_DOMAIN);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the name of specified provisioning_type
     *
     * @param provisioningType - provisioning type [thick / thin]
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getProvisioningTypeName(String provisioningType) {
        Map<String, Object> protectionDomainData = getStorageProtectionDomain(getProtectionDomainName());
        if (protectionDomainData != null && provisioningType != null) {
            List<Map<String, Object>> poolsList = (List<Map<String, Object>>) protectionDomainData.get(TAG_POOLS);
            for (Map<String, Object> poolData : poolsList) {
                if (!poolData.isEmpty() && poolData.containsKey(TAG_TYPES)) {
                    List<Map<String, Object>> provisioningTypeList = (List<Map<String, Object>>) poolData
                            .get(TAG_TYPES);
                    for (Map<String, Object> provisioningTypeData : provisioningTypeList) {
                        if (!provisioningTypeData.isEmpty() && provisioningTypeData.containsKey(TAG_PROVISIONING_TYPE)
                                && provisioningTypeData.get(TAG_PROVISIONING_TYPE).equals(provisioningType)) {
                            return (String) provisioningTypeData.get(TAG_NAME);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieve storage cluster password
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getStoragePassword() {
        Map<String, Object> configMap = getYamlConfig();
        Map<String, Object> ericssonData = (Map<String, Object>) configMap.get(TAG_ERICSSON);
        Map<String, Object> storageData = (Map<String, Object>) ericssonData.get(TAG_STORAGE);
        Map<String, Object> scaleioData = (Map<String, Object>) storageData.get(TAG_SCALEIO);
        if (scaleioData.containsKey(TAG_PASSWORD)) {
            return (String) scaleioData.get(TAG_PASSWORD);
        }
        return null;
    }

    /**
     * Get the Storage Pool Name
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public String getStoragePoolName() {
        Map<String, Object> protectionDomainData = getStorageProtectionDomain(getProtectionDomainName());
        if (protectionDomainData != null) {
            List<Map<String, Object>> poolsList = (List<Map<String, Object>>) protectionDomainData.get(TAG_POOLS);
            for (Map<String, Object> poolData : poolsList) {
                if (!poolData.isEmpty() && poolData.containsKey(TAG_NAME)) {
                    return (String) poolData.get(TAG_NAME);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getUsers() {
        Map<String, Object> configMap = getYamlConfig();
        Map<String, Object> ericssonData = (Map<String, Object>) configMap.get(TAG_ERICSSON);
        Map<String, Object> idamData = (Map<String, Object>) ericssonData.get(TAG_IDAM);
        Map<String, Object> users = (Map<String, Object>) idamData.get(TAG_USERS);
        List<String> userList = (List<String>) idamData.get(TAG_USERLIST);
        Map<String, String> userPass = new HashMap<String, String>();
        if (userList != null) {
            for (String user : userList) {
                Map<String, Object> usersP = (Map<String, Object>) users.get(user);
                String password = (String) usersP.get(TAG_PASSWD);
                userPass.put(user, password);
            }
        }
        return userPass;
    }
}
