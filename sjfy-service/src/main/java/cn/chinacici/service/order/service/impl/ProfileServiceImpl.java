package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.CalendarDayDto;
import cn.chinacici.service.order.dto.DiaryDto;
import cn.chinacici.service.order.dto.ProfileRespDto;
import cn.chinacici.service.order.dto.ProfileUpdateDto;
import cn.chinacici.service.order.dto.RestaurantVisitDto;
import cn.chinacici.service.order.dto.WeightRecordDto;
import cn.chinacici.service.order.entity.LoDish;
import cn.chinacici.service.order.entity.LoFile;
import cn.chinacici.service.order.entity.LoOrder;
import cn.chinacici.service.order.entity.LoOrderItem;
import cn.chinacici.service.order.entity.LoReview;
import cn.chinacici.service.order.entity.LoReviewImageRela;
import cn.chinacici.service.order.entity.LoUser;
import cn.chinacici.service.order.entity.LoUserProfile;
import cn.chinacici.service.order.entity.LoWeightRecord;
import cn.chinacici.service.order.mapper.LoDishMapper;
import cn.chinacici.service.order.mapper.LoFileMapper;
import cn.chinacici.service.order.mapper.LoOrderItemMapper;
import cn.chinacici.service.order.mapper.LoOrderMapper;
import cn.chinacici.service.order.mapper.LoReviewImageRelaMapper;
import cn.chinacici.service.order.mapper.LoReviewMapper;
import cn.chinacici.service.order.mapper.LoUserMapper;
import cn.chinacici.service.order.mapper.LoUserProfileMapper;
import cn.chinacici.service.order.mapper.LoWeightRecordMapper;
import cn.chinacici.service.order.entity.LoDiary;
import cn.chinacici.service.order.entity.LoRestaurantVisit;
import cn.chinacici.service.order.mapper.LoDiaryMapper;
import cn.chinacici.service.order.mapper.LoRestaurantVisitMapper;
import cn.chinacici.service.order.service.LifeRecordService;
import cn.chinacici.service.order.service.ProfileService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {
    private final LoUserMapper userMapper;
    private final LoUserProfileMapper profileMapper;
    private final LoWeightRecordMapper weightMapper;
    private final LoOrderMapper orderMapper;
    private final LoOrderItemMapper orderItemMapper;
    private final LoDishMapper dishMapper;
    private final LoReviewMapper reviewMapper;
    private final LoReviewImageRelaMapper reviewImageRelaMapper;
    private final LoFileMapper fileMapper;
    private final LoRestaurantVisitMapper visitMapper;
    private final LoDiaryMapper diaryMapper;
    private final LifeRecordService lifeRecordService;

    public ProfileServiceImpl(LoUserMapper userMapper,
                              LoUserProfileMapper profileMapper,
                              LoWeightRecordMapper weightMapper,
                              LoOrderMapper orderMapper,
                              LoOrderItemMapper orderItemMapper,
                              LoDishMapper dishMapper,
                              LoReviewMapper reviewMapper,
                              LoReviewImageRelaMapper reviewImageRelaMapper,
                              LoFileMapper fileMapper,
                              LoRestaurantVisitMapper visitMapper,
                              LoDiaryMapper diaryMapper,
                              LifeRecordService lifeRecordService) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.weightMapper = weightMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.dishMapper = dishMapper;
        this.reviewMapper = reviewMapper;
        this.reviewImageRelaMapper = reviewImageRelaMapper;
        this.fileMapper = fileMapper;
        this.visitMapper = visitMapper;
        this.diaryMapper = diaryMapper;
        this.lifeRecordService = lifeRecordService;
    }

    @Override
    public ProfileRespDto getProfile(Integer userId) {
        LoUser user = userMapper.selectById(userId);
        if (user == null) throw new ServiceException(ResultCode.PARAMETER_ERROR, "用户不存在");

        LoUserProfile profile = profileMapper.selectOne(
            new LambdaQueryWrapper<LoUserProfile>().eq(LoUserProfile::getUserId, userId)
        );

        LoWeightRecord latestWeight = weightMapper.selectOne(
            new LambdaQueryWrapper<LoWeightRecord>()
                .eq(LoWeightRecord::getUserId, userId)
                .orderByDesc(LoWeightRecord::getRecordDate)
                .last("LIMIT 1")
        );

        ProfileRespDto dto = new ProfileRespDto();
        dto.setUserId(userId);
        dto.setNickname(user.getNickname());
        if (user.getAvatarFileId() != null) {
            dto.setAvatarUrl("/api/order/file/" + user.getAvatarFileId() + "/thumbnail");
        }
        if (user.getBannerFileId() != null) {
            dto.setBannerUrl("/api/order/file/" + user.getBannerFileId() + "/thumbnail");
        }
        if (profile != null) {
            dto.setHeight(profile.getHeight());
            dto.setBio(profile.getBio());
            dto.setBirthday(profile.getBirthday());
        }
        if (latestWeight != null) {
            dto.setCurrentWeight(latestWeight.getWeight());
            dto.setCurrentWeightDate(latestWeight.getRecordDate());
        }
        return dto;
    }

    @Override
    public void updateProfile(Integer userId, ProfileUpdateDto dto) {
        int now = (int) (System.currentTimeMillis() / 1000);
        LoUser user = userMapper.selectById(userId);
        if (user == null) throw new ServiceException(ResultCode.PARAMETER_ERROR, "用户不存在");

        if (StringUtils.hasText(dto.getNickname())) {
            LambdaUpdateWrapper<LoUser> uw = new LambdaUpdateWrapper<LoUser>()
                .eq(LoUser::getId, userId)
                .set(LoUser::getNickname, dto.getNickname())
                .set(LoUser::getUpdatedAt, now);
            userMapper.update(null, uw);
        }

        LoUserProfile profile = profileMapper.selectOne(
            new LambdaQueryWrapper<LoUserProfile>().eq(LoUserProfile::getUserId, userId)
        );
        if (profile == null) {
            profile = new LoUserProfile();
            profile.setUserId(userId);
            profile.setTenantId(user.getTenantId() != null ? user.getTenantId() : 1);
            profile.setCreatedAt(now);
            profile.setUpdatedAt(now);
            if (dto.getHeight() != null) profile.setHeight(dto.getHeight());
            if (dto.getBio() != null) profile.setBio(dto.getBio());
            if (dto.getBirthday() != null) profile.setBirthday(dto.getBirthday());
            profileMapper.insert(profile);
        } else {
            LambdaUpdateWrapper<LoUserProfile> uw = new LambdaUpdateWrapper<LoUserProfile>()
                .eq(LoUserProfile::getUserId, userId)
                .set(LoUserProfile::getUpdatedAt, now);
            if (dto.getHeight() != null) uw.set(LoUserProfile::getHeight, dto.getHeight());
            if (dto.getBio() != null) uw.set(LoUserProfile::getBio, dto.getBio());
            if (dto.getBirthday() != null) uw.set(LoUserProfile::getBirthday, dto.getBirthday());
            profileMapper.update(null, uw);
        }
    }

    @Override
    public void updateAvatar(Integer userId, Integer fileId) {
        int now = (int) (System.currentTimeMillis() / 1000);
        LambdaUpdateWrapper<LoUser> uw = new LambdaUpdateWrapper<LoUser>()
            .eq(LoUser::getId, userId)
            .set(LoUser::getAvatarFileId, fileId)
            .set(LoUser::getUpdatedAt, now);
        userMapper.update(null, uw);
    }

    @Override
    public void updateBanner(Integer userId, Integer fileId) {
        int now = (int) (System.currentTimeMillis() / 1000);
        LambdaUpdateWrapper<LoUser> uw = new LambdaUpdateWrapper<LoUser>()
            .eq(LoUser::getId, userId)
            .set(LoUser::getBannerFileId, fileId)
            .set(LoUser::getUpdatedAt, now);
        userMapper.update(null, uw);
    }

    @Override
    public List<WeightRecordDto> getWeightRecords(Integer userId, int days) {
        String fromDate = LocalDate.now().minusDays(days - 1).toString();
        List<LoWeightRecord> records = weightMapper.selectList(
            new LambdaQueryWrapper<LoWeightRecord>()
                .eq(LoWeightRecord::getUserId, userId)
                .ge(LoWeightRecord::getRecordDate, fromDate)
                .orderByAsc(LoWeightRecord::getRecordDate)
        );
        return records.stream().map(r -> {
            WeightRecordDto d = new WeightRecordDto();
            d.setId(r.getId());
            d.setRecordDate(r.getRecordDate());
            d.setWeight(r.getWeight());
            d.setNote(r.getNote());
            return d;
        }).collect(Collectors.toList());
    }

    @Override
    public void saveWeightRecord(Integer userId, Integer tenantId, WeightRecordDto dto) {
        if (dto.getRecordDate() == null || dto.getWeight() == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "日期和体重不能为空");
        }
        int now = (int) (System.currentTimeMillis() / 1000);
        LoWeightRecord existing = weightMapper.selectOne(
            new LambdaQueryWrapper<LoWeightRecord>()
                .eq(LoWeightRecord::getUserId, userId)
                .eq(LoWeightRecord::getRecordDate, dto.getRecordDate())
        );
        if (existing == null) {
            LoWeightRecord r = new LoWeightRecord();
            r.setUserId(userId);
            r.setTenantId(tenantId);
            r.setRecordDate(dto.getRecordDate());
            r.setWeight(dto.getWeight());
            r.setNote(dto.getNote());
            r.setCreatedAt(now);
            r.setUpdatedAt(now);
            weightMapper.insert(r);
        } else {
            LambdaUpdateWrapper<LoWeightRecord> uw = new LambdaUpdateWrapper<LoWeightRecord>()
                .eq(LoWeightRecord::getId, existing.getId())
                .set(LoWeightRecord::getWeight, dto.getWeight())
                .set(LoWeightRecord::getNote, dto.getNote())
                .set(LoWeightRecord::getUpdatedAt, now);
            weightMapper.update(null, uw);
        }
    }

    @Override
    public void deleteWeightRecord(Integer userId, Integer recordId) {
        LoWeightRecord r = weightMapper.selectById(recordId);
        if (r == null || !r.getUserId().equals(userId)) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "记录不存在");
        }
        weightMapper.deleteById(recordId);
    }

    @Override
    public List<CalendarDayDto> getCalendar(Integer userId, Integer tenantId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        String fromDate = ym.atDay(1).toString();
        String toDate = ym.atEndOfMonth().toString();

        // 情侣共享日历：同一租户所有用户的打卡/日记/下馆子标记合并显示，让双方看到同样的日历
        List<Integer> tenantUserIds = userMapper.selectList(
            new LambdaQueryWrapper<LoUser>()
                .eq(LoUser::getTenantId, tenantId)
                .eq(LoUser::getIsDeleted, 0)
                .select(LoUser::getId)
        ).stream().map(LoUser::getId).collect(Collectors.toList());
        if (tenantUserIds.isEmpty()) tenantUserIds = List.of(userId);

        // 食堂订单：租户内所有用户的订单（显示是否有人做饭/点饭）
        List<LoOrder> orders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getTenantId, tenantId)
                .in(LoOrder::getUserId, tenantUserIds)
                .eq(LoOrder::getIsDeleted, 0)
                .ge(LoOrder::getOrderDate, LocalDate.parse(fromDate))
                .le(LoOrder::getOrderDate, LocalDate.parse(toDate))
        );
        Set<String> orderDates = orders.stream()
            .map(o -> o.getOrderDate().toString())
            .collect(Collectors.toSet());

        // 体重：仍然只显示请求者自己的体重（个人指标）
        List<LoWeightRecord> weights = weightMapper.selectList(
            new LambdaQueryWrapper<LoWeightRecord>()
                .eq(LoWeightRecord::getUserId, userId)
                .ge(LoWeightRecord::getRecordDate, fromDate)
                .le(LoWeightRecord::getRecordDate, toDate)
        );
        Map<String, BigDecimal> weightMap = new HashMap<>();
        for (LoWeightRecord w : weights) {
            weightMap.put(w.getRecordDate(), w.getWeight());
        }

        // 下馆子：租户内任意用户有记录即标记
        List<LoRestaurantVisit> visits = visitMapper.selectList(
            new LambdaQueryWrapper<LoRestaurantVisit>()
                .in(LoRestaurantVisit::getUserId, tenantUserIds)
                .ge(LoRestaurantVisit::getVisitDate, fromDate)
                .le(LoRestaurantVisit::getVisitDate, toDate)
                .select(LoRestaurantVisit::getVisitDate)
        );
        Set<String> visitDates = visits.stream().map(LoRestaurantVisit::getVisitDate).collect(Collectors.toSet());

        // 日记：租户内任意用户有日记即标记
        List<LoDiary> diaries = diaryMapper.selectList(
            new LambdaQueryWrapper<LoDiary>()
                .in(LoDiary::getUserId, tenantUserIds)
                .ge(LoDiary::getDiaryDate, fromDate)
                .le(LoDiary::getDiaryDate, toDate)
                .select(LoDiary::getDiaryDate)
        );
        Set<String> diaryDates = diaries.stream().map(LoDiary::getDiaryDate).collect(Collectors.toSet());

        List<CalendarDayDto> result = new ArrayList<>();
        int days = ym.lengthOfMonth();
        for (int d = 1; d <= days; d++) {
            String dateStr = ym.atDay(d).toString();
            CalendarDayDto day = new CalendarDayDto();
            day.setDate(dateStr);
            day.setHasOrder(orderDates.contains(dateStr));
            day.setWeight(weightMap.get(dateStr));
            day.setHasVisit(visitDates.contains(dateStr));
            day.setHasDiary(diaryDates.contains(dateStr));
            result.add(day);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDayDetail(Integer userId, Integer tenantId, String date) {
        LocalDate localDate = LocalDate.parse(date);

        // 获取租户内所有用户ID，订单以整个租户为维度（情侣看到同样的做饭记录）
        List<Integer> tenantUserIds = userMapper.selectList(
            new LambdaQueryWrapper<LoUser>()
                .eq(LoUser::getTenantId, tenantId)
                .eq(LoUser::getIsDeleted, 0)
                .select(LoUser::getId)
        ).stream().map(LoUser::getId).collect(Collectors.toList());
        if (tenantUserIds.isEmpty()) tenantUserIds = List.of(userId);

        // 订单：整个租户当天所有订单（双方共享，谁做饭都算）
        List<LoOrder> orders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getTenantId, tenantId)
                .in(LoOrder::getUserId, tenantUserIds)
                .eq(LoOrder::getIsDeleted, 0)
                .eq(LoOrder::getOrderDate, localDate)
                .orderByAsc(LoOrder::getMealType)
        );

        List<Map<String, Object>> orderList = new ArrayList<>();
        for (LoOrder order : orders) {
            Map<String, Object> om = new LinkedHashMap<>();
            om.put("id", order.getId());
            om.put("mealType", order.getMealType());
            om.put("mealTypeName", getMealTypeName(order.getMealType()));
            om.put("state", order.getState());
            om.put("remark", order.getRemark());

            List<LoOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<LoOrderItem>().eq(LoOrderItem::getOrderId, order.getId())
            );
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (LoOrderItem item : items) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("dishName", item.getDishName());
                im.put("qty", item.getQuantity());
                LoDish dish = dishMapper.selectById(item.getDishId());
                if (dish != null && dish.getImageFileId() != null) {
                    im.put("dishThumbUrl", "/api/order/file/" + dish.getImageFileId() + "/thumbnail");
                }
                itemList.add(im);
            }
            om.put("items", itemList);

            LoReview review = reviewMapper.selectOne(
                new LambdaQueryWrapper<LoReview>().eq(LoReview::getOrderId, order.getId())
            );
            if (review != null) {
                Map<String, Object> rv = new LinkedHashMap<>();
                rv.put("score", review.getScore());
                rv.put("content", review.getContent());
                List<LoReviewImageRela> imgRelas = reviewImageRelaMapper.selectList(
                    new LambdaQueryWrapper<LoReviewImageRela>().eq(LoReviewImageRela::getReviewId, review.getId())
                );
                List<String> imgs = new ArrayList<>();
                for (LoReviewImageRela rel : imgRelas) {
                    imgs.add("/api/order/file/" + rel.getFileId() + "/thumbnail");
                }
                rv.put("images", imgs);
                om.put("review", rv);
            }
            orderList.add(om);
        }

        // 体重：请求者自己的（个人数据，各自独立）
        LoWeightRecord wr = weightMapper.selectOne(
            new LambdaQueryWrapper<LoWeightRecord>()
                .eq(LoWeightRecord::getUserId, userId)
                .eq(LoWeightRecord::getRecordDate, date)
        );

        // 获取自己和伴侣信息
        LoUser selfUser = userMapper.selectById(userId);
        String myNickname = selfUser != null && selfUser.getNickname() != null ? selfUser.getNickname() : "我";

        Integer partnerId = getPartnerId(userId, tenantId);
        String partnerNickname = null;
        Object partnerWeightData = null;

        // 下馆子打卡：合并自己和伴侣的记录，各带 isOwn 标记
        List<Map<String, Object>> allVisits = new ArrayList<>();
        for (RestaurantVisitDto v : lifeRecordService.getVisitsByDate(userId, date)) {
            Map<String, Object> vm = buildVisitMap(v);
            vm.put("isOwn", true);
            vm.put("authorNickname", myNickname);
            allVisits.add(vm);
        }

        // 日记：合并自己和伴侣的，各带 isOwn 标记（数组，支持双方各自写）
        List<Map<String, Object>> allDiaries = new ArrayList<>();
        DiaryDto myDiary = lifeRecordService.getDiaryByDate(userId, date);
        if (myDiary != null) {
            Map<String, Object> dm = buildDiaryMap(myDiary);
            dm.put("isOwn", true);
            dm.put("authorNickname", myNickname);
            allDiaries.add(dm);
        }

        if (partnerId != null) {
            LoUser partnerUser = userMapper.selectById(partnerId);
            partnerNickname = partnerUser != null && partnerUser.getNickname() != null ? partnerUser.getNickname() : "TA";

            // 伴侣的下馆子打卡追加到列表末尾
            for (RestaurantVisitDto v : lifeRecordService.getVisitsByDate(partnerId, date)) {
                Map<String, Object> vm = buildVisitMap(v);
                vm.put("isOwn", false);
                vm.put("authorNickname", partnerNickname);
                allVisits.add(vm);
            }

            // 伴侣的日记追加到列表末尾
            DiaryDto partnerDiary = lifeRecordService.getDiaryByDate(partnerId, date);
            if (partnerDiary != null) {
                Map<String, Object> dm = buildDiaryMap(partnerDiary);
                dm.put("isOwn", false);
                dm.put("authorNickname", partnerNickname);
                allDiaries.add(dm);
            }

            // 伴侣体重（用于并排展示）
            LoWeightRecord partnerWr = weightMapper.selectOne(
                new LambdaQueryWrapper<LoWeightRecord>()
                    .eq(LoWeightRecord::getUserId, partnerId)
                    .eq(LoWeightRecord::getRecordDate, date)
            );
            partnerWeightData = partnerWr != null ? partnerWr.getWeight() : null;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date);
        result.put("orders", orderList);
        // 下馆子和日记以合并列表返回，前端用 isOwn 区分操作权限
        result.put("allVisits", allVisits);
        result.put("allDiaries", allDiaries);
        // 体重：自己的（weight）+ 伴侣的（partnerWeight），并排展示
        result.put("weight", wr != null ? wr.getWeight() : null);
        result.put("weightNote", wr != null ? wr.getNote() : null);
        result.put("myNickname", myNickname);
        result.put("partnerNickname", partnerNickname);
        result.put("partnerWeight", partnerWeightData);
        return result;
    }

    /** 构建下馆子打卡 Map（不含 isOwn/authorNickname，由调用方追加） */
    private Map<String, Object> buildVisitMap(RestaurantVisitDto v) {
        Map<String, Object> vm = new LinkedHashMap<>();
        vm.put("id", v.getId());
        vm.put("mealType", v.getMealType());
        vm.put("mealTypeName", getMealTypeName(v.getMealType()));
        vm.put("restaurantName", v.getRestaurantName());
        vm.put("score", v.getScore());
        vm.put("content", v.getContent());
        vm.put("imageUrls", v.getImageUrls() != null ? v.getImageUrls() : List.of());
        return vm;
    }

    /** 构建日记 Map（不含 isOwn/authorNickname，由调用方追加） */
    private Map<String, Object> buildDiaryMap(DiaryDto d) {
        Map<String, Object> dm = new LinkedHashMap<>();
        dm.put("id", d.getId());
        dm.put("content", d.getContent());
        dm.put("imageUrls", d.getImageUrls() != null ? d.getImageUrls() : List.of());
        return dm;
    }

    @Override
    public Integer getPartnerId(Integer userId, Integer tenantId) {
        LoUser self = userMapper.selectById(userId);
        if (self == null) return null;
        List<LoUser> others = userMapper.selectList(
            new LambdaQueryWrapper<LoUser>()
                .eq(LoUser::getTenantId, tenantId)
                .ne(LoUser::getId, userId)
                .eq(LoUser::getIsDeleted, 0)
        );
        return others.isEmpty() ? null : others.get(0).getId();
    }

    private String getMealTypeName(Integer mealType) {
        if (mealType == null) return "未知";
        switch (mealType) {
            case 0: return "早饭";
            case 1: return "午饭";
            case 2: return "晚饭";
            default: return "其他";
        }
    }
}
