package com.jcat.cloud.fw.hwmanagement.blademanagement;

import com.ericsson.commonlibrary.cf.spi.ConfigurationData;
import com.jcat.cloud.fw.components.model.target.EcsComputeBlade;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.PortAdminState;

/**
 * Equipment Manager library interface.
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2013-01-17 Initial version
 * @author esauali 2013-05-15 Removed unused methods
 * @author esauali 2013-08-02 add getBlaseInfo()
 * @author epergat 2014-05-19 Generalized interface.
 * @author ethssce 2014-06-13 changed return type of getBladeAdminState() and parameter of setPortState()
 * @author ehosmol 2014-10-12 CleanUp, Remove uncommon functions
 * @author zdagjyo 2017-14-03 modify powerOff and powerOn methods to include ecscomputeblade
 * @author zpralak 2017-10-18 modify powerReset method to include ecscomputeblade
 */
public interface IEquipmentController {

    /**
     * Close connection to the equipment.
     */
    void closeConnection();

    /**
     * Get controller properties/configuration
     *
     * @return
     */
    ConfigurationData getConfiguration();

    /**
     * Check if specified port(interface) is enabled on specified bridge
     *
     * @param bridgeInstance Bridge number
     * @param portInstance Bridge port number
     * @return Boolean
     */
    boolean isPortEnabled(int bridgeInstance, int portInstance);

    /**
     * Open connection
     *
     * @return
     * @throws EquipmentControllerException
     */
    void openConnection() throws EquipmentControllerException;

    /**
     * Power off a blade in the cluster. Method returns only when state is verified
     *
     * @param blade
     * @return
     * @throws EquipmentControllerException
     */
    boolean powerOff(EcsComputeBlade blade) throws EquipmentControllerException;

    /**
     * Power on a blade in the cluster. Method returns only when state is verified
     *
     * @param blade
     * @return
     * @throws EquipmentControllerException
     */
    boolean powerOn(EcsComputeBlade blade) throws EquipmentControllerException;

    /**
     * Perform a power reset on a blade. Method returns only when state is verified
     *
     * @param blade
     * @return
     * @throws EquipmentControllerException
     */
    boolean powerReset(EcsComputeBlade blade) throws EquipmentControllerException;

    /**
     * Enable/disable specified port(interface) on specified bridge
     *
     * @param bridgeInstance Bridge number
     * @param portInstance Bridge port number
     * @param portState AdminState.Enum
     * @return
     */
    void setPortState(int bridgeInstance, int portInstance, PortAdminState portState);

    /**
     * show the default vlan of a specific port of a bridge
     *
     * @param bridgeInstance Bridge number
     * @param portInstance Bridge port number
     * @return vlan
     */
    int showDefaultVlan(int bridgeInstance, int portInstance);

}
