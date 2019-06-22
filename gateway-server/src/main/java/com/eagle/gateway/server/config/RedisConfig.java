package com.eagle.gateway.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import com.eagle.gateway.server.security.session.SessionManager;

@Configuration
public class RedisConfig {

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Autowired
	private MessageListener messageListener;

	@Bean
	public RedisMessageListenerContainer container() {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(messageListener, new ChannelTopic(SessionManager.SESSION_REDIS_TOPIC));
		return container;
	}
}
