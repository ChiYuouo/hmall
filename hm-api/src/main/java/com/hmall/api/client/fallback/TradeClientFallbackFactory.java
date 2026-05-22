package com.hmall.api.client.fallback;
import com.hmall.api.client.TradeClient;;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


@Slf4j
public class TradeClientFallbackFactory implements FallbackFactory<TradeClient> {

    @Override
    public TradeClient create(Throwable cause) {
        return new TradeClient() {
            @Override
            public void markOrderPaySuccess(Long orderId) {
                log.error("远程调用CartClient.deleteCartItemByIds 失败；ids = {}", orderId, cause);
            }
        };
    }
}
