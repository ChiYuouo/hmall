package com.hmall.common.config;

import com.hmall.common.utils.UserContext;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class MqListenerUserContextAspect {

    @After("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    public void clearUserContext() {
        UserContext.removeUser();
    }
}