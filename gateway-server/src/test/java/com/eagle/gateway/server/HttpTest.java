package com.eagle.gateway.server;

import java.io.UnsupportedEncodingException;
import java.time.Duration;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

import com.eagle.gateway.server.constant.SysConst;
import com.eagle.gateway.server.enums.SecurityHeaderKey;
import com.eagle.gateway.server.enums.ServerExchangeKey;
import com.eagle.gateway.server.util.IdGenUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpTest {

	private WebTestClient webClient;

	private static final String BASE_URL = "http://127.0.0.1:8080";

	private static final int CONN_TIMEOUT = 10;

	private String sessionid = "";

	@Before
	public void setup() {
		this.webClient = WebTestClient.bindToServer().defaultCookie(ServerExchangeKey.gw_session.name(), sessionid)
				.defaultHeader(SecurityHeaderKey.REQID.value(), IdGenUtil.uuid())
				.defaultHeader(SecurityHeaderKey.REQTIME.value(), String.valueOf(System.currentTimeMillis()))
				.responseTimeout(Duration.ofSeconds(CONN_TIMEOUT)).baseUrl(BASE_URL).build();
	}

	/**
	 * 测试路由
	 */
	@Test
	public void echo() {
		ResponseSpec res = webClient.get().uri(BASE_URL + "/echo/11").exchange().expectStatus().isOk();
		res.expectBody().consumeWith(result -> {
			try {
				log.info("服务器响应：" + new String(result.getResponseBody(), SysConst.ENCODING));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		});
	}

	/**
	 * 测试路由
	 */
	@Test
	@Ignore
	public void echo2() {
		ResponseSpec res = webClient.get().uri(BASE_URL + "/echo2/11").exchange().expectStatus().isOk();
		res.expectBody().consumeWith(result -> {
			try {
				log.info("服务器响应：" + new String(result.getResponseBody(), SysConst.ENCODING));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		});
	}

	/**
	 * 测试重放攻击
	 */
	@Test
	@Ignore
	public void replayAttack() {
		String url = "/echo2/1111";
		ResponseSpec res = webClient.get().uri(url).exchange();
		res.expectBody().consumeWith(result -> {
			try {
				log.info("服务器响应：" + new String(result.getResponseBody(), SysConst.ENCODING));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		});

		res = webClient.get().uri(url).exchange();
		res.expectBody().consumeWith(result -> {
			try {
				log.info("服务器响应：" + new String(result.getResponseBody(), SysConst.ENCODING));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		});

		res = webClient.get().uri(url).exchange();
		res.expectBody().consumeWith(result -> {
			try {
				log.info("服务器响应：" + new String(result.getResponseBody(), SysConst.ENCODING));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		});
	}

	/**
	 * 测试熔断
	 */
	@Test
	@Ignore
	public void fuse() {
		ResponseSpec res = webClient.get().uri("/echo/sleep").exchange();
		res.expectBody().consumeWith(result -> {
			try {
				log.info("服务器响应【" + result.getStatus().value() + "】"
						+ new String(result.getResponseBody(), SysConst.ENCODING));
			} catch (UnsupportedEncodingException e) {
				log.error(e.getMessage(), e);
			}
		});
	}

}
