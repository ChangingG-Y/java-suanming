package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.AiConfigDto;

public interface AiConfigService {
    /** 读取单个配置值，不存在返回 defaultValue */
    String getConfig(String key, String defaultValue);

    /** 更新/覆盖配置 */
    void setConfig(String key, String value);

    /** 获取管理后台展示的 AI 配置汇总 */
    AiConfigDto getAiConfig();

    /** 更新管理后台 AI 配置 */
    void updateAiConfig(AiConfigDto dto);
}
