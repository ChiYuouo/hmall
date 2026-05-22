package com.hmall.api.client.fallback;

import com.hmall.api.client.CartClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;

@Slf4j
public class CartClientFallbackFactory implements FallbackFactory<CartClient> {

    @Override
    public CartClient create(Throwable cause) {
        return new CartClient(){
            @Override
            public void deleteCartItemByIds(Collection<Long> ids) {
                log.error("远程调用CartClient.deleteCartItemByIds 失败；ids = {}", ids, cause);
            }
        };
    }
}
