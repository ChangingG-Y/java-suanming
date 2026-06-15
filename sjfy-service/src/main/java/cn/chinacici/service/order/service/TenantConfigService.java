package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.AiConfigDto;
import cn.chinacici.service.order.dto.LayoutConfigDto;

import java.util.Map;

public interface TenantConfigService {
    String getConfig(Integer tenantId, String key, String defaultValue);
    void setConfig(Integer tenantId, String key, String value);
    Map<String, String> getAllConfig(Integer tenantId);
    LayoutConfigDto getLayoutConfig(Integer tenantId);
    void updateLayoutConfig(Integer tenantId, LayoutConfigDto dto);
    AiConfigDto getAiConfig(Integer tenantId);
    void updateAiConfig(Integer tenantId, AiConfigDto dto);
}
