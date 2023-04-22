# Spring boot starter for Unleash Client SDK for Java
This provides a springboot starter for the official Unleash Client SDK for Java. 
This takes care of the required bootstrapping and creation of Unleash client. This also 
provides a annotation based approach to feature toggling similar to the functionality provided
by FF4J client library. This also takes care of adding the unleash sdk as a transitive
dependency.

## Getting started
The following dependency needs to be added to the springboot project pom.

```xml
<dependency>
    <groupId>io.getunleash</groupId>
    <artifactId>springboot-unleash-starter</artifactId>
    <version>Latest version here</version>
</dependency>
```

### Add the following to application.yaml
```yaml
io:
  getunleash:
    app-name: <application-name>
    instance-id: <instance-id>
    environment: <environment>
    api-url: <url>
    api-token: <token>
```
ex:
```yaml
io:
  getunleash:
    app-name: springboot-test
    instance-id: instance x
    environment: development
    api-url: http://unleash.herokuapp.com/api/
    api-token: '*:development.21a0a7f37e3ee92a0e601560808894ee242544996cdsdsdefgsfgdf'
```
- The configuration takes care of creating configuring `UnleashConfig` and creating an instance of `io.getunleash.Unleash`.
- This takes care of binding all strategy instances (in-built and custom) to the `Unleash` instance.

### Including Spring Security Details in the Unleash Context
Provide an `UnleashContextProvider` bean to add details that Unleash can leverage when evaluating toggle strategies:
```java
@Bean
@ConditionalOnMissingBean
public UnleashContextProvider unleashContextProvider(final UnleashProperties unleashProperties) {
    return () -> {
        UnleashContext.Builder builder = UnleashContext.builder();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            builder.userId(((User)principal).getUsername());
        }
        
        return builder
            .appName(unleashProperties.getAppName())
            .environment(unleashProperties.getEnvironment())
            .build();
    };
}
```

### Usage
- Create a feature toggle `demo-toggle` on unleash server and enabled it.
- Create an interface FeatureDemoService and 2 implementation
```java
public interface FeatureDemoService {
    String getDemoString(String name);
}
```
```java
@Service("featureOldService")
public class FeatureDemoOldServiceImpl implements FeatureDemoService {
    public String getDemoString(String name) {
        return "old implementation";
    }
}
```
```java
@Service("featureNewService")
public class FeatureDemoNewServiceImpl implements FeatureDemoService {
    public String getDemoString(String name) {
        return "New implementation";
    }
}
```
- The requirement is that if the feature is enabled on the server, the new service implementation is used.
- To get the above functionality add the `@Toggle` annotation to the interface,
- If `Context` annotation is not used and UnleashContext is explicitly passed as a method parameter
```java
import io.getunleash.UnleashContext;
import org.unleash.features.annotation.Toggle;

public interface FeatureDemoService {
    @Toggle(name="demo-toggle", alterBean="featureNewService")
    String getDemoString(String name, UnleashContext context);
}
```
- If `@Context` annotation is used to set UnleashContext
```java
import io.getunleash.UnleashContext;
import org.unleash.features.annotation.Toggle;

public interface FeatureDemoService {
    @Toggle(name="demo-toggle", alterBean="featureNewService")
    String getDemoString(String name);
}
```
`FeatureDemoService` is injected where required.
- If `Context` annotation is not used and UnleashContext is explicitly passed as a method parameter
```java
import io.getunleash.UnleashContext;
import org.unleash.features.annotation.Toggle;

public interface FeatureDemoService {
    @Toggle(name="demo-toggle", alterBean="featureNewService")
    String getDemoString(String name, UnleashContext context);
}
```
```java
@RestController
@RequestMapping("/feature")
public class FeatureDemoController {
    private final FeatureDemoService featureDemoService;
    
    public FeatureDemoController(@Qualifier("featureOldService") final FeatureDemoService featureDemoService) {
        this.featureDemoService = featureDemoService;
    }
    
    @GetMapping
    public String feature(@RequestMapping final String name) {
        return featureDemoService.getDemoString(name, UnleashContext.builder().addProperty("name", name).build());
    }
}
```
- If `@Context` annotation is used to set UnleashContext
```java
import io.getunleash.UnleashContext;
import org.unleash.features.annotation.Toggle;

public interface FeatureDemoService {
    @Toggle(name="demo-toggle", alterBean="featureNewService")
    String getDemoString(String name);
}
```
```java
@RestController
@RequestMapping("/feature")
public class FeatureDemoController {
    private final FeatureDemoService featureDemoService;
    
    public FeatureDemoController(@Qualifier("featureOldService") final FeatureDemoService featureDemoService) {
        this.featureDemoService = featureDemoService;
    }
    
    @GetMapping
    public String feature(@RequestMapping @Context(name = "name") final String name) {
        return featureDemoService.getDemoString(name);
    }
}
```

- With the above, if the `demo-toggle` feature is enabled, the `featureNewService` is called even though `featureOldService` was injected.

### Example for variants
- Toggle annotation has support for variants. It can be used as follows:
  ```java
    @Toggle(name = "background-color-feature",
            variants = @FeatureVariants(
                    fallbackBean = "noBackgroundColorService",
                    variants = {
                            @FeatureVariant(name = "green-background-variant", variantBean = "greenBackgroundColorService"),
                            @FeatureVariant(name = "red-background-variant", variantBean = "redBackgroundColorService")
                    }))
  ```
  - In the above example, there are 2 variants `green-background-variant` and `red-background-variant` defined in unleash. Here the implementation to be used for each variant is defined. `fallbackBean` is the implementation that will be used if a variant to bean mapping is not found.
    


- git link to example app below:
  - https://github.com/praveenpg/unleash-starter-demo


## Development

### Releasing a new version

If you have push rights to the repo, make sure there are no modifications that haven't been checked in. Then run
```bash
mvn release:prepare
```
and answer the prompts for new release version. 

Once that's done, a new tag should've been created in the repo, and the [deploy release](./.github/workflows/deploy-release.yml) workflow will do the actual deploy.

Then, run mvn release:clean to clean your repo for release artifacts which only clutters up the main branch. This will leave you 
