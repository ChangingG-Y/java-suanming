package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CalorieAdviceReqDto;
import cn.chinacici.service.order.dto.CalorieAdviceRespDto;

public interface OrderAiService {
    /**
     * 根据点菜列表调用 AI 获取热量估算和用餐建议。
     * 返回的 advice 为 80 字以内的简短文本；如果 AI 功能关闭，advice 为 null，enabled 为 false。
     */
    CalorieAdviceRespDto getCalorieAdvice(CalorieAdviceReqDto req);
}
