package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CalorieAdviceReqDto;
import cn.chinacici.service.order.dto.CalorieAdviceRespDto;

public interface OrderAiService {
    CalorieAdviceRespDto getCalorieAdvice(CalorieAdviceReqDto req, Integer tenantId);
}
