package org.ahedstrom.google;

import java.io.UnsupportedEncodingException;

import org.ahedstrom.google.auth.OAuth;
import org.ahedstrom.google.bloggerapi.v3.Posts;
import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public abstract class Api {
	private static final String TAG = "Api";
	
	public interface Callback {
		void onSuccess(JSONObject jsonObject);
		void onFailure();
	}
	
	private final String baseUrl; 
	private final static AsyncHttpClient client = new AsyncHttpClient();
	
	private final OAuth oauth;
	
	protected Api(String baseUrl, OAuth oauth) {
		this.baseUrl = baseUrl;
		this.oauth = oauth;
	}

	protected void invokeGet(final String path, final Callback callback) {
		String url = buildUrl(path);
		Log.d(TAG, "url: " + url);
		client.get(url, new ResponseHander(callback){
			@Override
			public void onSuccess(JSONObject json) {
				callback.onSuccess(json);
			}
			@Override
			public void onFailure(Throwable arg0, JSONObject error) {
				try {
					if (401 == error.getJSONObject("error").getInt("code")) {
						renewOAuthToken(path, callback);
					} else {
						callback.onFailure();
					}
				} catch (JSONException e) {
					callback.onFailure();
				}
			}
		});
	}
	
	protected void invokePost(Context ctx, final String path, Posts entry, final Callback callback) {
		try {
			String url = buildUrl(path);
			client.post(ctx,
					url,
					new Header[]{new BasicHeader("Authorization", "Bearer " + oauth.getAccessToken())},
					new StringEntity(entry.toString(), "utf-8"),
					"application/json",
					new ResponseHander(callback){
						@Override
						public void onSuccess(JSONObject blogs) {
							callback.onSuccess(blogs);
						}
						@Override
						public void onFailure(Throwable arg0, JSONObject error) {
							try {
								if (401 == error.getJSONObject("error").getInt("code")) {
									renewOAuthToken(path, callback);
								} else {
									callback.onFailure();
								}
							} catch (JSONException e) {
								callback.onFailure();
							}
						}
					});
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private void renewOAuthToken(final String path, final Callback callback) {
		RequestParams params = new RequestParams();
		params.put("refresh_token", oauth.getRefreshToken());
		params.put("client_id", oauth.getClientId());
		params.put("grant_type", "refresh_token");
		
		client.post("https://accounts.google.com/o/oauth2/token", params, new ResponseHander(callback) {
			@Override
			public void onSuccess(JSONObject json) {
				try {
					String renewedAccessToken = json.getString("access_token");
					oauth.onAccessTokenExpired(renewedAccessToken);
					invokeGet(path, callback);
				} catch (JSONException e) {
					callback.onFailure();
				}
			}
		});
	}
	
	private String buildUrl(String path) {
		return String.format("%s%s?access_token=%s",
				baseUrl,
				path,
				oauth.getAccessToken());
	}
	
	private static class ResponseHander extends JsonHttpResponseHandler {
		private final Callback callback;

		ResponseHander(Callback callback) {
			this.callback = callback;
		}

		@Override
		public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(int statusCode, Header[] headers,
				String responseBody, Throwable e) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(int statusCode, Header[] headers, Throwable e,
				JSONArray errorResponse) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(int statusCode, Header[] headers, Throwable e,
				JSONObject errorResponse) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(int statusCode, Throwable e,
				JSONArray errorResponse) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(int statusCode, Throwable e,
				JSONObject errorResponse) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(String responseBody, Throwable error) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(Throwable e, JSONArray errorResponse) {
			this.callback.onFailure();
		}
		@Override
		public void onFailure(Throwable e, JSONObject errorResponse) {
			this.callback.onFailure();
		}
	}
}