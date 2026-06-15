package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.AiConfigDto;
import cn.chinacici.service.order.dto.CalorieAdviceReqDto;
import cn.chinacici.service.order.dto.CalorieAdviceRespDto;
import cn.chinacici.service.order.service.AiConfigService;
import cn.chinacici.service.order.service.OrderAiService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.*;

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

    /**
     * POST /api/order/ai/calorie
     * 客户端在结算页调用：传入购物车菜品列表，返回 AI 热量估算和用餐建议。
     * 需要登录。
     */
    @PostMapping("/ai/calorie")
    public ResponseData<CalorieAdviceRespDto> getCalorieAdvice(
        @RequestBody CalorieAdviceReqDto req,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireUserId(auth);
        return ResponseData.success(orderAiService.getCalorieAdvice(req));
    }

    /**
     * GET /api/order/admin/ai-config
     * 管理员查看当前 AI 配置（provider/model/prompt/enabled）。
     */
    @GetMapping("/admin/ai-config")
    public ResponseData<AiConfigDto> getAiConfig(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        return ResponseData.success(aiConfigService.getAiConfig());
    }

    /**
     * PUT /api/order/admin/ai-config
     * 管理员更新 AI 配置（存储到数据库，重启不丢失）。
     */
    @PutMapping("/admin/ai-config")
    public ResponseData<Void> updateAiConfig(
        @RequestBody AiConfigDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        aiConfigService.updateAiConfig(dto);
        return ResponseData.success();
    }
}
