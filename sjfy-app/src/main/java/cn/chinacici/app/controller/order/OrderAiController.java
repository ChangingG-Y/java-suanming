package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.AiConfigDto;
import cn.chinacici.service.order.dto.CalorieAdviceReqDto;
import cn.chinacici.service.order.dto.CalorieAdviceRespDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.service.AiConfigService;
import cn.chinacici.service.order.service.OrderAiService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderAiController {
    private final OrderAiService orderAiService;
    private final AiConfigService aiConfigService;
    private final UserOrderService userOrderService;

    public OrderAiController(OrderAiService orderAiService, AiConfigService aiConfigService, UserOrderService userOrderService) {
        this.orderAiService = orderAiService;
        this.aiConfigService = aiConfigService;
        this.userOrderService = userOrderService;
    }

    @PostMapping("/ai/calorie")
    public ResponseData<CalorieAdviceRespDto> getCalorieAdvice(
        @RequestBody CalorieAdviceReqDto req,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(orderAiService.getCalorieAdvice(req, session.getTenantId()));
    }

    @GetMapping("/admin/ai-config")
    public ResponseData<AiConfigDto> getAiConfig(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        return ResponseData.success(aiConfigService.getAiConfig(admin.getTenantId()));
    }

    @PutMapping("/admin/ai-config")
    public ResponseData<Void> updateAiConfig(
        @RequestBody AiConfigDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        aiConfigService.updateAiConfig(admin.getTenantId(), dto);
        return ResponseData.success();
    }

    @PostMapping("/admin/ai/generate-description")
    public ResponseData<String> generateDishDescription(
        @RequestBody Map<String, String> body,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        String name = body.get("name");
        String desc = orderAiService.generateDishDescription(name, admin.getTenantId());
        return ResponseData.success(desc);
    }
}
