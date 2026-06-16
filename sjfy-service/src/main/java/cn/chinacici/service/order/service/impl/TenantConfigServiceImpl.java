package cn.chinacici.service.order.service.impl;

import cn.chinacici.service.order.dto.AiConfigDto;
import cn.chinacici.service.order.dto.LayoutConfigDto;
import cn.chinacici.service.order.entity.LoTenantConfig;
import cn.chinacici.service.order.mapper.LoTenantConfigMapper;
import cn.chinacici.service.order.service.TenantConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TenantConfigServiceImpl implements TenantConfigService {
    private final LoTenantConfigMapper configMapper;

    public TenantConfigServiceImpl(LoTenantConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public String getConfig(Integer tenantId, String key, String defaultValue) {
        LoTenantConfig config = configMapper.selectOne(
            new LambdaQueryWrapper<LoTenantConfig>()
                .eq(LoTenantConfig::getTenantId, tenantId)
                .eq(LoTenantConfig::getConfigKey, key)
        );
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    @Override
    public void setConfig(Integer tenantId, String key, String value) {
        int now = (int)(System.currentTimeMillis() / 1000);
        LoTenantConfig existing = configMapper.selectOne(
            new LambdaQueryWrapper<LoTenantConfig>()
                .eq(LoTenantConfig::getTenantId, tenantId)
                .eq(LoTenantConfig::getConfigKey, key)
        );
        if (existing != null) {
            configMapper.update(null, new LambdaUpdateWrapper<LoTenantConfig>()
                .eq(LoTenantConfig::getTenantId, tenantId)
                .eq(LoTenantConfig::getConfigKey, key)
                .set(LoTenantConfig::getConfigValue, value)
                .set(LoTenantConfig::getUpdatedAt, now));
        } else {
            LoTenantConfig c = new LoTenantConfig();
            c.setTenantId(tenantId);
            c.setConfigKey(key);
            c.setConfigValue(value);
            c.setDescription(key);
            c.setUpdatedAt(now);
            configMapper.insert(c);
        }
    }

    @Override
    public Map<String, String> getAllConfig(Integer tenantId) {
        List<LoTenantConfig> list = configMapper.selectList(
            new LambdaQueryWrapper<LoTenantConfig>().eq(LoTenantConfig::getTenantId, tenantId)
        );
        Map<String, String> map = new HashMap<>();
        for (LoTenantConfig c : list) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return map;
    }

    @Override
    public LayoutConfigDto getLayoutConfig(Integer tenantId) {
        Map<String, String> m = getAllConfig(tenantId);
        LayoutConfigDto dto = new LayoutConfigDto();
        dto.setLoginEmoji(m.getOrDefault("layout.login.emoji", "🍱"));
        dto.setLoginTitle(m.getOrDefault("layout.login.title", "小新补给站"));
        dto.setLoginSub(m.getOrDefault("layout.login.sub", "今天想吃点什么呀~"));
        dto.setLoginBtn(m.getOrDefault("layout.login.btn", "进入点餐系统"));
        dto.setTab0Icon(m.getOrDefault("layout.tab.0.icon", "🍱"));
        dto.setTab0Label(m.getOrDefault("layout.tab.0.label", "点菜"));
        dto.setTab1Icon(m.getOrDefault("layout.tab.1.icon", "🍖"));
        dto.setTab1Label(m.getOrDefault("layout.tab.1.label", "订单"));
        dto.setTab2Icon(m.getOrDefault("layout.tab.2.icon", "📖"));
        dto.setTab2Label(m.getOrDefault("layout.tab.2.label", "历史"));
        dto.setAdminTitle(m.getOrDefault("layout.admin.title", "管理后台"));
        dto.setSuccessMsg(m.getOrDefault("layout.order.success_msg", "下单成功！等待接单 🍳"));
        dto.setCalEmojiCooking(m.getOrDefault("layout.cal.emoji.cooking", "🍳"));
        dto.setCalEmojiDining(m.getOrDefault("layout.cal.emoji.dining", "🍜"));
        dto.setCalEmojiDiary(m.getOrDefault("layout.cal.emoji.diary", "📝"));
        return dto;
    }

    @Override
    public void updateLayoutConfig(Integer tenantId, LayoutConfigDto dto) {
        if (dto.getLoginEmoji() != null)  setConfig(tenantId, "layout.login.emoji",  dto.getLoginEmoji());
        if (dto.getLoginTitle() != null)  setConfig(tenantId, "layout.login.title",  dto.getLoginTitle());
        if (dto.getLoginSub() != null)    setConfig(tenantId, "layout.login.sub",    dto.getLoginSub());
        if (dto.getLoginBtn() != null)    setConfig(tenantId, "layout.login.btn",    dto.getLoginBtn());
        if (dto.getTab0Icon() != null)    setConfig(tenantId, "layout.tab.0.icon",   dto.getTab0Icon());
        if (dto.getTab0Label() != null)   setConfig(tenantId, "layout.tab.0.label",  dto.getTab0Label());
        if (dto.getTab1Icon() != null)    setConfig(tenantId, "layout.tab.1.icon",   dto.getTab1Icon());
        if (dto.getTab1Label() != null)   setConfig(tenantId, "layout.tab.1.label",  dto.getTab1Label());
        if (dto.getTab2Icon() != null)    setConfig(tenantId, "layout.tab.2.icon",   dto.getTab2Icon());
        if (dto.getTab2Label() != null)   setConfig(tenantId, "layout.tab.2.label",  dto.getTab2Label());
        if (dto.getAdminTitle() != null)       setConfig(tenantId, "layout.admin.title",         dto.getAdminTitle());
        if (dto.getSuccessMsg() != null)       setConfig(tenantId, "layout.order.success_msg",   dto.getSuccessMsg());
        if (dto.getCalEmojiCooking() != null)  setConfig(tenantId, "layout.cal.emoji.cooking",   dto.getCalEmojiCooking());
        if (dto.getCalEmojiDining() != null)   setConfig(tenantId, "layout.cal.emoji.dining",    dto.getCalEmojiDining());
        if (dto.getCalEmojiDiary() != null)    setConfig(tenantId, "layout.cal.emoji.diary",     dto.getCalEmojiDiary());
    }

    @Override
    public AiConfigDto getAiConfig(Integer tenantId) {
        AiConfigDto dto = new AiConfigDto();
        dto.setEnabled(getConfig(tenantId, "order.ai.enabled", "1"));
        dto.setProvider(getConfig(tenantId, "order.ai.provider", "doubao"));
        dto.setModel(getConfig(tenantId, "order.ai.model", "doubao-seed-2-0-lite-260428"));
        dto.setPrompt(getConfig(tenantId, "order.ai.prompt", ""));
        dto.setDoubaoApiKey(getConfig(tenantId, "ai.doubao_api_key", ""));
        dto.setDeepseekApiKey(getConfig(tenantId, "ai.deepseek_api_key", ""));
        dto.setDishDescPrompt(getConfig(tenantId, "ai.dish_desc_prompt", ""));
        return dto;
    }

    @Override
    public void updateAiConfig(Integer tenantId, AiConfigDto dto) {
        if (dto.getEnabled() != null)         setConfig(tenantId, "order.ai.enabled",       dto.getEnabled());
        if (dto.getProvider() != null)        setConfig(tenantId, "order.ai.provider",      dto.getProvider());
        if (dto.getModel() != null)           setConfig(tenantId, "order.ai.model",         dto.getModel());
        if (dto.getPrompt() != null)          setConfig(tenantId, "order.ai.prompt",        dto.getPrompt());
        if (dto.getDoubaoApiKey() != null)    setConfig(tenantId, "ai.doubao_api_key",      dto.getDoubaoApiKey());
        if (dto.getDeepseekApiKey() != null)  setConfig(tenantId, "ai.deepseek_api_key",    dto.getDeepseekApiKey());
        if (dto.getDishDescPrompt() != null)  setConfig(tenantId, "ai.dish_desc_prompt",    dto.getDishDescPrompt());
    }
}
