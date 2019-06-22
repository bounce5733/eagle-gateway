package com.eagle.gateway.server.filter.factory;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.eagle.gateway.server.enums.ServerExchangeKey;

import lombok.extern.slf4j.Slf4j;

/**
 * 添加会话头过滤器
 * 
 * @author jiangyonghua
 * @date 2019年6月3日
 */
@Slf4j
@Component
public class SessionHeaderGateFilterFactory
		extends AbstractGatewayFilterFactory<SessionHeaderGateFilterFactory.Config> {

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			log.debug("========进入添加会话头过滤器========");

			String session = exchange.getAttribute(ServerExchangeKey.gw_session.name()) == null ? ""
					: JSON.toJSONString(exchange.getAttribute(ServerExchangeKey.gw_session.name()));
			return chain.filter(exchange.mutate()
					.request(
							exchange.getRequest().mutate().header(ServerExchangeKey.gw_session.name(), session).build())
					.build());
		};
	}

	public SessionHeaderGateFilterFactory() {
		super(Config.class);
	}

	public static class Config {
	}

}
