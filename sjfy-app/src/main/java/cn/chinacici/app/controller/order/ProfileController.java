package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.CalendarDayDto;
import cn.chinacici.service.order.dto.ProfileRespDto;
import cn.chinacici.service.order.dto.ProfileUpdateDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.dto.WeightRecordDto;
import cn.chinacici.service.order.service.FileOrderService;
import cn.chinacici.service.order.service.ProfileService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final UserOrderService userOrderService;
    private final FileOrderService fileOrderService;

    public ProfileController(ProfileService profileService,
                             UserOrderService userOrderService,
                             FileOrderService fileOrderService) {
        this.profileService = profileService;
        this.userOrderService = userOrderService;
        this.fileOrderService = fileOrderService;
    }

    @GetMapping("/me")
    public ResponseData<ProfileRespDto> getMyProfile(@RequestHeader("Authorization") String auth) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(profileService.getProfile(session.getUserId()));
    }

    @PutMapping("/me")
    public ResponseData<Void> updateMyProfile(@RequestHeader("Authorization") String auth,
                                              @RequestBody ProfileUpdateDto dto) {
        SessionDto session = userOrderService.getSession(auth);
        profileService.updateProfile(session.getUserId(), dto);
        return ResponseData.success();
    }

    @PostMapping("/avatar")
    public ResponseData<String> uploadAvatar(@RequestHeader("Authorization") String auth,
                                             @RequestParam("file") MultipartFile file) {
        SessionDto session = userOrderService.getSession(auth);
        var resp = fileOrderService.upload(file, session.getUserId(), "dish");
        profileService.updateAvatar(session.getUserId(), resp.getId());
        return ResponseData.success((Object) resp.getThumbnailUrl());
    }

    @PostMapping("/banner")
    public ResponseData<String> uploadBanner(@RequestHeader("Authorization") String auth,
                                             @RequestParam("file") MultipartFile file) {
        SessionDto session = userOrderService.getSession(auth);
        var resp = fileOrderService.upload(file, session.getUserId(), "dish");
        profileService.updateBanner(session.getUserId(), resp.getId());
        return ResponseData.success((Object) resp.getThumbnailUrl());
    }

    @GetMapping("/weight")
    public ResponseData<List<WeightRecordDto>> getWeightRecords(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "90") int days) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(profileService.getWeightRecords(session.getUserId(), days));
    }

    @PostMapping("/weight")
    public ResponseData<Void> saveWeight(@RequestHeader("Authorization") String auth,
                                         @RequestBody WeightRecordDto dto) {
        SessionDto session = userOrderService.getSession(auth);
        profileService.saveWeightRecord(session.getUserId(), session.getTenantId(), dto);
        return ResponseData.success();
    }

    @DeleteMapping("/weight/{id}")
    public ResponseData<Void> deleteWeight(@RequestHeader("Authorization") String auth,
                                           @PathVariable Integer id) {
        SessionDto session = userOrderService.getSession(auth);
        profileService.deleteWeightRecord(session.getUserId(), id);
        return ResponseData.success();
    }

    @GetMapping("/calendar")
    public ResponseData<List<CalendarDayDto>> getCalendar(
            @RequestHeader("Authorization") String auth,
            @RequestParam int year,
            @RequestParam int month) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(
            profileService.getCalendar(session.getUserId(), session.getTenantId(), year, month)
        );
    }

    @GetMapping("/day")
    public ResponseData<Map<String, Object>> getDayDetail(
            @RequestHeader("Authorization") String auth,
            @RequestParam String date) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(
            profileService.getDayDetail(session.getUserId(), session.getTenantId(), date)
        );
    }

    // ===== Both admin and regular user can view partner's data =====
    // In this couple app, any authenticated user can peek at their partner

    @GetMapping("/partner")
    public ResponseData<ProfileRespDto> getPartnerProfile(@RequestHeader("Authorization") String auth) {
        SessionDto session = userOrderService.getSession(auth);
        Integer partnerId = profileService.getPartnerId(session.getUserId(), session.getTenantId());
        if (partnerId == null) return ResponseData.success(null);
        return ResponseData.success(profileService.getProfile(partnerId));
    }

    @GetMapping("/partner/weight")
    public ResponseData<List<WeightRecordDto>> getPartnerWeight(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "90") int days) {
        SessionDto session = userOrderService.getSession(auth);
        Integer partnerId = profileService.getPartnerId(session.getUserId(), session.getTenantId());
        if (partnerId == null) return ResponseData.success(List.of());
        return ResponseData.success(profileService.getWeightRecords(partnerId, days));
    }

    @GetMapping("/partner/calendar")
    public ResponseData<List<CalendarDayDto>> getPartnerCalendar(
            @RequestHeader("Authorization") String auth,
            @RequestParam int year,
            @RequestParam int month) {
        SessionDto session = userOrderService.getSession(auth);
        Integer partnerId = profileService.getPartnerId(session.getUserId(), session.getTenantId());
        if (partnerId == null) return ResponseData.success(List.of());
        return ResponseData.success(
            profileService.getCalendar(partnerId, session.getTenantId(), year, month)
        );
    }

    @GetMapping("/partner/day")
    public ResponseData<Map<String, Object>> getPartnerDayDetail(
            @RequestHeader("Authorization") String auth,
            @RequestParam String date) {
        SessionDto session = userOrderService.getSession(auth);
        Integer partnerId = profileService.getPartnerId(session.getUserId(), session.getTenantId());
        if (partnerId == null) return ResponseData.success(Map.of());
        return ResponseData.success(
            profileService.getDayDetail(partnerId, session.getTenantId(), date)
        );
    }
}
