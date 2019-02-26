package org.ernest.applications.trampoline.model;

import lombok.Data;
import org.ernest.applications.trampoline.entities.BuildTools;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UpdateMicroService {

    @NotBlank
    private String pomLocation;

    @NotNull
    @Min(1024)
    @Max(65535)
    private Integer defaultPort;

    private String actuatorPrefix = "/actuator";

    private String vmArguments;

    private String appArguments;

    private String gitLocation;

    private BuildTools buildTool;
}

