package gms.integration.util;

import gms.shared.frameworks.configuration.Configuration;
import gms.shared.frameworks.test.utils.containers.GmsDeploymentContainer;
import gms.shared.frameworks.test.utils.services.GmsServiceType;

import java.io.IOException;
import java.util.Optional;

public class ProcessingConfigUtility {

  public static Optional<String> postConfiguration(GmsDeploymentContainer deploymentContainer, Configuration configuration)
    throws IOException {

    final String rootUrl = String.format("%s%s:%d", ServiceUtility.URL_PREFIX,
        deploymentContainer.getServiceHost(GmsServiceType.PROCESSING_CONFIG_SERVICE),
        deploymentContainer.getServicePort(GmsServiceType.PROCESSING_CONFIG_SERVICE));
    Optional<String> retVal = null;
    retVal = StepUtils.postDataToEndpoint(rootUrl + "/processing-cfg/put", configuration);
    return retVal;
  }


  public static Optional<String> getConfiguration(GmsDeploymentContainer deploymentContainer, String configurationName)
    throws IOException{

    final String rootUrl = String.format("%s%s:%d", ServiceUtility.URL_PREFIX,
        deploymentContainer.getServiceHost(GmsServiceType.PROCESSING_CONFIG_SERVICE),
        deploymentContainer.getServicePort(GmsServiceType.PROCESSING_CONFIG_SERVICE));
    Optional<String> retVal = null;
    retVal=StepUtils
        .postDataToEndpoint(rootUrl + "/processing-cfg/get", configurationName);
    return retVal;

  }
}
