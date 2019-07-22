package com.eagle.gateway.server.security.session;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.eagle.gateway.server.util.IdGenUtil;
import com.eagle.gateway.server.vo.GwSession;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Component
@ConfigurationProperties("session")
public class SessionManager {

	public static final String SESSION_REDIS_TOPIC = "session_topic";

	public static Map<String, String> appKeyStore;

	private static int expiredTime;

	private static RedisTemplate<String, String> redisTemplate;

	private static Cache<String, GwSession> appSession;

	@PostConstruct
	public void init() {
		appSession = CacheBuilder.newBuilder().expireAfterWrite(expiredTime, TimeUnit.SECONDS).build();
	}

	/**
	 * 登陆
	 * 
	 * @param appid
	 * @param ticket
	 * @return
	 * @throws Exception
	 */
	public static String login(String appid, String ticket) throws Exception {
		String token = "";
		redisTemplate.convertAndSend(SESSION_REDIS_TOPIC, appid + "|" + token);
		GwSession user = new GwSession();
		user.setAppID("123");
		user.setJobNO("23232");
		user.setOrgID("123");
		user.setUserName("张三");

		String sessionid = IdGenUtil.uuid();
		appSession.put(sessionid, user);
		return sessionid;
	}

	/**
	 * 认证
	 * 
	 * @param sessionid
	 * @return 会话信息
	 * @throws ExecutionException
	 */
	public static GwSession auth(String sessionid) throws ExecutionException {
		return appSession.get(sessionid, null);
	}

	public void setExpiredTime(int expiredTime) {
		SessionManager.expiredTime = expiredTime;
	}

	public void setAppKeyStore(Map<String, String> appKeyStore) {
		SessionManager.appKeyStore = appKeyStore;
	}

	@Autowired
	public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
		SessionManager.redisTemplate = redisTemplate;
	}

}
