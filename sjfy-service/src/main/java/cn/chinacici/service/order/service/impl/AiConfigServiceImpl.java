package cn.chinacici.service.order.service.impl;

import cn.chinacici.service.order.dto.AiConfigDto;
import cn.chinacici.service.order.entity.LoAiConfig;
import cn.chinacici.service.order.mapper.LoAiConfigMapper;
import cn.chinacici.service.order.service.AiConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiConfigServiceImpl implements AiConfigService {
    private final LoAiConfigMapper configMapper;

    public AiConfigServiceImpl(LoAiConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public String getConfig(String key, String defaultValue) {
        LoAiConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<LoAiConfig>().eq(LoAiConfig::getConfigKey, key)
        );
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    @Override
    public void setConfig(String key, String value) {
        int now = (int)(System.currentTimeMillis() / 1000);
        LoAiConfig existing = configMapper.selectOne(
            new LambdaQueryWrapper<LoAiConfig>().eq(LoAiConfig::getConfigKey, key)
        );
        if (existing != null) {
            configMapper.update(null, new LambdaUpdateWrapper<LoAiConfig>()
                .eq(LoAiConfig::getConfigKey, key)
                .set(LoAiConfig::getConfigValue, value)
                .set(LoAiConfig::getUpdatedAt, now));
        } else {
            LoAiConfig c = new LoAiConfig();
            c.setConfigKey(key);
            c.setConfigValue(value);
            c.setDescription(key);
            c.setCreatedAt(now);
            c.setUpdatedAt(now);
            configMapper.insert(c);
        }
    }

    @Override
    public AiConfigDto getAiConfig() {
        AiConfigDto dto = new AiConfigDto();
        dto.setEnabled(getConfig("order.ai.enabled", "1"));
        dto.setProvider(getConfig("order.ai.provider", "doubao"));
        dto.setModel(getConfig("order.ai.model", "doubao-seed-2-0-lite-260428"));
        dto.setPrompt(getConfig("order.ai.prompt", ""));
        return dto;
    }

    @Override
    public void updateAiConfig(AiConfigDto dto) {
        if (dto.getEnabled() != null) setConfig("order.ai.enabled", dto.getEnabled());
        if (dto.getProvider() != null) setConfig("order.ai.provider", dto.getProvider());
        if (dto.getModel() != null) setConfig("order.ai.model", dto.getModel());
        if (dto.getPrompt() != null) setConfig("order.ai.prompt", dto.getPrompt());
    }
}
