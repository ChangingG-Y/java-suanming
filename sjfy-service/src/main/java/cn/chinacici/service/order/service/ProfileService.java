package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CalendarDayDto;
import cn.chinacici.service.order.dto.ProfileRespDto;
import cn.chinacici.service.order.dto.ProfileUpdateDto;
import cn.chinacici.service.order.dto.WeightRecordDto;

import java.util.List;
import java.util.Map;

public interface ProfileService {
    ProfileRespDto getProfile(Integer userId);

    void updateProfile(Integer userId, ProfileUpdateDto dto);

    void updateAvatar(Integer userId, Integer fileId);

    List<WeightRecordDto> getWeightRecords(Integer userId, int days);

    void saveWeightRecord(Integer userId, Integer tenantId, WeightRecordDto dto);

    void deleteWeightRecord(Integer userId, Integer recordId);

    List<CalendarDayDto> getCalendar(Integer userId, Integer tenantId, int year, int month);

    Map<String, Object> getDayDetail(Integer userId, Integer tenantId, String date);

    /** 获取同一租户下的伴侣用户ID（admin 查用户，或用户查 admin） */
    Integer getPartnerId(Integer userId, Integer tenantId);
}
