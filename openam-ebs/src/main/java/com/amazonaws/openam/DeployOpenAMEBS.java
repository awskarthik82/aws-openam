package com.amazonaws.openam;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentTier;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;

public class DeployOpenAMEBS {

	// This is the latest stack name for Java 8 / Tomcat 8 environment. Please
	// check AWS docs frequently for new stack name
	private String solutionStackName = "64bit Amazon Linux 2017.09 v2.7.7 running Tomcat 8 Java 8";
	private String warArtifactID = "ebspackage.zip";
	private String versionLabel = "openam-v1";
	private String imageId = "ami-a70eabda";
	private String cnamePrefix = null;
	private String bucketName = null;
	private String appName = null;
	private String envName = null;
//	private String cnamePrefix = "openam-server1";
//	private String bucketName = "lambda-karthik1";
//	private String appName = "openam-server1";
//	private String envName = "openam-test-env";
	
	public DeployOpenAMEBS(String[] args) {
		boolean isParamMissing = initialize(args);
		if(isParamMissing) {
			return;
		}
		// TODO Auto-generated constructor stub
		AWSElasticBeanstalk beanstalkClient = AWSElasticBeanstalkClientBuilder.defaultClient();
		CheckDNSAvailabilityResult dnsResult = beanstalkClient.checkDNSAvailability(new CheckDNSAvailabilityRequest(cnamePrefix));
		if(!dnsResult.isAvailable()) {
			System.out.println("Enter a different CNAME prefix for this environment.");
			System.out.println(cnamePrefix + ".<region>.elasticbeanstalk.com is already taken by someone else.");
			return;
		} else {
			System.out.println("URL for this environment is : " + dnsResult.getFullyQualifiedCNAME());
			System.out.println("Please login to \"AWS Management console > Elastic Beanstalk\" service "
					+ "to monitor this environment");
		}
		// 1. Create application
		beanstalkClient.createApplication(new CreateApplicationRequest().
				 withApplicationName(appName));
		beanstalkClient.createApplicationVersion(createApplicationVersion());
		beanstalkClient.createEnvironment(createEnvironment());
	}

	private boolean initialize(String args[]) {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		boolean isParamMissing = false;
		// create the Options
		Options options = new Options();
		options.addOption("b", true, "s3 bucket name where package is uploaded");
		options.addOption("a", true, "Elastic beanstalk application name");
		options.addOption("e", true, "Elastic beanstalk environment name. For ex: test, dev");
		options.addOption("c", true, "Elastic beanstalk cname prefix");
		options.addOption("h", false, "Help, show command line options");
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			
			if (line.hasOption("b")) {
				bucketName = line.getOptionValue("b");
			}
			if (line.hasOption("a")) {
				appName = line.getOptionValue("a");
			}
			if (line.hasOption("e")) {
				envName = line.getOptionValue("e");
			}
			if (line.hasOption("c")) {
				cnamePrefix = line.getOptionValue("c");
			}
			if(bucketName == null || appName == null ||
			   envName == null | cnamePrefix == null) {
				isParamMissing = true;
			}
			if(bucketName == null && !line.hasOption("h")) {
				System.out.println("-b : S3 Bucket Name is missing");
			}
			if(appName == null && !line.hasOption("h")) {
				System.out.println("-a : Application Name is missing");
			}
			if(envName == null && !line.hasOption("h")) {
				System.out.println("-e : Environment Name is missing");
			}
			if(cnamePrefix == null && !line.hasOption("h")) {
				System.out.println("-c : CNAME prefix is missing");
			}
			String hostOS = System.getProperty("os.name").toLowerCase();
			if (line.hasOption("h") || isParamMissing) {
				HelpFormatter formatter = new HelpFormatter();
				if(hostOS.contains("win")) {
					formatter.printHelp( "awsopenam.bat <OPTIONS> ", options );
				} else {
					formatter.printHelp( "./awsopenam.sh <OPTIONS> ", options );
				}
				return isParamMissing;
			}
			if(!isParamMissing) {
				System.out.println("S3 Bucket : " + bucketName);
				System.out.println("EBS Application Name : " + appName);
				System.out.println("EBS Environment Name : " + envName);
				System.out.println("EBS Application URL Prefix : " + cnamePrefix);
			}
			return isParamMissing;
		} catch (ParseException exp) {
			System.out.println("Unexpected exception:" + exp.getMessage());
			return true;
		}
	}
	
	private CreateApplicationVersionRequest createApplicationVersion() {
	    return new CreateApplicationVersionRequest()
	            .withApplicationName(appName)
	            .withAutoCreateApplication(false)
	            .withSourceBundle(new S3Location(bucketName, warArtifactID))
	            .withVersionLabel(versionLabel);
	}
	
	private List<ConfigurationOptionSetting> setEnvironmentProperties() {
	    List<ConfigurationOptionSetting> configurationOptionSettings = new ArrayList<ConfigurationOptionSetting>();
	    configurationOptionSettings.add(new ConfigurationOptionSetting("aws:autoscaling:launchconfiguration", "InstanceType", "t2.micro"));
	    configurationOptionSettings.add(new ConfigurationOptionSetting("aws:autoscaling:launchconfiguration", "ImageId", imageId));
	    configurationOptionSettings.add(new ConfigurationOptionSetting("aws:autoscaling:asg", "MaxSize", "1"));
	    configurationOptionSettings.add(new ConfigurationOptionSetting("aws:elasticbeanstalk:environment", "EnvironmentType", "SingleInstance"));
	    configurationOptionSettings.add(new ConfigurationOptionSetting("aws:elasticbeanstalk:xray", "XRayEnabled", "false"));
	    return configurationOptionSettings;
	}
	
	private CreateEnvironmentRequest createEnvironment() {
		return new CreateEnvironmentRequest()
				.withApplicationName(appName)
				.withEnvironmentName(envName)
				.withVersionLabel(versionLabel)
				.withCNAMEPrefix(cnamePrefix)
				.withOptionSettings(setEnvironmentProperties())
				.withSolutionStackName(solutionStackName)
				.withTier(new EnvironmentTier().withName("WebServer")
						.withType("Standard"));
	}
	
	public static void main(String[] args) {
		new DeployOpenAMEBS(args);
	}

}
