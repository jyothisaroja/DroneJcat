package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

import java.util.Calendar;

import com.jcat.cloud.fw.hwmanagement.blademanagement.EquipmentControllerException;

/**
 * <p>
 * Ecs Blade class. This class is a wrapper for GEP blades and any other blade hardware we will use later.
 * </p>
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2013
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author eqinann 2013-06-24 Initial version
 * @author ethssce 2014-06-13 renamed AdministrativeState to BladeAdminState
 *
 */
public class EcsBlade {

    public enum BladeAdminState {
        LOCKED(1), UNLOCKED(2);
        private final int mIntValue;

        private BladeAdminState(int i) {
            mIntValue = i;
        }

        public static BladeAdminState getEnumFromInt(int i) throws EquipmentControllerException {
            for (BladeAdminState iterator : values()) {
                if (iterator.getIntValue() == i) {
                    return iterator;
                }
            }
            throw new EquipmentControllerException("Enum not match for Administrative State!");
        }

        public int getIntValue() {
            return mIntValue;
        }
    };

    public enum AvailabilityStatus {
        NO_STATUS(1), FAILED(2), POWER_OFF(3), OFF_LINE(4), OFF_DUTY(5), DEGRADED(6), NOT_INSTALLED(7), LOG_FULL(8), DEPENDENCY(
                9);
        private final int mIntValue;

        private AvailabilityStatus(int i) {
            mIntValue = i;
        }

        public static AvailabilityStatus getEnumFromInt(int i) throws EquipmentControllerException {
            for (AvailabilityStatus iterator : values()) {
                if (iterator.getIntValue() == i) {
                    return iterator;
                }
            }
            throw new EquipmentControllerException("Enum not match for Availability Status!");
        }

        public int getIntValue() {
            return mIntValue;
        }
    }

    public enum BladeType {
        GEP3, GEP4, GEP5, HP, DELL
    }

    public enum BusType {
        IPMI(1), MBUS(2);
        private final int mIntValue;

        private BusType(int i) {
            mIntValue = i;
        }

        public static BusType getEnumFromInt(int i) throws EquipmentControllerException {
            for (BusType iterator : values()) {
                if (iterator.getIntValue() == i) {
                    return iterator;
                }
            }
            throw new EquipmentControllerException("Enum not match for Bus Type!");
        }

        public int getIntValue() {
            return mIntValue;
        }
    }

    public enum OperationalState {
        DISABLED(1), ENABLED(2);
        private final int mIntValue;

        private OperationalState(int i) {
            mIntValue = i;
        }

        public static OperationalState getEnumFromInt(int i) throws EquipmentControllerException {
            for (OperationalState iterator : values()) {
                if (iterator.getIntValue() == i) {
                    return iterator;
                }
            }
            throw new EquipmentControllerException("Enum not match for Operational State!");
        }

        public int getIntValue() {
            return mIntValue;
        }
    }

    private BladeAdminState mAdminState;

    private OperationalState mOperState;

    private AvailabilityStatus mAvailStatus;

    private BusType mBusType;

    private String mChangeDate;

    private long mConsecutiveMacAddresses;

    private String mFirstMacAddress;

    private Calendar mManufacturingDate;

    private String mProductName;

    private String mProductNumber;

    private String mProductRevisionState;

    private String mSerialNumber;

    private String mVendorName;

    private BladeType mBladeType;

    /**
     * Get administrative state of the blade
     *
     * @return {@link AdministrativeState}
     */
    public BladeAdminState getAdminState() {
        return mAdminState;
    }

    /**
     * Get availability state of the blade
     *
     * @return {@link AvailabilityStatus}
     */
    public AvailabilityStatus getAvailStatus() {
        return mAvailStatus;
    }

    /**
     * Get blade type of the blade
     *
     * @return {@link BladeType}
     */
    public BladeType getBladeType() {
        return mBladeType;
    }

    /**
     * Get bus type of GEP blade
     *
     * @return {@link BusType}
     */
    public BusType getBusType() {
        return mBusType;
    }

