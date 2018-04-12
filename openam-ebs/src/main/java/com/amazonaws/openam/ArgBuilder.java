package com.amazonaws.openam;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ArgBuilder {

	private String bucketName = null;
	private String appName = null;
	private String envName = null;
	private String cnamePrefix = null;
	
	public ArgBuilder(String[] args) {
		// create the command line parser
		CommandLineParser parser = new DefaultParser();
		boolean isParamMissing = false;
		// create the Options
		Options options = new Options();
		options.addOption("b", true, "s3 bucket name where package is uploaded");
		options.addOption("a", true, "Elastic beanstalk application name");
		options.addOption("e", true, "Elastic beanstalk environment name. For ex: test, dev");
		options.addOption("c", true, "Elastic beanstalk cname prefix");
		options.addOption("h", false, "show command line options");
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
			if(bucketName == null || appName == null || envName == null || cnamePrefix == null) {
				isParamMissing = true;
			}
			System.out.println(bucketName);
			System.out.println(appName);
			System.out.println(envName);
			System.out.println(cnamePrefix);
			if (line.hasOption("h") || isParamMissing) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "ant", options );
			}
		} catch (ParseException exp) {
			System.out.println("Unexpected exception:" + exp.getMessage());
		}
	}

	public static void main(String[] a) {
		new ArgBuilder(a);
	}
}
