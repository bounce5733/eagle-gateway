package com.eagle.gateway.server.filter.factory;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebSession;

import com.eagle.gateway.server.enums.ServerErrorCode;
import com.eagle.gateway.server.enums.ServerExchangeKey;
import com.eagle.gateway.server.exception.ServerException;

import lombok.extern.slf4j.Slf4j;

/**
 * 认证过滤器
 * 
 * @author jiangyonghua
 * @date 2019年6月3日
 */
@Slf4j
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.Config> {

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			log.debug("========进入认证过滤器========");

			// 过滤白名单
			Object isWhitelistUrlFlag = exchange.getAttribute(ServerExchangeKey.is_whitelist_url.name());
			if (null != isWhitelistUrlFlag && Boolean.valueOf(isWhitelistUrlFlag.toString()))
				return chain.filter(exchange);

			WebSession webSession = exchange.getSession().block();
			if (null == webSession.getAttribute(ServerExchangeKey.gw_user.name()))
				throw new ServerException(ServerErrorCode.AUTHENTICATE_FAILED);

			try {
				exchange.getAttributes().put(ServerExchangeKey.appid.name(),
						webSession.getAttribute(ServerExchangeKey.appid.name()));
				exchange.getAttributes().put(ServerExchangeKey.gw_session.name(),
						webSession.getAttribute(ServerExchangeKey.gw_user.name()));
				return chain.filter(exchange);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				throw new ServerException(ServerErrorCode.AUTHENTICATE_FAILED);
			}
		};
	}

	public AuthGatewayFilterFactory() {
		super(Config.class);
	}

	public static class Config {

	}

}
