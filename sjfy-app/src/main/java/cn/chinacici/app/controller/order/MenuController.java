package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.CategoryRespDto;
import cn.chinacici.service.order.dto.DishRespDto;
import cn.chinacici.service.order.dto.MealTypeInfoDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.service.DishService;
import cn.chinacici.service.order.service.OrderService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MenuController {
    private final DishService dishService;
    private final OrderService orderService;
    private final UserOrderService userOrderService;

    public MenuController(DishService dishService,
                          OrderService orderService,
                          UserOrderService userOrderService) {
        this.dishService = dishService;
        this.orderService = orderService;
        this.userOrderService = userOrderService;
    }

    /** GET /api/order/menu/categories - 需要登录（多租户下菜单按租户隔离） */
    @GetMapping("/order/menu/categories")
    public ResponseData<List<CategoryRespDto>> getCategories(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(dishService.getCategoryList(session.getTenantId()));
    }

    /** GET /api/order/menu/dishes?categoryId= - 需要登录 */
    @GetMapping("/order/menu/dishes")
    public ResponseData<List<DishRespDto>> getDishes(
        @RequestParam(required = false) Integer categoryId,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(dishService.getDishList(categoryId, session.getTenantId()));
    }

    /** GET /api/order/meal-type - 需要登录 */
    @GetMapping("/order/meal-type")
    public ResponseData<MealTypeInfoDto> getMealType(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        return ResponseData.success(orderService.getMealTypeInfo(userId));
    }
}
