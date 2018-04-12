package com.amazonaws.s3;

import java.io.File;

public class UploadOpenAMPackage {

	public UploadOpenAMPackage() {
		// TODO Auto-generated constructor stub
//C:\Tools\ebspackage
		File file = new File("C:/Tools/ebspackage/ebspackage.zip");
		long contentLength = file.length();
		System.out.println(contentLength);
	}

	public static void main(String a[]) {
		new UploadOpenAMPackage();
	}
}
