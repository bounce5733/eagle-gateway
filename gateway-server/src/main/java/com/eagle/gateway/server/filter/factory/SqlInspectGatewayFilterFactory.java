package com.eagle.gateway.server.filter.factory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import com.eagle.gateway.server.enums.ServerErrorCode;
import com.eagle.gateway.server.enums.ServerExchangeKey;
import com.eagle.gateway.server.exception.ServerException;
import com.eagle.gateway.server.prop.SqlInjectProperties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Sql注入检测过滤器
 * 
 * @author jiangyonghua
 * @date 2019年6月21日
 */
@Slf4j
@Component
public class SqlInspectGatewayFilterFactory
		extends AbstractGatewayFilterFactory<SqlInspectGatewayFilterFactory.Config> {

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			log.debug("========进入Sql注入检测过滤器========");

			if (config.isIncludeQueryParams()) {
				String queryParams = exchange.getAttribute(ServerExchangeKey.requestQueryParams.name());
				if (StringUtils.isNotEmpty(queryParams) && SqlInjectProperties.match(queryParams))
					throw new ServerException(ServerErrorCode.SQL_INJECT_ERROR);
			}

			if (config.isIncludeBody()) {
				String bodyData = exchange.getAttribute(ServerExchangeKey.requestBody.name());
				if (StringUtils.isNotEmpty(bodyData) && SqlInjectProperties.match(bodyData))
					throw new ServerException(ServerErrorCode.SQL_INJECT_ERROR);
			}

			return chain.filter(exchange);
		};
	}

	public SqlInspectGatewayFilterFactory() {
		super(Config.class);
	}

	@Data
	public static class Config {

		private boolean includeQueryParams = true;

		private boolean includeBody = true;
	}

}
