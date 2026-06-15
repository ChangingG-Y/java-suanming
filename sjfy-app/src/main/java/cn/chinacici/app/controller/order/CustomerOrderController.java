package cn.chinacici.app.controller.order;

import cn.chinacici.core.ResponseData;
import cn.chinacici.service.order.dto.CreateOrderReqDto;
import cn.chinacici.service.order.dto.CreateReviewReqDto;
import cn.chinacici.service.order.dto.DayOrderSummaryDto;
import cn.chinacici.service.order.dto.OrderRespDto;
import cn.chinacici.service.order.dto.ReviewRespDto;
import cn.chinacici.service.order.dto.SessionDto;
import cn.chinacici.service.order.service.OrderService;
import cn.chinacici.service.order.service.ReviewService;
import cn.chinacici.service.order.service.UserOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CustomerOrderController {
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final UserOrderService userOrderService;

    public CustomerOrderController(OrderService orderService,
                                   ReviewService reviewService,
                                   UserOrderService userOrderService) {
        this.orderService = orderService;
        this.reviewService = reviewService;
        this.userOrderService = userOrderService;
    }

    /** POST /api/order/orders - 创建订单（需要登录） */
    @PostMapping("/order/orders")
    public ResponseData<OrderRespDto> createOrder(
        @RequestBody CreateOrderReqDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        SessionDto session = userOrderService.getSession(auth);
        return ResponseData.success(orderService.createOrder(dto, session.getUserId(), session.getTenantId()));
    }

    /** GET /api/order/orders/my - 我的订单列表（需要登录） */
    @GetMapping("/order/orders/my")
    public ResponseData<List<OrderRespDto>> getMyOrders(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        return ResponseData.success(orderService.getMyOrders(userId));
    }

    /** GET /api/order/orders/{id} - 订单详情（需要登录） */
    @GetMapping("/order/orders/{id}")
    public ResponseData<OrderRespDto> getOrderById(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        return ResponseData.success(orderService.getOrderById(id, userId));
    }

    /** POST /api/order/orders/{id}/complete - 完成吃饭（需要登录） */
    @PostMapping("/order/orders/{id}/complete")
    public ResponseData<Void> completeOrder(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        orderService.completeOrder(id, userId);
        return ResponseData.success();
    }

    /** POST /api/order/reviews - 提交评价（需要登录） */
    @PostMapping("/order/reviews")
    public ResponseData<ReviewRespDto> createReview(
        @RequestBody CreateReviewReqDto dto,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        return ResponseData.success(reviewService.createReview(dto, userId));
    }

    /** GET /api/order/reviews/{orderId} - 获取评价（需要登录） */
    @GetMapping("/order/reviews/{orderId}")
    public ResponseData<ReviewRespDto> getReview(
        @PathVariable Integer orderId,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        return ResponseData.success(reviewService.getReviewByOrderId(orderId, userId));
    }

    /** POST /api/order/orders/{id}/cancel - 撤单（仅限待接单状态） */
    @PostMapping("/order/orders/{id}/cancel")
    public ResponseData<Void> cancelOrder(
        @PathVariable Integer id,
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        orderService.cancelOrder(id, userId);
        return ResponseData.success();
    }

    /** GET /api/order/orders/history - 按天统计订单历史（变更6） */
    @GetMapping("/order/orders/history")
    public ResponseData<List<DayOrderSummaryDto>> getOrderHistory(
        @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        Integer userId = userOrderService.requireUserId(auth);
        return ResponseData.success(orderService.getOrderHistory(userId));
    }
}
