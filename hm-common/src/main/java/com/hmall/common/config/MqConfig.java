package com.hmall.common.config;

import com.hmall.common.utils.RabbitMqHelper;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    public static final String USER_INFO_KEY = "user-info";

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter jacksonConverter = new Jackson2JsonMessageConverter();

        // 可选：自动创建 messageId，后面做幂等时有用
        jacksonConverter.setCreateMessageIds(true);

        return new AuthMessageConverter(jacksonConverter);
    }

    static class AuthMessageConverter implements MessageConverter {

        private final MessageConverter delegate;

        public AuthMessageConverter(MessageConverter delegate) {
            this.delegate = delegate;
        }

        /**
         * 发送消息时调用：
         * Java对象 -> MQ消息
         *
         * 这里把 UserContext 中的 userId 放入 MQ 消息头
         */
        @Override
        public Message toMessage(Object object, MessageProperties messageProperties)
                throws MessageConversionException {

            Long userId = UserContext.getUser();
            if (userId != null) {
                messageProperties.setHeader(USER_INFO_KEY, userId.toString());
            }

            return delegate.toMessage(object, messageProperties);
        }

        /**
         * 接收消息时调用：
         * MQ消息 -> Java对象
         *
         * 这里从 MQ 消息头中取出 userId，放入 UserContext
         */
        @Override
        public Object fromMessage(Message message)
                throws MessageConversionException {

            Object userInfo = message.getMessageProperties().getHeader(USER_INFO_KEY);
            if (userInfo != null) {
                UserContext.setUser(Long.valueOf(userInfo.toString()));
            }

            return delegate.fromMessage(message);
        }
    }

    @Bean
    public RabbitMqHelper rabbitMqHelper(RabbitTemplate rabbitTemplate){
        return new RabbitMqHelper(rabbitTemplate);
    }
}