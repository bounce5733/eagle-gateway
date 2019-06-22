package com.eagle.gateway.server.filter.factory;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import com.eagle.gateway.server.enums.SecurityHeaderKey;
import com.eagle.gateway.server.enums.ServerErrorCode;
import com.eagle.gateway.server.exception.ServerException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 重放攻击检测过滤器
 * 
 * @author jiangyonghua
 * @date 2019年6月3日
 */
@Slf4j
@Component
public class ReplayAttackGatewayFilterFactory
		extends AbstractGatewayFilterFactory<ReplayAttackGatewayFilterFactory.Config> {

	private static Cache<String, Boolean> reqidCache;

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			log.debug("========进入重放攻击检测过滤器========");

			if (reqidCache == null) {
				log.info("初始化重放攻击检测过滤器缓存，过期时间：" + config.expiredTime);
				reqidCache = CacheBuilder.newBuilder().expireAfterWrite(config.expiredTime, TimeUnit.SECONDS).build();
			}

			ServerHttpRequest request = exchange.getRequest();
			String reqid = request.getHeaders().getFirst(SecurityHeaderKey.REQID.value());
			String reqtime = request.getHeaders().getFirst(SecurityHeaderKey.REQTIME.value());
			// 作用于设置了reqid和reqtime的客户端
			if (StringUtils.isEmpty(reqid) || StringUtils.isEmpty(reqtime) || !NumberUtils.isDigits(reqtime))
				throw new ServerException(ServerErrorCode.ILLEGAL_SECURITY_HEADER);

			// 比对是否过期和重复
			if (checkExpired(Long.valueOf(reqtime), config.expiredTime) || reqidCache.getIfPresent(reqid) != null)
				throw new ServerException(ServerErrorCode.REPLAY_ATTACK_ERROR);

			reqidCache.put(reqid, true);
			return chain.filter(exchange);
		};
	}

	public ReplayAttackGatewayFilterFactory() {
		super(Config.class);
	}

	@Data
	public static class Config {
		private int expiredTime;
	}

	private boolean checkExpired(long reqtime, int expiredTime) {
		return System.currentTimeMillis() - reqtime > expiredTime * 1000;
	}

}
