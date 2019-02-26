package org.ernest.applications.trampoline.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MicroService {

    private String id;

    private String name;

    private String pomLocation;

    private String gitLocation;

    private Integer defaultPort;

    private String actuatorPrefix;

    private String vmArguments;

    private String appArguments;

    private BuildTools buildTool;

    private Float version;

}