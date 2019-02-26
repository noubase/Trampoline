package org.ernest.applications.trampoline.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ToString
public class Ecosystem {

    private GitCredentials gitCredentials;

    private String mavenBinaryLocation;

    private String mavenHomeLocation;

    private List<MicroService> microservices;

    private List<ExternalInstance> externalInstances;

    private List<Instance> instances;

    private List<MicroservicesGroup> microservicesGroups;

    public Ecosystem() {
        microservices = new ArrayList<>();
        instances = new ArrayList<>();
        microservicesGroups = new ArrayList<>();
        externalInstances = new ArrayList<>();
        gitCredentials = new GitCredentials();
    }

    public boolean removeInstance(String id) {
        return instances.removeIf(i -> i.getId().equalsIgnoreCase(id));
    }

}
