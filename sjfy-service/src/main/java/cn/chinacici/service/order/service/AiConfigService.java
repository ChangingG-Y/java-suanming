package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.AiConfigDto;

public interface AiConfigService {
    AiConfigDto getAiConfig(Integer tenantId);
    void updateAiConfig(Integer tenantId, AiConfigDto dto);
}
