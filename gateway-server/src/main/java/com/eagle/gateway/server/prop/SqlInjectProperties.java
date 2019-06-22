package com.eagle.gateway.server.prop;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sql-inject")
public class SqlInjectProperties {

	private static String regex;

	private static Pattern sqlPattern;

	public static boolean match(String content) {
		return getSqlPattern().matcher(content).find();
	}

	public void setRegex(String regex) {
		SqlInjectProperties.regex = regex;
	}

	private static Pattern getSqlPattern() {
		return sqlPattern == null ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) : sqlPattern;
	}
}
