package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.CartItemDto;
import cn.chinacici.service.order.dto.CreateOrderReqDto;
import cn.chinacici.service.order.dto.DayOrderSummaryDto;
import cn.chinacici.service.order.dto.FileRespDto;
import cn.chinacici.service.order.dto.MealTypeInfoDto;
import cn.chinacici.service.order.dto.OrderItemDto;
import cn.chinacici.service.order.dto.OrderRespDto;
import cn.chinacici.service.order.dto.ReviewRespDto;
import cn.chinacici.service.order.entity.LoDish;
import cn.chinacici.service.order.entity.LoFile;
import cn.chinacici.service.order.entity.LoOrder;
import cn.chinacici.service.order.entity.LoOrderItem;
import cn.chinacici.service.order.entity.LoReview;
import cn.chinacici.service.order.entity.LoReviewImageRela;
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

        // 只有晚饭且订单未出餐(state < 2)才能加菜（加菜会合并进原订单）
        List<LoOrder> todayDinnerOrders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getUserId, userId)
                .eq(LoOrder::getOrderDate, today)
                .eq(LoOrder::getMealType, 2)
                .eq(LoOrder::getIsAddDish, 0)
                .lt(LoOrder::getState, 2)
                .eq(LoOrder::getIsDeleted, 0)
        );
        dto.setCanAddDish(!todayDinnerOrders.isEmpty() && mealType == 2);
        if (!todayDinnerOrders.isEmpty()) {
            dto.setTodayDinnerOrderId(todayDinnerOrders.get(0).getId());
        }
        return dto;
    }

    @Override
    @Transactional
    public OrderRespDto createOrder(CreateOrderReqDto dto, Integer userId) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请至少选择一道菜");
        }

        int targetMealType = dto.getMealType() != null ? dto.getMealType() : calcMealType();
        LocalDate targetDate = parseMealDate(dto.getMealDate());
        if (targetMealType < 0 || targetMealType > 2) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "餐次不正确");
        }

        // 前端显式带 parentOrderId 时优先按原单处理。原单已出餐则不再加菜，继续落成一笔新订单。
        if (dto.getIsAddDish() != null && dto.getIsAddDish() == 1
                && dto.getParentOrderId() != null && dto.getParentOrderId() > 0) {
            LoOrder parent = orderMapper.selectById(dto.getParentOrderId());
            if (canMergeAddDish(parent, userId)) {
                return mergeAddDishToOrder(dto, parent);
            }
            if (parent != null && !parent.getUserId().equals(userId)) {
                throw new ServiceException(ResultCode.USER_NO_PRIVILEGE, "无权操作");
            }
        }

        // 同一用户、同一天、同一餐次如果还有未出餐订单，普通下单也合并进去，评价和详情都归在同一单。
        LoOrder mergeTarget = findMergeableOrder(userId, targetDate, targetMealType);
        if (mergeTarget != null) {
            return mergeAddDishToOrder(dto, mergeTarget);
        }

        String orderNo = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + System.currentTimeMillis() % 1000000;

        int now = (int)(System.currentTimeMillis() / 1000);

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
        order.setOrderDate(targetDate);
        order.setMealType(targetMealType);
        order.setIsAddDish(0);
        order.setParentOrderId(0);
        order.setState(0);
        order.setRemark(dto.getRemark() != null ? dto.getRemark() : "");
        order.setIsVerified(1);
        order.setTotalKiss(totalKiss);
        order.setIsDeleted(0);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        for (CartItemDto item : dto.getItems()) {
            LoDish dish = dishMapper.selectById(item.getDishId());
            String dishName = dish != null ? dish.getName() : (item.getDishName() != null ? item.getDishName() : "");
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

        return getOrderById(order.getId(), userId);
    }

    /** 加菜合并：将新菜品追加到原订单，更新 totalKiss，不创建新 order 记录。 */
    private OrderRespDto mergeAddDishToOrder(CreateOrderReqDto dto, LoOrder parent) {
        int now = (int)(System.currentTimeMillis() / 1000);
        int addKiss = 0;

        for (CartItemDto item : dto.getItems()) {
            LoDish dish = dishMapper.selectById(item.getDishId());
            String dishName = dish != null ? dish.getName() : (item.getDishName() != null ? item.getDishName() : "");
            int price = (dish != null && dish.getPrice() != null) ? dish.getPrice() : 0;
            int qty = item.getQuantity() != null ? item.getQuantity() : 1;
            addKiss += price * qty;

            LoOrderItem oi = new LoOrderItem();
            oi.setOrderId(parent.getId());
            oi.setDishId(item.getDishId());
            oi.setDishName(dishName);
            oi.setQuantity(qty);
            oi.setRemark(item.getRemark() != null ? item.getRemark() : "");
            oi.setItemPrice(price);
            oi.setCreatedAt(now);
            orderItemMapper.insert(oi);
        }

        int newTotal = (parent.getTotalKiss() != null ? parent.getTotalKiss() : 0) + addKiss;
        orderMapper.update(null, new LambdaUpdateWrapper<LoOrder>()
            .eq(LoOrder::getId, parent.getId())
            .set(LoOrder::getTotalKiss, newTotal)
            .set(LoOrder::getUpdatedAt, now));

        return getOrderById(parent.getId(), parent.getUserId());
    }

    /** 解析前端餐次日期，未传或格式异常时使用今天，避免因为日期控件缺省导致下单失败。 */
    private LocalDate parseMealDate(String mealDate) {
        if (mealDate == null || mealDate.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(mealDate.trim());
        } catch (Exception e) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "点餐日期格式不正确");
        }
    }

    /** 判断订单是否还能承接加菜：同一用户且未出餐。 */
    private boolean canMergeAddDish(LoOrder order, Integer userId) {
        return order != null
            && order.getIsDeleted() != null && order.getIsDeleted() == 0
            && order.getUserId() != null && order.getUserId().equals(userId)
            && order.getState() != null && order.getState() < 2;
    }

    /** 查找同餐次未出餐主订单；找到后普通下单也合并，避免同餐次被拆成多笔评价。 */
    private LoOrder findMergeableOrder(Integer userId, LocalDate orderDate, Integer mealType) {
        List<LoOrder> orders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getUserId, userId)
                .eq(LoOrder::getOrderDate, orderDate)
                .eq(LoOrder::getMealType, mealType)
                .eq(LoOrder::getIsAddDish, 0)
                .lt(LoOrder::getState, 2)
                .eq(LoOrder::getIsDeleted, 0)
                .orderByDesc(LoOrder::getCreatedAt)
        );
        return orders.isEmpty() ? null : orders.get(0);
    }

    @Override
    public List<OrderRespDto> getMyOrders(Integer userId) {
        List<LoOrder> orders = orderMapper.selectList(
            new LambdaQueryWrapper<LoOrder>()
                .eq(LoOrder::getUserId, userId)
                .eq(LoOrder::getIsDeleted, 0)
                .orderByDesc(LoOrder::getCreatedAt)
        );
        return orders.stream().map(o -> toOrderRespDto(o, false)).collect(Collectors.toList());
    }

    @Override
    public OrderRespDto getOrderById(Integer orderId, Integer userId) {
        LoOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getIsDeleted() == 1) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "订单不存在");
        }
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new ServiceException(ResultCode.USER_NO_PRIVILEGE, "无权查看");
        }
        return toOrderRespDto(order, true);
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
        return orderMapper.selectList(qw).stream().map(o -> toOrderRespDto(o, false)).collect(Collectors.toList());
    }

    @Override
    public void acceptOrder(Integer orderId) {
        changeState(orderId, 0, 1);
    }

    @Override
    public void serveOrder(Integer orderId) {
        changeState(orderId, 1, 2);
    }

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
            OrderRespDto dto = toOrderRespDto(order, false);
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

    /**
     * @param loadFullReview true=详情页：加载完整 review（含 images）；false=列表页：只带 reviewScore
     */
    private OrderRespDto toOrderRespDto(LoOrder order, boolean loadFullReview) {
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
        dto.setTotalKiss(order.getTotalKiss());

        // 菜品列表：加载 imageFileId（从 lo_dish 取）
        List<LoOrderItem> items = orderItemMapper.selectList(
            new LambdaQueryWrapper<LoOrderItem>().eq(LoOrderItem::getOrderId, order.getId())
        );
        List<OrderItemDto> itemDtos = items.stream().map(i -> {
            OrderItemDto itemDto = new OrderItemDto();
            itemDto.setId(i.getId());
            itemDto.setDishId(i.getDishId());
            itemDto.setDishName(i.getDishName());
            itemDto.setQuantity(i.getQuantity());
            itemDto.setItemPrice(i.getItemPrice());
            itemDto.setRemark(i.getRemark());
            // 从菜品表取图片 fileId（可能为 null）
            if (i.getDishId() != null) {
                LoDish dish = dishMapper.selectById(i.getDishId());
                if (dish != null) {
                    itemDto.setImageFileId(dish.getImageFileId());
                }
            }
            return itemDto;
        }).collect(Collectors.toList());
        dto.setItems(itemDtos);

        // 评价
        LoReview review = reviewMapper.selectOne(
            new LambdaQueryWrapper<LoReview>()
                .eq(LoReview::getOrderId, order.getId())
                .eq(LoReview::getIsDeleted, 0)
        );
        dto.setHasReview(review != null);
        if (review != null) {
            dto.setReviewScore(review.getScore());
            if (loadFullReview) {
                dto.setReview(buildReviewRespDto(review));
            }
        }

        return dto;
    }

    /** 构建完整评价 DTO，包含图片列表 */
    private ReviewRespDto buildReviewRespDto(LoReview review) {
        ReviewRespDto dto = new ReviewRespDto();
        dto.setId(review.getId());
        dto.setOrderId(review.getOrderId());
        dto.setUserId(review.getUserId());
        dto.setScore(review.getScore());
        dto.setContent(review.getContent());
        dto.setCreatedAt(review.getCreatedAt());

        List<LoReviewImageRela> relas = reviewImageRelaMapper.selectList(
            new LambdaQueryWrapper<LoReviewImageRela>()
                .eq(LoReviewImageRela::getReviewId, review.getId())
                .orderByAsc(LoReviewImageRela::getSeq)
        );
        List<FileRespDto> images = new ArrayList<>();
        for (LoReviewImageRela rela : relas) {
            LoFile loFile = fileMapper.selectById(rela.getFileId());
            if (loFile != null) {
                FileRespDto fileDto = new FileRespDto();
                fileDto.setId(loFile.getId());
                fileDto.setUrl("/api/order/file/" + loFile.getId());
                fileDto.setThumbnailUrl("/api/order/file/" + loFile.getId() + "/thumbnail");
                fileDto.setOriginalName(loFile.getOriginalName());
                fileDto.setFileSize(loFile.getFileSize());
                images.add(fileDto);
            }
        }
        dto.setImages(images);
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
