package com.eagle.gateway.server.security.session;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionListener implements MessageListener {
	@Override
	public void onMessage(Message message, byte[] pattern) {
		log.info("收到会话刷新数据：" + message);
	}

}
