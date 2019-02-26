package org.ernest.applications.trampoline.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class CreateMicroService extends UpdateMicroService {

    @NotBlank
    private String name;

}
