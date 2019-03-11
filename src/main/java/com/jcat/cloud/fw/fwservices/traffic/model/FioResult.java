package com.jcat.cloud.fw.fwservices.traffic.model;

import java.util.Calendar;
import java.util.Map;
import org.apache.log4j.Logger;
import com.jcat.cloud.fw.components.model.EcsComponent;

/**<p>
 * <b>Copyright:</b> Copyright (c) 2015
 * </p>
 * <p>
 * <b>Company:</b> Ericsson
 * </p>
 *
 * Data structure for FIO(File I/O) traffic
 *
 * @author eqinann 2015-09-09 initial version
 *
 */
public class FioResult extends EcsComponent {
    private final Logger mLogger = Logger.getLogger(FioResult.class);
    private String fioVmName;
    private Calendar timestamp;
    private float rIo;
    private float rAggrb;
    private float rMinb;
    private float rMaxb;
    private float rMint;
    private float rMaxt;
    private float wIo;
    private float wAggrb;
    private float wMinb;
    private float wMaxb;
    private float wMint;
    private float wMaxt;

    public FioResult(String name, Calendar timestamp, Map<String, Float> read, Map<String, Float> write) {
        fioVmName = name;
        this.timestamp = timestamp;
        rIo = read.get("io");
        rAggrb = read.get("aggrb");
        rMinb = read.get("minb");
        rMaxb = read.get("maxb");
        rMint = read.get("mint");
        rMaxt = read.get("maxt");
        wIo = write.get("io");
        wAggrb = write.get("aggrb");
        wMinb = write.get("minb");
        wMaxb = write.get("maxb");
        wMint = write.get("mint");
        wMaxt = write.get("maxt");
    }

    public Logger getmLogger() {
        return mLogger;
    }

    public float getReadAggrb() {
        return rAggrb;
    }

    public float getReadIo() {
        return rIo;
    }

    public float getReadMaxb() {
        return rMaxb;
    }

    public float getReadMaxt() {
        return rMaxt;
    }

    public float getReadMinb() {
        return rMinb;
    }

    public float getReadMint() {
        return rMint;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public String getVmName() {
        return fioVmName;
    }

    public float getWriteAggrb() {
        return wAggrb;
    }

    public float getWriteIo() {
        return wIo;
    }

    public float getWriteMaxb() {
        return wMaxb;
    }

    public float getWriteMaxt() {
        return wMaxt;
    }

    public float getWriteMinb() {
        return wMinb;
    }

    public float getWriteMint() {
        return wMint;
    }
}
