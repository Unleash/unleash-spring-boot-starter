package io.getunleash;

import org.unleash.features.annotation.Toggle;

public interface FeatureDemoService {
    @Toggle(name = "feature-demo-toggle", alterBean = "featureNewService")
    String getDemoString(String name);
}
