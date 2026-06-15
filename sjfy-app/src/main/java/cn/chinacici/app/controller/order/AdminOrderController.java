package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.CategoryRespDto;
import cn.chinacici.service.order.dto.DishRespDto;
import cn.chinacici.service.order.dto.OrderRespDto;
import cn.chinacici.service.order.dto.SaveCategoryReqDto;
import cn.chinacici.service.order.dto.SaveDishReqDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.service.DishService;
import cn.chinacici.service.order.service.OrderService;
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

import java.util.List;

@RestController
@RequestMapping("/order/admin")
public class AdminOrderController {
    private final OrderService orderService;
    private final DishService dishService;
    private final UserOrderService userOrderService;

    public AdminOrderController(OrderService orderService,
                                DishService dishService,
                                UserOrderService userOrderService) {
        this.orderService = orderService;
        this.dishService = dishService;
        this.userOrderService = userOrderService;
    }

    /** GET /api/order/admin/orders?state= - 获取订单列表（需要管理员） */
    @GetMapping("/orders")
    public ResponseData<List<OrderRespDto>> getOrders(
        @RequestParam(required = false) Integer state,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        return ResponseData.success(orderService.getAdminOrders(state));
    }

    /** POST /api/order/admin/orders/{id}/accept - 接单（需要管理员） */
    @PostMapping("/orders/{id}/accept")
    public ResponseData<Void> acceptOrder(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        orderService.acceptOrder(id);
        return ResponseData.success();
    }

    /** POST /api/order/admin/orders/{id}/serve - 确认出餐（需要管理员） */
    @PostMapping("/orders/{id}/serve")
    public ResponseData<Void> serveOrder(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        orderService.serveOrder(id);
        return ResponseData.success();
    }

    /** GET /api/order/admin/categories - 分类列表（需要管理员） */
    @GetMapping("/categories")
    public ResponseData<List<CategoryRespDto>> getCategories(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        return ResponseData.success(dishService.getAdminCategoryList());
    }

    /** POST /api/order/admin/categories - 新增分类 */
    @PostMapping("/categories")
    public ResponseData<CategoryRespDto> saveCategory(
        @RequestBody SaveCategoryReqDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        return ResponseData.success(dishService.saveCategory(dto, admin.getUserId()));
    }

    /** PUT /api/order/admin/categories/{id} - 修改分类 */
    @PutMapping("/categories/{id}")
    public ResponseData<Void> updateCategory(
        @PathVariable Integer id,
        @RequestBody SaveCategoryReqDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        dishService.updateCategory(id, dto, admin.getUserId());
        return ResponseData.success();
    }

    /** DELETE /api/order/admin/categories/{id} - 删除分类 */
    @DeleteMapping("/categories/{id}")
    public ResponseData<Void> deleteCategory(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        dishService.deleteCategory(id);
        return ResponseData.success();
    }

    /** GET /api/order/admin/dishes?categoryId= - 菜品列表 */
    @GetMapping("/dishes")
    public ResponseData<List<DishRespDto>> getDishes(
        @RequestParam(required = false) Integer categoryId,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        return ResponseData.success(dishService.getAdminDishList(categoryId));
    }

    /** POST /api/order/admin/dishes - 新增菜品 */
    @PostMapping("/dishes")
    public ResponseData<DishRespDto> saveDish(
        @RequestBody SaveDishReqDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        return ResponseData.success(dishService.saveDish(dto, admin.getUserId()));
    }

    /** PUT /api/order/admin/dishes/{id} - 修改菜品 */
    @PutMapping("/dishes/{id}")
    public ResponseData<Void> updateDish(
        @PathVariable Integer id,
        @RequestBody SaveDishReqDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto admin = userOrderService.requireAdmin(auth);
        dishService.updateDish(id, dto, admin.getUserId());
        return ResponseData.success();
    }

    /** DELETE /api/order/admin/dishes/{id} - 删除菜品 */
    @DeleteMapping("/dishes/{id}")
    public ResponseData<Void> deleteDish(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        userOrderService.requireAdmin(auth);
        dishService.deleteDish(id);
        return ResponseData.success();
    }
}
