package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CreateOrderReqDto;
import cn.chinacici.service.order.dto.DayOrderSummaryDto;
import cn.chinacici.service.order.dto.MealTypeInfoDto;
import cn.chinacici.service.order.dto.OrderRespDto;

import java.util.List;

public interface OrderService {
    MealTypeInfoDto getMealTypeInfo(Integer userId);
    OrderRespDto createOrder(CreateOrderReqDto dto, Integer userId, Integer tenantId);
    List<OrderRespDto> getMyOrders(Integer userId);
    OrderRespDto getOrderById(Integer orderId, Integer userId);
    void completeOrder(Integer orderId, Integer userId);

    // Admin
    List<OrderRespDto> getAdminOrders(Integer state, Integer tenantId);
    void acceptOrder(Integer orderId);
    void serveOrder(Integer orderId);

    List<DayOrderSummaryDto> getOrderHistory(Integer userId);
    void cancelOrder(Integer orderId, Integer userId);
}
