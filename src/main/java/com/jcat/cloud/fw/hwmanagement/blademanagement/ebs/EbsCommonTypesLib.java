package com.jcat.cloud.fw.hwmanagement.blademanagement.ebs;

import bspEricssonComTop.ManagedElementDocument.ManagedElement.DmxcFunction.Networks.IPv4Network;

import com.ericsson.dmx.ManagedElementDocument.ManagedElement.DmxFunctions.DmxSysM.Network.ArpTargets;
import com.google.common.base.Joiner;

/**
 *
 * <p>
 * <b>Copyright:</b> Copyright (c) 2014
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * @author ethssce 2014-06-04 - initial implementation with PortAdminState and CommonArpTarget
 * @author ehosmol 2014-10-12 - Added enum for BSP and DMX bridge and bridge port numbers
 */
public class EbsCommonTypesLib {

    /**
     * Bridge number defined in BSP8100
     *
     * @author ehosmol
     */
    public enum BridgeNumber {
        M0S0("0-0", 0), M0S25("0-25", 1), M0S26("0-26", 4), M0S28("0-28", 5), M1S0("1-0", 2), M1S25("1-25", 3), M1S26(
                "1-26", 6), M1S28("1-28", 7);

        private final String value;
        private final int index;

        private BridgeNumber(String value, int index) {
            this.value = value;
            this.index = index;
        }

        /**
         * Maps the specified bridge number index to the relevant bridge number value as string
         *
         * @param index
         * @return String - bridge number
         */
        public static String getValueForIndex(int index) {
            for (BridgeNumber bridgeNumber : BridgeNumber.values()) {
                if (bridgeNumber.getIndex() == index) {
                    return bridgeNumber.getValue();
                }
            }
            return null;
        }

        public int getIndex() {
            return index;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Bridge port number defined in BSP8100
     *
     * @author ehosmol
     */
    public enum BridgePortNumber {

        BP1("BP1", 0), BP10("BP10", 9), BP11("BP11", 10), BP12("BP12", 11), BP13("BP13", 12), BP14("BP14", 13), BP15(
                "BP15", 14), BP16("BP16", 15), BP17("BP17", 16), BP18("BP18", 17), BP19("BP19", 18), BP2("BP2", 1), BP20(
                        "BP20", 19), BP21("BP21", 20), BP22("BP22", 21), BP23("BP23", 22), BP24("BP24", 23), BP26("BP26", 35), BP28(
                                "BP28", 36), BP3("BP3", 2), BP4("BP4", 3), BP5("BP5", 4), BP6("BP6", 5), BP7("BP7", 6), BP8("BP8", 7), BP9(
                                        "BP9", 8), E1("E1", 30), E2("E2", 31), E3("E3", 32), E4("E4", 33), GE1("GE1", 26), GE2("GE2", 27), GE3(
                                                "GE3", 34), LOCALHOST("LOCALHOST", 24), REMOTEHOST("REMOTEHOST", 25), LOCALCMX("LOCALCMX", 28), REMOTECMX(
                                                        "REMOTECMX", 29);

        private final String value;
        private final int index;

        private BridgePortNumber(String value, int index) {
            this.value = value;
            this.index = index;
        }

        /**
         * Maps the specified bridge port number index to the relevant bridge port number value as string
         *
         * @param index
         * @return String - bridge port number
         */
        public static String getValueForIndex(int index) {
            for (BridgePortNumber bridgePortNumber : BridgePortNumber.values()) {
                if (bridgePortNumber.getIndex() == index) {
                    return bridgePortNumber.getValue();
                }
            }
            return null;
        }

        public int getIndex() {
            return index;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * represents a common ArpTarget usable for DMX and BSP in their respective NetonfLib
     */
    public static final class CommonArpTarget {

        private static final int NR_OF_OCTETS = 4;
        private static final int OCTET_MAX_VALUE = 255;

        /**
         * arp responder data
         */
        private final String mId;
        private final String mIpLeft;
        private final String mIpRight;
        private final int mVlan;

        /**
         * hidden constructor, CommonArpTarget will be created by calling createCommonArpTarget
         */
        private CommonArpTarget(String id, String ipLeft, String ipRight, int vlan) {
            mId = id;
            mIpLeft = ipLeft;
            mIpRight = ipRight;
            mVlan = vlan;
        }

        /**
         * create a CommonArpTarget from a DMX3 ARP target object
         *
         * @param arpTarget3 - ArpTargets
         * @return CommonArpTarget
         */
        public static CommonArpTarget createCommonArpTarget(ArpTargets arpTarget3) {
            String subnet = arpTarget3.getSubnet();
            String[] subnetParts1 = subnet.split("\\.");
            String[] subnetParts2 = subnetParts1.clone();
            String mask = arpTarget3.getNetmask();
            String[] maskParts = mask.split("\\.");
            int i = 0;
            for (; i < NR_OF_OCTETS; i++) {
                if (Integer.parseInt(maskParts[i]) != OCTET_MAX_VALUE) {
                    break;
                }
            }
            if (i < NR_OF_OCTETS) {
                int base = Integer.parseInt(subnetParts1[i]);
                subnetParts1[i] = Integer.toString(base + 1);
                subnetParts2[i] = Integer.toString(base + 1);
            }
            Joiner joiner = Joiner.on(".");
            return new CommonArpTarget(arpTarget3.getArpTargetId(), joiner.join(subnetParts1),
                    joiner.join(subnetParts2), arpTarget3.getVlanid());
        }

        /**
         * create a CommonArpTarget from a BSP (DMX4) ARP target object
         *
         * @param arpTarget4 - IPv4Network - NetworkObject containing ARP target information
         * @return CommonArpTarget
         */
        public static CommonArpTarget createCommonArpTarget(IPv4Network arpTarget4) {
            return new CommonArpTarget(arpTarget4.getIPv4NetworkId(), arpTarget4.getDmxAddrLeft(),
                    arpTarget4.getDmxAddrRight(), arpTarget4.getVlanId());
        }

        /**
         *
         * @return - String - ArpTarget ID
         */
        public String getId() {
            return mId;
        }

        /**
         *
         * @return - String - ArpTarget left IP
         */
        public String getIpLeft() {
            return mIpLeft;
        }

        /**
         *
         * @return - String - ArpTarget right IP
         */
        public String getIpRight() {
            return mIpRight;
        }

        /**
         *
         * @return - int - ArpTarget VLAN ID
         */
        public int getVlan() {
            return mVlan;
        }
    }

}
