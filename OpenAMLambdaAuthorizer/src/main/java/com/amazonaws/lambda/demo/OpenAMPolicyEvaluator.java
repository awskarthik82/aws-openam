package com.amazonaws.lambda.demo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAMPolicyEvaluator {

	public PolicyResponse evaluatePolicy() {
		PolicyResponse policyResp = null;
		try {
			String token = authenticate("policyuser", "pass1234");
			HttpClient client = HttpClientBuilder.create().build();
			URIBuilder builder = new URIBuilder("http://openamserver1.us-east-1.elasticbeanstalk.com/auth/json/realms/root/realms/products/policies?_action=evaluate");
			builder.setParameter("_action", "evaluate");				
			HttpPost postRequest = new HttpPost(builder.build());
			postRequest.addHeader("iPlanetDirectoryPro", token);
			JSONObject json = new JSONObject();
			JSONArray resources = new JSONArray();
			resources.put("https://*.execute-api.*.amazonaws.com/product/1?action=GET");
			json.put("resources", resources);
			json.put("application", "PRODUCT_PS");
			JSONObject subject = new JSONObject();
			subject.put("ssoToken", token);
			json.put("subject", subject);
			StringEntity se = new StringEntity(json.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			postRequest.setEntity(se);
			HttpResponse response = client.execute(postRequest);
			HttpEntity entity = response.getEntity();
			JSONArray resultJson = new JSONArray(EntityUtils.toString(entity, "UTF-8"));
			System.out.println(resultJson.toString());
			JSONObject jsonResponse = (JSONObject)resultJson.get(0);
			JSONArray uid = (JSONArray)((JSONObject)jsonResponse.get("attributes")).get("uid");
			System.out.println(uid.get(0));
			JSONObject actions = (JSONObject)jsonResponse.get("actions");
			System.out.println(actions.get("GET"));
			policyResp = new PolicyResponse((String)uid.get(0), (Boolean)actions.get("GET"));
			System.out.println(policyResp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return policyResp;
	}

	public String authenticate(String userName, String password) {
		String token = null;
		try {
			HttpClient client = HttpClientBuilder.create().build();
			URIBuilder builder = new URIBuilder("http://openamserver1.us-east-1.elasticbeanstalk.com/auth/json/realms/root/realms/products/authenticate");
			HttpPost postRequest = new HttpPost(builder.build());
			postRequest.addHeader("X-OpenAM-Username", userName);
			postRequest.addHeader("X-OpenAM-Password", password);
			postRequest.addHeader("Content-type", "application/json");
			HttpResponse response = client.execute(postRequest);
			HttpEntity entity = response.getEntity();
			JSONObject resultJson = new JSONObject(EntityUtils.toString(entity, "UTF-8"));
			token = (String) resultJson.get("tokenId");
			System.out.println(token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return token;
	}

	public static void main(String a[]) {
		PolicyResponse resp = new OpenAMPolicyEvaluator().evaluatePolicy();
		System.out.println("final response uid : " + resp.getUid());
		System.out.println("final permission : " + resp.isAllowed());
	}

	class PolicyResponse {
		
		String uid;
		boolean allowed;
		
		public PolicyResponse(String uid, boolean allowed) {
			this.uid = uid;
			this.allowed = allowed;
		}
		
		public String getUid() {
			return uid;
		}
		public boolean isAllowed() {
			return allowed;
		}

		@Override
		public String toString() {
			return "PolicyResponse [uid=" + uid + ", allowed=" + allowed + "]";
		}
		
	}
}
