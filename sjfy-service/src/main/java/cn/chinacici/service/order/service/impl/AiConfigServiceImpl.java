package cn.chinacici.service.order.service.impl;

import cn.chinacici.service.order.dto.AiConfigDto;
import cn.chinacici.service.order.service.AiConfigService;
import cn.chinacici.service.order.service.TenantConfigService;
import org.springframework.stereotype.Service;

@Service
public class AiConfigServiceImpl implements AiConfigService {
    private final TenantConfigService tenantConfigService;

    public AiConfigServiceImpl(TenantConfigService tenantConfigService) {
        this.tenantConfigService = tenantConfigService;
    }

    @Override
    public AiConfigDto getAiConfig(Integer tenantId) {
        return tenantConfigService.getAiConfig(tenantId);
    }

    @Override
    public void updateAiConfig(Integer tenantId, AiConfigDto dto) {
        tenantConfigService.updateAiConfig(tenantId, dto);
    }
}
