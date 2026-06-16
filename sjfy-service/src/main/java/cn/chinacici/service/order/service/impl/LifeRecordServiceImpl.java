package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.DiaryDto;
import cn.chinacici.service.order.dto.RestaurantVisitDto;
import cn.chinacici.service.order.entity.LoDiary;
import cn.chinacici.service.order.entity.LoDiaryImage;
import cn.chinacici.service.order.entity.LoRestaurantVisit;
import cn.chinacici.service.order.entity.LoRestaurantVisitImage;
import cn.chinacici.service.order.mapper.LoDiaryImageMapper;
import cn.chinacici.service.order.mapper.LoDiaryMapper;
import cn.chinacici.service.order.mapper.LoRestaurantVisitImageMapper;
import cn.chinacici.service.order.mapper.LoRestaurantVisitMapper;
import cn.chinacici.service.order.service.LifeRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LifeRecordServiceImpl implements LifeRecordService {
    private final LoRestaurantVisitMapper visitMapper;
    private final LoRestaurantVisitImageMapper visitImageMapper;
    private final LoDiaryMapper diaryMapper;
    private final LoDiaryImageMapper diaryImageMapper;

    public LifeRecordServiceImpl(LoRestaurantVisitMapper visitMapper,
                                 LoRestaurantVisitImageMapper visitImageMapper,
                                 LoDiaryMapper diaryMapper,
                                 LoDiaryImageMapper diaryImageMapper) {
        this.visitMapper = visitMapper;
        this.visitImageMapper = visitImageMapper;
        this.diaryMapper = diaryMapper;
        this.diaryImageMapper = diaryImageMapper;
    }

    @Override
    @Transactional
    public RestaurantVisitDto saveVisit(Integer userId, Integer tenantId, RestaurantVisitDto dto) {
        if (dto.getVisitDate() == null || dto.getRestaurantName() == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "日期和餐厅名称不能为空");
        }
        int now = (int) (System.currentTimeMillis() / 1000);

        if (dto.getId() != null) {
            // update
            LoRestaurantVisit existing = visitMapper.selectById(dto.getId());
            if (existing == null || !existing.getUserId().equals(userId)) {
                throw new ServiceException(ResultCode.PARAMETER_ERROR, "记录不存在");
            }
            existing.setRestaurantName(dto.getRestaurantName());
            existing.setMealType(dto.getMealType());
            existing.setScore(dto.getScore());
            existing.setContent(dto.getContent());
            existing.setUpdatedAt(now);
            visitMapper.updateById(existing);
            if (dto.getFileIds() != null) {
                visitImageMapper.delete(new LambdaQueryWrapper<LoRestaurantVisitImage>().eq(LoRestaurantVisitImage::getVisitId, dto.getId()));
                saveVisitImages(dto.getId(), dto.getFileIds(), now);
            }
            dto.setImageUrls(loadVisitImageUrls(dto.getId()));
        } else {
            LoRestaurantVisit visit = new LoRestaurantVisit();
            visit.setUserId(userId);
            visit.setTenantId(tenantId);
            visit.setVisitDate(dto.getVisitDate());
            visit.setMealType(dto.getMealType());
            visit.setRestaurantName(dto.getRestaurantName());
            visit.setScore(dto.getScore());
            visit.setContent(dto.getContent());
            visit.setCreatedAt(now);
            visit.setUpdatedAt(now);
            visitMapper.insert(visit);
            dto.setId(visit.getId());
            if (dto.getFileIds() != null && !dto.getFileIds().isEmpty()) {
                saveVisitImages(visit.getId(), dto.getFileIds(), now);
            }
            dto.setImageUrls(loadVisitImageUrls(visit.getId()));
        }
        return dto;
    }

    @Override
    public List<RestaurantVisitDto> getVisitsByDate(Integer userId, String date) {
        List<LoRestaurantVisit> visits = visitMapper.selectList(
            new LambdaQueryWrapper<LoRestaurantVisit>()
                .eq(LoRestaurantVisit::getUserId, userId)
                .eq(LoRestaurantVisit::getVisitDate, date)
                .orderByAsc(LoRestaurantVisit::getMealType)
        );
        return visits.stream().map(v -> {
            RestaurantVisitDto d = toVisitDto(v);
            d.setImageUrls(loadVisitImageUrls(v.getId()));
            return d;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteVisit(Integer userId, Integer visitId) {
        LoRestaurantVisit v = visitMapper.selectById(visitId);
        if (v == null || !v.getUserId().equals(userId)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "记录不存在");
        }
        visitImageMapper.delete(new LambdaQueryWrapper<LoRestaurantVisitImage>().eq(LoRestaurantVisitImage::getVisitId, visitId));
        visitMapper.deleteById(visitId);
    }

    @Override
    @Transactional
    public DiaryDto saveDiary(Integer userId, Integer tenantId, DiaryDto dto) {
        if (dto.getDiaryDate() == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "日期不能为空");
        }
        int now = (int) (System.currentTimeMillis() / 1000);

        LoDiary existing = diaryMapper.selectOne(
            new LambdaQueryWrapper<LoDiary>()
                .eq(LoDiary::getUserId, userId)
                .eq(LoDiary::getDiaryDate, dto.getDiaryDate())
        );
        if (existing != null) {
            existing.setContent(dto.getContent());
            existing.setUpdatedAt(now);
            diaryMapper.updateById(existing);
            if (dto.getFileIds() != null) {
                diaryImageMapper.delete(new LambdaQueryWrapper<LoDiaryImage>().eq(LoDiaryImage::getDiaryId, existing.getId()));
                saveDiaryImages(existing.getId(), dto.getFileIds(), now);
            }
            dto.setId(existing.getId());
        } else {
            LoDiary diary = new LoDiary();
            diary.setUserId(userId);
            diary.setTenantId(tenantId);
            diary.setDiaryDate(dto.getDiaryDate());
            diary.setContent(dto.getContent());
            diary.setCreatedAt(now);
            diary.setUpdatedAt(now);
            diaryMapper.insert(diary);
            dto.setId(diary.getId());
            if (dto.getFileIds() != null && !dto.getFileIds().isEmpty()) {
                saveDiaryImages(diary.getId(), dto.getFileIds(), now);
            }
        }
        dto.setImageUrls(loadDiaryImageUrls(dto.getId()));
        return dto;
    }

    @Override
    public DiaryDto getDiaryByDate(Integer userId, String date) {
        LoDiary diary = diaryMapper.selectOne(
            new LambdaQueryWrapper<LoDiary>()
                .eq(LoDiary::getUserId, userId)
                .eq(LoDiary::getDiaryDate, date)
        );
        if (diary == null) return null;
        DiaryDto dto = new DiaryDto();
        dto.setId(diary.getId());
        dto.setDiaryDate(diary.getDiaryDate());
        dto.setContent(diary.getContent());
        dto.setImageUrls(loadDiaryImageUrls(diary.getId()));
        return dto;
    }

    @Override
    @Transactional
    public void deleteDiary(Integer userId, Integer diaryId) {
        LoDiary d = diaryMapper.selectById(diaryId);
        if (d == null || !d.getUserId().equals(userId)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "记录不存在");
        }
        diaryImageMapper.delete(new LambdaQueryWrapper<LoDiaryImage>().eq(LoDiaryImage::getDiaryId, diaryId));
        diaryMapper.deleteById(diaryId);
    }

    // ---- helpers ----

    private void saveVisitImages(Integer visitId, List<Integer> fileIds, int now) {
        for (int i = 0; i < fileIds.size() && i < 3; i++) {
            LoRestaurantVisitImage img = new LoRestaurantVisitImage();
            img.setVisitId(visitId);
            img.setFileId(fileIds.get(i));
            img.setSort(i);
            img.setCreatedAt(now);
            visitImageMapper.insert(img);
        }
    }

    private void saveDiaryImages(Integer diaryId, List<Integer> fileIds, int now) {
        for (int i = 0; i < fileIds.size() && i < 3; i++) {
            LoDiaryImage img = new LoDiaryImage();
            img.setDiaryId(diaryId);
            img.setFileId(fileIds.get(i));
            img.setSort(i);
            img.setCreatedAt(now);
            diaryImageMapper.insert(img);
        }
    }

    private List<String> loadVisitImageUrls(Integer visitId) {
        List<LoRestaurantVisitImage> imgs = visitImageMapper.selectList(
            new LambdaQueryWrapper<LoRestaurantVisitImage>()
                .eq(LoRestaurantVisitImage::getVisitId, visitId)
                .orderByAsc(LoRestaurantVisitImage::getSort)
        );
        return imgs.stream().map(i -> "/api/order/file/" + i.getFileId() + "/thumbnail").collect(Collectors.toList());
    }

    private List<String> loadDiaryImageUrls(Integer diaryId) {
        List<LoDiaryImage> imgs = diaryImageMapper.selectList(
            new LambdaQueryWrapper<LoDiaryImage>()
                .eq(LoDiaryImage::getDiaryId, diaryId)
                .orderByAsc(LoDiaryImage::getSort)
        );
        return imgs.stream().map(i -> "/api/order/file/" + i.getFileId() + "/thumbnail").collect(Collectors.toList());
    }

    private RestaurantVisitDto toVisitDto(LoRestaurantVisit v) {
        RestaurantVisitDto d = new RestaurantVisitDto();
        d.setId(v.getId());
        d.setVisitDate(v.getVisitDate());
        d.setMealType(v.getMealType());
        d.setRestaurantName(v.getRestaurantName());
        d.setScore(v.getScore());
        d.setContent(v.getContent());
        return d;
    }

    // package-level helper for calendar query
    public boolean hasVisitOnDate(Integer userId, String date) {
        return visitMapper.selectCount(
            new LambdaQueryWrapper<LoRestaurantVisit>()
                .eq(LoRestaurantVisit::getUserId, userId)
                .eq(LoRestaurantVisit::getVisitDate, date)
        ) > 0;
    }

    public boolean hasDiaryOnDate(Integer userId, String date) {
        return diaryMapper.selectCount(
            new LambdaQueryWrapper<LoDiary>()
                .eq(LoDiary::getUserId, userId)
                .eq(LoDiary::getDiaryDate, date)
        ) > 0;
    }
}
