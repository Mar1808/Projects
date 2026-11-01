package fr.uge.xplain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public record JavaClassRequest (@JsonProperty("classJava") String classJava,
                                @JsonProperty("errors") String errors,
                                @JsonProperty("correction") String correction,
                                @JsonProperty("xplanation") String xplanation) {
    public JavaClassRequest {
        Objects.requireNonNull(classJava);
        Objects.requireNonNull(errors);
        Objects.requireNonNull(correction);
        Objects.requireNonNull(xplanation);
    }

    public String getClassJava() {
        return classJava;
    }

    public String getErrors() {
        return errors;
    }

    public String getCorrection() {
        return correction;
    }

    public String getXplanation() {
        return xplanation;
    }
}