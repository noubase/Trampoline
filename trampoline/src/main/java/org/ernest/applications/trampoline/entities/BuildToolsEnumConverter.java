package org.ernest.applications.trampoline.entities;

import org.springframework.core.convert.converter.Converter;

public class BuildToolsEnumConverter implements Converter<String, BuildTools> {

    @Override
    public BuildTools convert(String from) {
        try {
            return BuildTools.valueOf(from.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BuildTools.MAVEN;
        }
    }

}
