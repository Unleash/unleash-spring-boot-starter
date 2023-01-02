package io.getunleash;

import org.springframework.stereotype.Service;

@Service("featureNewService")
public class FeatureDemoServiceNewImpl implements FeatureDemoService {
    @Override
    public String getDemoString(String name) {
        return "New implementation";
    }
}