    /**
     * Get change date in string
     *
     * @return the changeDate
     */
    public String getChangeDate() {
        return mChangeDate;
    }

    /**
     * Get consecutive MAC addresses
     *
     * @return the consecutiveMacAddresses
     */
    public long getConsecutiveMacAddresses() {
        return mConsecutiveMacAddresses;
    }

    /**
     * Get the first MAC address on the blade
     *
     * @return the firstMacAddress
     */
    public String getFirstMacAddress() {
        return mFirstMacAddress;
    }

    /**
     * Get manufacturing date
     *
     * @return the manufacturingDate
     */
    public Calendar getManufacturingDate() {
        return mManufacturingDate;
    }

    /**
     * Get operational state
     *
     * @return the {@link OperationalState}
     */
    public OperationalState getOperState() {
        return mOperState;
    }

    /**
     * Get the product name
     *
     * @return the productName
     */
    public String getProductName() {
        return mProductName;
    }

    /**
     * Get the product number
     *
     * @return the productNumber
     */
    public String getProductNumber() {
        return mProductNumber;
    }

    /**
     * Get the product revision number
     *
     * @return the productRevisionState
     */
    public String getProductRevisionState() {
        return mProductRevisionState;
    }

    /**
     * Get serial number
     *
     * @return the serialNumber
     */
    public String getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * Get vendor's name
     *
     * @return the vendorName
     */
    public String getVendorName() {
        return mVendorName;
    }

    /**
     * Set administrative state
     *
     * @param adminState the adminState to set
     */
    public void setAdminState(BladeAdminState adminState) {
        mAdminState = adminState;
    }

    /**
     * Set availability status
     *
     * @param availStatus the availStatus to set
     */
    public void setAvailStatus(AvailabilityStatus availStatus) {
        mAvailStatus = availStatus;
    }

    /**
     * Set blade type
     *
     * @param bladeType the bladeType to set
     */
    public void setBladeType(BladeType bladeType) {
        mBladeType = bladeType;
    }

    /**
     * Set bus type of GEP blade
     *
     * @param busType the busType to set
     */
    public void setBusType(BusType busType) {
        mBusType = busType;
    }

    /**
     * Set change date
     *
     * @param changeDate the changeDate to set
     */
    public void setChangeDate(String changeDate) {
        mChangeDate = changeDate;
    }

    /**
     * Set consecutive MAC addresses
     *
     * @param consecutiveMacAddresses the consecutiveMacAddresses to set
     */
    public void setConsecutiveMacAddresses(long consecutiveMacAddresses) {
        mConsecutiveMacAddresses = consecutiveMacAddresses;
    }

    /**
     * Set first MAC address
     *
     * @param firstMacAddress the firstMacAddress to set
     */
    public void setFirstMacAddress(String firstMacAddress) {
        mFirstMacAddress = firstMacAddress;
    }

    /**
     * Set manufacturing date
     *
     * @param manufacturingDate the manufacturingDate to set
     */
    public void setManufacturingDate(Calendar manufacturingDate) {
        mManufacturingDate = manufacturingDate;
    }

    /**
     * Set operational state
     *
     * @param operState the operState to set
     */
    public void setOperState(OperationalState operState) {
        mOperState = operState;
    }

    /**
     * Set product name
     *
     * @param productName the productName to set
     */
    public void setProductName(String productName) {
        mProductName = productName;
    }

    /**
     * Set porduct number
     *
     * @param productNumber the productNumber to set
     */
    public void setProductNumber(String productNumber) {
        mProductNumber = productNumber;
    }

    /**
     * Set product revision state
     *
     * @param productRevisionState the productRevisionState to set
     */
    public void setProductRevisionState(String productRevisionState) {
        mProductRevisionState = productRevisionState;
    }

    /**
     * Set serial number
     *
     * @param serialNumber the serialNumber to set
     */
    public void setSerialNumber(String serialNumber) {
        mSerialNumber = serialNumber;
    }

    /**
     * Set vendor name
     *
     * @param vendorName the vendorName to set
     */
    public void setVendorName(String vendorName) {
        mVendorName = vendorName;
    }

}
