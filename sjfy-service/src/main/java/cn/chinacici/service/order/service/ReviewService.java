package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CreateReviewReqDto;
import cn.chinacici.service.order.dto.ReviewRespDto;

public interface ReviewService {
    ReviewRespDto createReview(CreateReviewReqDto dto, Integer userId);
    ReviewRespDto getReviewByOrderId(Integer orderId, Integer userId);
}
