package dev.stratospheric.todoapp.cdk;

import dev.stratospheric.cdk.ApplicationEnvironment;
import dev.stratospheric.cdk.Network;
import dev.stratospheric.cdk.Service;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.util.*;

import static dev.stratospheric.todoapp.cdk.Validations.requireNonEmpty;
import static java.util.Collections.singletonList;

public class ServiceApp {

  public static void main(final String[] args) {
    App app = new App();

    String environmentName = (String) app.getNode().tryGetContext("environmentName");
    requireNonEmpty(environmentName, "context variable 'environmentName' must not be null");

    String applicationName = (String) app.getNode().tryGetContext("applicationName");
    requireNonEmpty(applicationName, "context variable 'applicationName' must not be null");

    String accountId = (String) app.getNode().tryGetContext("accountId");
    requireNonEmpty(accountId, "context variable 'accountId' must not be null");

    String springProfile = (String) app.getNode().tryGetContext("springProfile");
    requireNonEmpty(springProfile, "context variable 'springProfile' must not be null");

    String dockerImageUrl = (String) app.getNode().tryGetContext("dockerImageUrl");
    requireNonEmpty(dockerImageUrl, "context variable 'dockerImageUrl' must not be null");

    String region = (String) app.getNode().tryGetContext("region");
    requireNonEmpty(region, "context variable 'region' must not be null");

    Environment awsEnvironment = makeEnv(accountId, region);

    ApplicationEnvironment applicationEnvironment = new ApplicationEnvironment(
      applicationName,
      environmentName
    );

    Stack serviceStack = new Stack(app, "ServiceStack", StackProps.builder()
      .stackName(applicationEnvironment.prefix("Service"))
      .env(awsEnvironment)
      .build());

    Service.DockerImageSource dockerImageSource = new Service.DockerImageSource(dockerImageUrl);
    Network.NetworkOutputParameters networkOutputParameters = Network.getOutputParametersFromParameterStore(serviceStack, applicationEnvironment.getEnvironmentName());
    Service.ServiceInputParameters serviceInputParameters = new Service.ServiceInputParameters(dockerImageSource,Collections.emptyList(), environmentVariables(springProfile))
      .withHealthCheckIntervalSeconds(30)
      .withTaskRolePolicyStatements(List.of(
        PolicyStatement.Builder.create()
          .effect(Effect.ALLOW)
          .resources(singletonList("*"))
          .actions(singletonList("cognito-idp:*"))
          .build()));

    Service service = new Service(
      serviceStack,
      "Service",
      awsEnvironment,
      applicationEnvironment,
      serviceInputParameters,
      networkOutputParameters);

    app.synth();
  }

  static Map<String, String> environmentVariables(String springProfile) {
    Map<String, String> vars = new HashMap<>();
    vars.put("SPRING_PROFILES_ACTIVE", springProfile);
    return vars;
  }

  static Environment makeEnv(String account, String region) {
    return Environment.builder()
      .account(account)
      .region(region)
      .build();
  }
}
