package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

import com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.BladeGroupManagement.Group.ShelfSlot.Blade;
import com.jcat.cloud.fw.hwmanagement.blademanagement.EquipmentControllerException;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EcsBlade.AvailabilityStatus;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EcsBlade.BladeAdminState;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EcsBlade.BladeType;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EcsBlade.BusType;
import com.jcat.cloud.fw.hwmanagement.blademanagement.ebs.EcsBlade.OperationalState;

/**
 * <p>
 * ECS blade build class. Used for building up ecs blade from whatever blade hardware implementation
 * </p>
 * <p>
 * The builder can be used as constructor or as a normal java builder.
 * </p>
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2013-06-24 initial version
 * @author ethssce 2014-06-13 changed AdministrativeState to BladeAdminState
 *
 */
public class EcsBladeBuilder {
    private static final int BLADE_TYPE_LENGTH = 4;

    /**
     * Build ECS blade from GEP blade by using DMX blade information.
     *
     * Blade class passed as argument is using importion now but might need full package call when different kinds of
     * "blades" are imported/introduced
     *
     * @param GEP blade instance provided by DMX lib
     * @return
     * @throws EquipmentControllerException
     */
    public EcsBlade build(Blade gepBlade) throws EquipmentControllerException {
        EcsBlade blade = new EcsBlade();
        String productName = gepBlade.getProductName().substring(0, BLADE_TYPE_LENGTH);
        blade.setBladeType(BladeType.valueOf(productName));
        blade.setChangeDate(gepBlade.getChangeDate());
        blade.setOperState(OperationalState.getEnumFromInt(gepBlade.getOperationalState().intValue()));
        blade.setAdminState(BladeAdminState.getEnumFromInt(gepBlade.getAdministrativeState().intValue()));
        blade.setAvailStatus(AvailabilityStatus.getEnumFromInt(gepBlade.getAvailabilityStatus().intValue()));
        blade.setBusType(BusType.getEnumFromInt(gepBlade.getBusType().intValue()));
        blade.setManufacturingDate(gepBlade.getManufacturingDate());
        blade.setConsecutiveMacAddresses(gepBlade.getConsecutiveMacAddresses());
        blade.setFirstMacAddress(gepBlade.getFirstMacAddress());
        blade.setProductName(gepBlade.getProductName());
        blade.setProductNumber(gepBlade.getProductNumber());
        blade.setProductRevisionState(gepBlade.getProductRevisionState());
        blade.setSerialNumber(gepBlade.getSerialNumber());
        blade.setVendorName(gepBlade.getVendorName());
        return blade;
    }
}
