package com.jcat.cloud.fw.components.model.compute;

import org.openstack4j.model.compute.Keypair;
import com.jcat.cloud.fw.components.model.EcsComponent;

public class EcsKey extends EcsComponent {
    private Keypair keypair;

    public EcsKey(Keypair keypair) {
        this.keypair = keypair;
    }

    public String getName() {
        return keypair.getName();
    }

    public String getPrivateKey() {
        return keypair.getPrivateKey();
    }

    public String getPublicKey() {
        return keypair.getPublicKey();
    }

}
