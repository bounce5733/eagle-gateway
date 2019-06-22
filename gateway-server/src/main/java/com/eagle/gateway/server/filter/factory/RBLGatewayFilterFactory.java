package com.eagle.gateway.server.filter.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import com.eagle.gateway.server.enums.ServerErrorCode;
import com.eagle.gateway.server.enums.ServerExchangeKey;
import com.eagle.gateway.server.exception.ServerException;

import lombok.extern.slf4j.Slf4j;

/**
 * 名单过滤过滤器
 * 
 * @author jiangyonghua
 * @date 2019年6月3日
 */
@Slf4j
@Component
public class RBLGatewayFilterFactory extends AbstractGatewayFilterFactory<RBLGatewayFilterFactory.Config> {

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			log.debug("========进入黑名单检测过滤器========");
			
			String url = exchange.getRequest().getURI().getPath();
			if (config.matchBlacklist(url))
				throw new ServerException(ServerErrorCode.ILLEGAL_URL, url);

			if (config.matchWhitelist(url))
				exchange.getAttributes().put(ServerExchangeKey.is_whitelist_url.name(), true);

			return chain.filter(exchange);
		};
	}

	public RBLGatewayFilterFactory() {
		super(Config.class);
	}

	public static class Config {

		private List<String> blacklistUrl;

		private List<String> whitelistUrl;

		private List<Pattern> blacklistUrlPattern = new ArrayList<>();

		private List<Pattern> whitelistUrlPattern = new ArrayList<>();

		public boolean matchBlacklist(String url) {
			return blacklistUrlPattern.isEmpty() ? false
					: blacklistUrlPattern.stream().filter(p -> p.matcher(url).find()).findAny().isPresent();
		}

		public boolean matchWhitelist(String url) {
			return whitelistUrlPattern.isEmpty() ? false
					: whitelistUrlPattern.stream().filter(p -> p.matcher(url).find()).findAny().isPresent();
		}

		public List<String> getBlacklistUrl() {
			return blacklistUrl;
		}

		public void setBlacklistUrl(List<String> blacklistUrl) {
			this.blacklistUrl = blacklistUrl;
			this.blacklistUrlPattern.clear();
			this.blacklistUrl.forEach(url -> {
				this.blacklistUrlPattern
						.add(Pattern.compile(url.replaceAll("\\*\\*", "(.*?)"), Pattern.CASE_INSENSITIVE));
			});
		}

		public List<String> getWhitelistUrl() {
			return whitelistUrl;
		}

		public void setWhitelistUrl(List<String> whitelistUrl) {
			this.whitelistUrl = whitelistUrl;
			this.whitelistUrlPattern.clear();
			this.whitelistUrl.forEach(url -> {
				this.whitelistUrlPattern
						.add(Pattern.compile(url.replaceAll("\\*\\*", "(.*?)"), Pattern.CASE_INSENSITIVE));
			});
		}
	}

}
