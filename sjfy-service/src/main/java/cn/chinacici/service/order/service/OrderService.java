package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CreateOrderReqDto;
import cn.chinacici.service.order.dto.DayOrderSummaryDto;
import cn.chinacici.service.order.dto.MealTypeInfoDto;
import cn.chinacici.service.order.dto.OrderRespDto;

import java.util.List;

public interface OrderService {
    MealTypeInfoDto getMealTypeInfo(Integer userId);
    OrderRespDto createOrder(CreateOrderReqDto dto, Integer userId);
    List<OrderRespDto> getMyOrders(Integer userId);
    OrderRespDto getOrderById(Integer orderId);
    void completeOrder(Integer orderId, Integer userId);

    // Admin
    List<OrderRespDto> getAdminOrders(Integer state);
    void acceptOrder(Integer orderId);
    void serveOrder(Integer orderId);

    // 变更6：按天统计订单历史
    List<DayOrderSummaryDto> getOrderHistory(Integer userId);
}
