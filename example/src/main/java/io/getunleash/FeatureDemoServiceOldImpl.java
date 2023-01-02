package io.getunleash;

import org.springframework.stereotype.Service;

@Service("featureOldService")
public class FeatureDemoServiceOldImpl implements FeatureDemoService {
    @Override
    public String getDemoString(String name) {
        return "Old implementation";
    }
}
