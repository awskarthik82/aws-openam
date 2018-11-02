package com.amazonaws.lambda.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.amazonaws.lambda.demo.AuthPolicy.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * Handles IO for a Java Lambda function as a custom authorizer for API Gateway
 *
 *
 */
public class OpenAMAuthorizer implements RequestHandler<TokenAuthorizerContext, AuthPolicy> {

	@Override
	public AuthPolicy handleRequest(TokenAuthorizerContext input, Context context) {

		String token = input.getAuthorizationToken();
		String principalId = null;
		boolean allow = false;
		// if the client token is not recognized or invalid
		// you can send a 401 Unauthorized response to the client by failing
		// like so:
		// throw new RuntimeException("Unauthorized");

		// if the token is valid, a policy should be generated which will allow
		// or deny access to the client

		// if access is denied, the client will receive a 403 Access Denied
		// response
		// if access is allowed, API Gateway will proceed with the back-end
		// integration configured on the method that was called

		String methodArn = input.getMethodArn();
		String[] arnPartials = methodArn.split(":");
		String region = arnPartials[3];
		String awsAccountId = arnPartials[4];
		String[] apiGatewayArnPartials = arnPartials[5].split("/");
		String restApiId = apiGatewayArnPartials[0];
		String stage = apiGatewayArnPartials[1];
		String httpMethod = apiGatewayArnPartials[2];
		String resource = ""; // root resource
		if (apiGatewayArnPartials.length == 5) {
			resource = apiGatewayArnPartials[3] + "/" + apiGatewayArnPartials[4];
		}
		StringBuilder resourceUrl = new StringBuilder("https://*.execute-api.*.amazonaws.com/");
		resourceUrl.append(resource);
		resourceUrl.append("?action=");
		resourceUrl.append(httpMethod.toUpperCase());
		PolicyResponse evalResponse = evaluatePolicy(context, resourceUrl.toString(), token, httpMethod);
		if (evalResponse != null && evalResponse.isAllowed()) {
			if (evalResponse.getUid() != null) {
				principalId = evalResponse.getUid();
				allow = true;
			}
		}

		// this function must generate a policy that is associated with the
		// recognized principal user identifier.
		// depending on your use case, you might store policies in a DB, or
		// generate them on the fly

		// keep in mind, the policy is cached for 5 minutes by default (TTL is
		// configurable in the authorizer)
		// and will apply to subsequent calls to any method/resource in the
		// RestApi
		// made with the same token

		// the example policy below denies access to all resources in the
		// RestApi
		context.getLogger().log("token : " + token);
		context.getLogger().log("methodArn : " + methodArn);
		context.getLogger().log("arnPartials : " + arnPartials);
		context.getLogger().log("region : " + region);
		context.getLogger().log("awsAccountId : " + awsAccountId);
		context.getLogger().log("apiGatewayArnPartials : " + apiGatewayArnPartials);
		context.getLogger().log("restApiId : " + restApiId);
		context.getLogger().log("stage : " + stage);
		context.getLogger().log("httpMethod : " + httpMethod);
		context.getLogger().log("resource : " + resource);

		AuthPolicy.PolicyDocument policyDoc = null;
		if (allow) {
			policyDoc = AuthPolicy.PolicyDocument.getAllowOnePolicy(region, awsAccountId, restApiId, stage,
					HttpMethod.valueOf(httpMethod), resource);
		} else {
			policyDoc = AuthPolicy.PolicyDocument.getDenyOnePolicy(region, awsAccountId, restApiId, stage,
					HttpMethod.valueOf(httpMethod), resource);
		}
		return new AuthPolicy(principalId, policyDoc);
	}

	private PolicyResponse evaluatePolicy(Context context, String resource, String accessToken, String httpMethod) {
		PolicyResponse policyResp = null;
		try {
			context.getLogger().log("Start policy evaluation");
			String token = authenticate(context);
			if(token != null) {
				HttpClient client = HttpClientBuilder.create().build();
				String openamHost = "http://" + System.getenv("OPENAM_URL");
				context.getLogger().log("openam url : " + openamHost);
				URIBuilder builder = new URIBuilder(
						openamHost + "/auth/json/realms/root/realms/products/policies?_action=evaluate");
				builder.setParameter("_action", "evaluate");
				HttpPost postRequest = new HttpPost(builder.build());
				postRequest.addHeader("iPlanetDirectoryPro", token);
				JSONObject json = new JSONObject();
				JSONArray resources = new JSONArray();
				resources.put(resource);
				json.put("resources", resources);
				json.put("application", "PRODUCT_PS");
				JSONObject subject = new JSONObject();
				subject.put("ssoToken", accessToken);
				json.put("subject", subject);
				context.getLogger().log(json.toString());
				StringEntity se = new StringEntity(json.toString());
				se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
				postRequest.setEntity(se);
				HttpResponse response = client.execute(postRequest);
				HttpEntity entity = response.getEntity();
				JSONArray resultJson = new JSONArray(EntityUtils.toString(entity, "UTF-8"));
				JSONObject jsonResponse = (JSONObject) resultJson.get(0);
				JSONArray uid = (JSONArray) ((JSONObject) jsonResponse.get("attributes")).get("uid");
				context.getLogger().log(uid.get(0).toString());
				JSONObject actions = (JSONObject) jsonResponse.get("actions");
				context.getLogger().log(actions.get(httpMethod).toString());
				policyResp = new PolicyResponse((String) uid.get(0), (Boolean) actions.get(httpMethod));
				context.getLogger().log("uid : " + policyResp.getUid());
			}
		} catch (Exception e) {
			context.getLogger().log("Error in evaluating policy : " + e.getMessage());
		}
		return policyResp;
	}

	private String authenticate(Context context) {
		String token = null;
		try {
			context.getLogger().log("Start authentication");
			HttpClient client = HttpClientBuilder.create().build();
			URIBuilder builder = new URIBuilder(
					"http://openamserver1.us-east-1.elasticbeanstalk.com/auth/json/realms/root/realms/products/authenticate");
			HttpPost postRequest = new HttpPost(builder.build());
			String policyEvalUid = System.getenv("POLICY_EVAL_UNAME");
			String policyEvalPwd = System.getenv("POLICY_EVAL_PWD");
			postRequest.addHeader("X-OpenAM-Username", policyEvalUid);
			postRequest.addHeader("X-OpenAM-Password", policyEvalPwd);
			postRequest.addHeader("Content-type", "application/json");
			HttpResponse response = client.execute(postRequest);
			HttpEntity entity = response.getEntity();
			JSONObject resultJson = new JSONObject(EntityUtils.toString(entity, "UTF-8"));
			token = (String) resultJson.get("tokenId");
			context.getLogger().log("authentication done");
		} catch (Exception e) {
			context.getLogger().log("Error in authenticating user : " + e.getMessage());
		}
		return token;
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
	}
}
