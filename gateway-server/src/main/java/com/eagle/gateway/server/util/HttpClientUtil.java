package com.eagle.gateway.server.util;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 客户端
 * 
 * @author jiangyonghua
 * @date 2019年6月4日
 */
@Slf4j
public final class HttpClientUtil {
	private static HttpClientUtil instance = null;
	private static Lock lock = new ReentrantLock();
	private CloseableHttpClient httpClient;

	private HttpClientUtil() {
		instance = this;
	}

	public static HttpClientUtil getHttpClient() {
		if (instance == null) {
			lock.lock();
			try {
				if (instance == null) {
					instance = new HttpClientUtil();
					instance.init();
				}
			} finally {
				lock.unlock();
			}
		}
		return instance;
	}

	private final static int MAX_TOTAL = 1000;
	private final static int MAX_CONNECTION_PRE_ROUTE = 100;

	private void init() {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(MAX_TOTAL);
		cm.setDefaultMaxPerRoute(MAX_CONNECTION_PRE_ROUTE);
		httpClient = HttpClientBuilder.create().setConnectionManager(cm).build();
	}

	public byte[] executeAndReturnByte(HttpRequestBase request) {
		HttpEntity entity = null;
		CloseableHttpResponse resp = null;
		byte[] rtn = new byte[0];
		if (request == null)
			return rtn;
		try {
			lock.lock();
			try {
				if (httpClient == null)
					init();
			} finally {
				lock.unlock();
			}
			if (httpClient == null) {
				log.error("{}\nreturn error {}", request.getURI().toString(), "httpclient连接获取异常！");
				return rtn;
			}
			resp = httpClient.execute(request);
			entity = resp.getEntity();
			int statusCode = resp.getStatusLine().getStatusCode();
			if (statusCode == 200) {
				log.info("{}\nreturn correctly httpstatus code:{}", request.getURI().toString(), statusCode);
				String encoding = ("" + resp.getFirstHeader("Content-Encoding")).toLowerCase();
				if (encoding.indexOf("gzip") > 0)
					entity = new GzipDecompressingEntity(entity);
				rtn = EntityUtils.toByteArray(entity);
			} else if (statusCode == 400) {
				rtn = EntityUtils.toByteArray(entity);
				log.error("{}\nreturn error httpstatus code:{}", request.getURI().toString(), statusCode);
			} else {
				log.error("{}\nreturn error httpstatus code:{}", request.getURI().toString(), statusCode);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			EntityUtils.consumeQuietly(entity);
			if (resp != null) {
				try {
					resp.close();
				} catch (Exception e) {
					log.error("httpclient连接释放异常！", e);
				}
			}
		}
		return rtn;
	}

	public String execute(HttpRequestBase request, String charset) throws UnsupportedEncodingException {
		byte[] bytes = executeAndReturnByte(request);
		if (bytes == null || bytes.length == 0)
			return null;
		return new String(bytes, charset);
	}
}