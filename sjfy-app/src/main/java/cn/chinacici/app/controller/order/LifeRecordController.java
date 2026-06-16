package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.DiaryDto;
import cn.chinacici.service.order.dto.RestaurantVisitDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.service.LifeRecordService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order/life")
public class LifeRecordController {
    private final LifeRecordService lifeRecordService;
    private final UserOrderService userOrderService;

    public LifeRecordController(LifeRecordService lifeRecordService, UserOrderService userOrderService) {
        this.lifeRecordService = lifeRecordService;
        this.userOrderService = userOrderService;
    }

    // ---- 下馆子 ----

    @PostMapping("/visit")
    public ResponseData<RestaurantVisitDto> saveVisit(@RequestHeader("Authorization") String auth,
                                                      @RequestBody RestaurantVisitDto dto) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(lifeRecordService.saveVisit(session.getUserId(), session.getTenantId(), dto));
    }

    @GetMapping("/visit")
    public ResponseData<java.util.List<RestaurantVisitDto>> getVisitsByDate(
            @RequestHeader("Authorization") String auth,
            @RequestParam String date) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(lifeRecordService.getVisitsByDate(session.getUserId(), date));
    }

    @DeleteMapping("/visit/{id}")
    public ResponseData<Void> deleteVisit(@RequestHeader("Authorization") String auth,
                                          @PathVariable Integer id) {
        SessionDto session = userOrderService.getSession(auth);
        lifeRecordService.deleteVisit(session.getUserId(), id);
        return ResponseData.success();
    }

    // ---- 日记 ----

    @PostMapping("/diary")
    public ResponseData<DiaryDto> saveDiary(@RequestHeader("Authorization") String auth,
                                            @RequestBody DiaryDto dto) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(lifeRecordService.saveDiary(session.getUserId(), session.getTenantId(), dto));
    }

    @GetMapping("/diary")
    public ResponseData<DiaryDto> getDiaryByDate(@RequestHeader("Authorization") String auth,
                                                 @RequestParam String date) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(lifeRecordService.getDiaryByDate(session.getUserId(), date));
    }

    @DeleteMapping("/diary/{id}")
    public ResponseData<Void> deleteDiary(@RequestHeader("Authorization") String auth,
                                          @PathVariable Integer id) {
        SessionDto session = userOrderService.getSession(auth);
        lifeRecordService.deleteDiary(session.getUserId(), id);
        return ResponseData.success();
    }
}
