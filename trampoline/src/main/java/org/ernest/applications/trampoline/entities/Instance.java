package org.ernest.applications.trampoline.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Instance {

    private String id;

    private String ip;

    private String name;

    private String pomLocation;

    private String port;

    private String actuatorPrefix;

    private String vmArguments;

    private String appArguments;

    private String microserviceId;

    public String buildActuatorUrl() {
        return "http://" + getIp() + ":" + getPort() + "/" + getActuatorPrefix();
    }

}
