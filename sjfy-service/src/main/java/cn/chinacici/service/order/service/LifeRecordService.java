package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.DiaryDto;
import cn.chinacici.service.order.dto.RestaurantVisitDto;

import java.util.List;

public interface LifeRecordService {

    // ---- 下馆子 ----
    RestaurantVisitDto saveVisit(Integer userId, Integer tenantId, RestaurantVisitDto dto);

    List<RestaurantVisitDto> getVisitsByDate(Integer userId, String date);

    void deleteVisit(Integer userId, Integer visitId);

    // ---- 日记 ----
    DiaryDto saveDiary(Integer userId, Integer tenantId, DiaryDto dto);

    DiaryDto getDiaryByDate(Integer userId, String date);

    void deleteDiary(Integer userId, Integer diaryId);
}
