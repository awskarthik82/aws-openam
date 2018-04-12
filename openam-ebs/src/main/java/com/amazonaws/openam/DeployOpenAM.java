package com.amazonaws.openam;

import java.util.List;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeInstancesHealthRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeInstancesHealthResult;

public class DeployOpenAM {
	
	private String appName = "openam-server1";
	private String envName = "openam-server1";

	public DeployOpenAM() {
		AWSElasticBeanstalk ebsClient = AWSElasticBeanstalkClientBuilder.defaultClient();
		System.out.println(ebsClient.checkDNSAvailability(new CheckDNSAvailabilityRequest("openam-server1")));
		DescribeApplicationsRequest openamAppReq = new DescribeApplicationsRequest();
		openamAppReq.withApplicationNames(appName);
		DescribeApplicationsResult appList = ebsClient.describeApplications(openamAppReq);
		System.out.println(appList.getApplications().get(0).getApplicationName());

		DescribeApplicationVersionsRequest appVersionReq = new DescribeApplicationVersionsRequest();
		appVersionReq.withApplicationName(appName);
		DescribeApplicationVersionsResult appVersionResult = ebsClient.describeApplicationVersions(appVersionReq);
		System.out.println(appVersionResult.getApplicationVersions().get(0).getVersionLabel());
		System.out.println(appVersionResult.getApplicationVersions().get(0).getSourceBundle().getS3Bucket());
		System.out.println(appVersionResult.getApplicationVersions().get(0).getSourceBundle().getS3Key());

		
		DescribeEnvironmentsRequest envRequest = new DescribeEnvironmentsRequest()
								.withApplicationName(appName)
								.withEnvironmentNames(envName);
		DescribeEnvironmentsResult envResult = ebsClient.describeEnvironments((envRequest));
		System.out.println(envResult.getEnvironments().get(0).getApplicationName());
		System.out.println(envResult.getEnvironments().get(0).getCNAME());
		System.out.println(envResult.getEnvironments().get(0).getEnvironmentName());
		System.out.println(envResult.getEnvironments().get(0).getPlatformArn());
		System.out.println(envResult.getEnvironments().get(0).getSolutionStackName());
		System.out.println(envResult.getEnvironments().get(0).getEndpointURL());
		System.out.println(envResult.getEnvironments().get(0).getTier().getName());		
		System.out.println(envResult.getEnvironments().get(0).getTier().getType());	
		System.out.println(envResult.getEnvironments().get(0).getHealth());	
		System.out.println(envResult.getEnvironments().get(0).getHealthStatus());	
		
//		DescribeInstancesHealthRequest healthRequest = new DescribeInstancesHealthRequest().withAttributeNames("All").withEnvironmentName(envName);
//		DescribeInstancesHealthResult healthResult = ebsClient.describeInstancesHealth(healthRequest);
//		System.out.println(healthResult.getInstanceHealthList().get(0).getHealthStatus());
//		System.out.println(healthResult.getInstanceHealthList().get(0).getInstanceId());
//		System.out.println(healthResult.getInstanceHealthList().get(0).getInstanceType());
//		System.out.println(healthResult.getInstanceHealthList().get(0).getDeployment().getStatus());
		
		DescribeConfigurationSettingsRequest configOpsRequest = new DescribeConfigurationSettingsRequest();
		configOpsRequest.withApplicationName(appName);
		configOpsRequest.withEnvironmentName(envName);
		DescribeConfigurationSettingsResult configOpsResult = ebsClient.describeConfigurationSettings(configOpsRequest);
		System.out.println(configOpsResult.getConfigurationSettings().get(0).getEnvironmentName());
		System.out.println(configOpsResult.getConfigurationSettings().get(0).getSolutionStackName());
		System.out.println(configOpsResult.getConfigurationSettings().get(0).getDeploymentStatus());
		System.out.println(configOpsResult.getConfigurationSettings().get(0).getApplicationName());
		System.out.println("----------------------------------");
		List<ConfigurationOptionSetting> configList = configOpsResult.getConfigurationSettings().get(0).getOptionSettings();
		for(ConfigurationOptionSetting configOption : configList) {
			if(configOption.getValue() != null && !configOption.getValue().isEmpty()) {
				System.out.println(configOption.getNamespace() + ":" + configOption.getOptionName() + " : " + configOption.getValue());
			}
		}
	}

	public static void main(String[] args) {
		new DeployOpenAM();
	}

}
