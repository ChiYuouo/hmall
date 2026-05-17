package com.hmall.getway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouter {

    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter writer;

    private final String dataId="gateway-routes.json";
    private final String group="DEFAULT_GROUP";
    private final Set<String> routerIds=new HashSet<>();

    @PostConstruct
    public void initRouteConfigListener() throws NacosException {
        //项目启动，拉取一次配置，并添加配置监听器
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        //监听到配置变更，需要去更新路由表
                        updateConfigInfo(configInfo);
                    }
                });
        //第一次读取到配置，也需要更新到路由表
        updateConfigInfo(configInfo);
    }

    public void updateConfigInfo(String configInfo){
        log.debug("监听到路由配置信息:{}",configInfo);
        //解析配置信息，转为RouterDefinition
        List<RouteDefinition> routerDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        //删除旧的路由表
        for (String routerId : routerIds) {
            writer.delete(Mono.just(routerId)).subscribe();
        }
        routerIds.clear();
        //更新新的路由表
        for (RouteDefinition routerDefinition : routerDefinitions) {
            //更新路由表
            writer.save(Mono.just(routerDefinition)).subscribe();
            //记录路由id，便于下次路由更新时删除
            routerIds.add(routerDefinition.getId());
        }
    }
}
