package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.CartItemDto;
import cn.chinacici.service.order.dto.CreateOrderReqDto;
import cn.chinacici.service.order.dto.DayOrderSummaryDto;
import cn.chinacici.service.order.dto.MealTypeInfoDto;
import cn.chinacici.service.order.dto.OrderItemDto;
import cn.chinacici.service.order.dto.OrderRespDto;
import cn.chinacici.service.order.entity.LoDish;
import cn.chinacici.service.order.entity.LoOrder;
import cn.chinacici.service.order.entity.LoOrderItem;
import cn.chinacici.service.order.entity.LoReview;
import cn.chinacici.service.order.mapper.LoDishMapper;
import cn.chinacici.service.order.mapper.LoFileMapper;
import cn.chinacici.service.order.mapper.LoOrderItemMapper;
import cn.chinacici.service.order.mapper.LoOrderMapper;
import cn.chinacici.service.order.mapper.LoReviewImageRelaMapper;
import cn.chinacici.service.order.mapper.LoReviewMapper;
import cn.chinacici.service.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private final LoOrderMapper orderMapper;
    private final LoOrderItemMapper orderItemMapper;
    private final LoDishMapper dishMapper;
    private final LoReviewMapper reviewMapper;
    private final LoReviewImageRelaMapper reviewImageRelaMapper;
    private final LoFileMapper fileMapper;

    public OrderServiceImpl(LoOrderMapper orderMapper,
                            LoOrderItemMapper orderItemMapper,
                            LoDishMapper dishMapper,
                            LoReviewMapper reviewMapper,
                            LoReviewImageRelaMapper reviewImageRelaMapper,
                            LoFileMapper fileMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.dishMapper = dishMapper;
        this.reviewMapper = reviewMapper;
        this.reviewImageRelaMapper = reviewImageRelaMapper;
        this.fileMapper = fileMapper;
    }

    /** 根据当前时间推算餐次: 20:00~07:59->早饭(0), 08:00~11:59->午饭(1), 12:00~19:59->晚饭(2) */
    private int calcMealType() {
        int hour = LocalTime.now().getHour();
        if (hour >= 20 || hour < 8) return 0;
        if (hour >= 8 && hour < 12) return 1;
        return 2;
    }

    @Override
    public MealTypeInfoDto getMealTypeInfo(Integer userId) {
        int mealType = calcMealType();
        LocalDate today = LocalDate.now();

        MealTypeInfoDto dto = new MealTypeInfoDto();
        dto.setMealType(mealType);
        dto.setMealTypeName(mealTypeName(mealType));

        List<LoOrder> todayOrders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getUserId, userId)
                .eq(LoOrder::getOrderDate, today)
                .eq(LoOrder::getMealType, 2)
                .eq(LoOrder::getIsAddDish, 0)
                .lt(LoOrder::getState, 3)
                .eq(LoOrder::getIsDeleted, 0)
        );
        dto.setCanAddDish(!todayOrders.isEmpty() && mealType == 2);
        if (!todayOrders.isEmpty()) {
            dto.setTodayDinnerOrderId(todayOrders.get(0).getId());
        }
        return dto;
    }

    @Override
    @Transactional
    public OrderRespDto createOrder(CreateOrderReqDto dto, Integer userId) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请至少选择一道菜");
        }

        String orderNo = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + System.currentTimeMillis() % 1000000;

        LocalDate today = LocalDate.now();
        int now = (int)(System.currentTimeMillis() / 1000);

        // 变更4：计算totalKiss = sum(itemPrice * quantity)
        int totalKiss = 0;
        for (CartItemDto item : dto.getItems()) {
            LoDish dish = dishMapper.selectById(item.getDishId());
            int price = (dish != null && dish.getPrice() != null) ? dish.getPrice() : 0;
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            totalKiss += price * qty;
        }

        LoOrder order = new LoOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setOrderDate(today);
        order.setMealType(dto.getMealType() != null ? dto.getMealType() : calcMealType());
        order.setIsAddDish(dto.getIsAddDish() != null ? dto.getIsAddDish() : 0);
        order.setParentOrderId(dto.getParentOrderId() != null ? dto.getParentOrderId() : 0);
        order.setState(0);
        order.setRemark(dto.getRemark() != null ? dto.getRemark() : "");
        order.setIsVerified(1);
        // 变更3：保存totalKiss
        order.setTotalKiss(totalKiss);
        order.setIsDeleted(0);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        for (CartItemDto item : dto.getItems()) {
            LoDish dish = dishMapper.selectById(item.getDishId());
            String dishName = dish != null ? dish.getName() : (item.getDishName() != null ? item.getDishName() : "");
            // 变更3：item_price 单价快照
            int price = (dish != null && dish.getPrice() != null) ? dish.getPrice() : 0;
            LoOrderItem oi = new LoOrderItem();
            oi.setOrderId(order.getId());
            oi.setDishId(item.getDishId());
            oi.setDishName(dishName);
            oi.setQuantity(item.getQuantity() != null ? item.getQuantity() : 1);
            oi.setRemark(item.getRemark() != null ? item.getRemark() : "");
            oi.setItemPrice(price);
            oi.setCreatedAt(now);
            orderItemMapper.insert(oi);
        }

        return getOrderById(order.getId());
    }

    @Override
    public List<OrderRespDto> getMyOrders(Integer userId) {
        List<LoOrder> orders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getUserId, userId)
                .eq(LoOrder::getIsDeleted, 0)
                .orderByDesc(LoOrder::getCreatedAt)
        );
        return orders.stream().map(this::toOrderRespDto).collect(Collectors.toList());
    }

    @Override
    public OrderRespDto getOrderById(Integer orderId) {
        LoOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "订单不存在");
        }
        return toOrderRespDto(order);
    }

    @Override
    public void completeOrder(Integer orderId, Integer userId) {
        LoOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException(ResultCode.PARAMETER_ERROR, "订单不存在");
        if (!order.getUserId().equals(userId)) throw new ServiceException(ResultCode.USER_NO_PRIVILEGE, "无权操作");
        if (order.getState() != 2) throw new ServiceException(ResultCode.PARAMETER_ERROR, "当前订单状态不能完成");
        int now = (int)(System.currentTimeMillis() / 1000);
        orderMapper.update(null, new LambdaUpdateWrapper<LoOrder>()
            .eq(LoOrder::getId, orderId)
            .set(LoOrder::getState, 3)
            .set(LoOrder::getUpdatedAt, now));
    }

    @Override
    public List<OrderRespDto> getAdminOrders(Integer state) {
        LambdaQueryWrapper<LoOrder> qw = new LambdaQueryWrapper<LoOrder>()
            .eq(LoOrder::getIsDeleted, 0)
            .orderByDesc(LoOrder::getCreatedAt);
        if (state != null) qw.eq(LoOrder::getState, state);
        return orderMapper.selectList(qw).stream().map(this::toOrderRespDto).collect(Collectors.toList());
    }

    @Override
    public void acceptOrder(Integer orderId) {
        changeState(orderId, 0, 1);
    }

    @Override
    public void serveOrder(Integer orderId) {
        changeState(orderId, 1, 2);
    }

    /**
     * 变更6：按天统计订单历史，按日期 desc 排序，每天聚合所有订单（含已完成和进行中），带 review 评分。
     */
    @Override
    public List<DayOrderSummaryDto> getOrderHistory(Integer userId) {
        List<LoOrder> orders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getUserId, userId)
                .eq(LoOrder::getIsDeleted, 0)
                .orderByDesc(LoOrder::getOrderDate)
                .orderByDesc(LoOrder::getCreatedAt)
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<OrderRespDto>> dateMap = new LinkedHashMap<>();
        for (LoOrder order : orders) {
            String dateKey = order.getOrderDate() != null ? order.getOrderDate().format(formatter) : "未知日期";
            OrderRespDto dto = toOrderRespDto(order);
            dateMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(dto);
        }

        List<DayOrderSummaryDto> result = new ArrayList<>();
        for (Map.Entry<String, List<OrderRespDto>> entry : dateMap.entrySet()) {
            DayOrderSummaryDto summary = new DayOrderSummaryDto();
            summary.setDate(entry.getKey());
            int dayTotal = entry.getValue().stream()
                .mapToInt(o -> o.getTotalKiss() != null ? o.getTotalKiss() : 0)
                .sum();
            summary.setTotalKiss(dayTotal);
            summary.setOrders(entry.getValue());
            result.add(summary);
        }
        return result;
    }

    private void changeState(Integer orderId, int fromState, int toState) {
        LoOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new ServiceException(ResultCode.PARAMETER_ERROR, "订单不存在");
        if (order.getState() != fromState) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "订单状态不正确，无法操作");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        orderMapper.update(null, new LambdaUpdateWrapper<LoOrder>()
            .eq(LoOrder::getId, orderId)
            .set(LoOrder::getState, toState)
            .set(LoOrder::getUpdatedAt, now));
    }

    private OrderRespDto toOrderRespDto(LoOrder order) {
        OrderRespDto dto = new OrderRespDto();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setUserId(order.getUserId());
        dto.setOrderDate(order.getOrderDate() != null ? order.getOrderDate().toString() : "");
        dto.setMealType(order.getMealType());
        dto.setMealTypeName(mealTypeName(order.getMealType()));
        dto.setIsAddDish(order.getIsAddDish());
        dto.setParentOrderId(order.getParentOrderId());
        dto.setState(order.getState());
        dto.setStateName(stateName(order.getState()));
        dto.setRemark(order.getRemark());
        dto.setCreatedAt(order.getCreatedAt());
        // 变更3/5：totalKiss
        dto.setTotalKiss(order.getTotalKiss());

        List<LoOrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<LoOrderItem>().eq(LoOrderItem::getOrderId, order.getId())
        );
        List<OrderItemDto> itemDtos = items.stream().map(i -> {
            OrderItemDto itemDto = new OrderItemDto();
            itemDto.setId(i.getId());
            itemDto.setDishId(i.getDishId());
            itemDto.setDishName(i.getDishName());
            itemDto.setQuantity(i.getQuantity());
            // 变更5：带出 itemPrice
            itemDto.setItemPrice(i.getItemPrice());
            return itemDto;
        }).collect(Collectors.toList());
        dto.setItems(itemDtos);

        // 变更5/6：查 review，同时取 score
        LoReview review = reviewMapper.selectOne(
            new LambdaQueryWrapper<LoReview>()
                .eq(LoReview::getOrderId, order.getId())
                .eq(LoReview::getIsDeleted, 0)
        );
        dto.setHasReview(review != null);
        if (review != null) {
            dto.setReviewScore(review.getScore());
        }

        return dto;
    }

    private String mealTypeName(Integer mt) {
        if (mt == null) return "";
        switch (mt) {
            case 0: return "早饭";
            case 1: return "午饭";
            case 2: return "晚饭";
            default: return "";
        }
    }

    private String stateName(Integer s) {
        if (s == null) return "";
        switch (s) {
            case 0: return "待接单";
            case 1: return "等待开饭";
            case 2: return "饭好了";
            case 3: return "已完成";
            default: return "";
        }
    }
}
