package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.CreateReviewReqDto;
import cn.chinacici.service.order.dto.FileRespDto;
import cn.chinacici.service.order.dto.ReviewRespDto;
import cn.chinacici.service.order.entity.LoFile;
import cn.chinacici.service.order.entity.LoOrder;
import cn.chinacici.service.order.entity.LoReview;
import cn.chinacici.service.order.entity.LoReviewImageRela;
import cn.chinacici.service.order.mapper.LoFileMapper;
import cn.chinacici.service.order.mapper.LoOrderMapper;
import cn.chinacici.service.order.mapper.LoReviewImageRelaMapper;
import cn.chinacici.service.order.mapper.LoReviewMapper;
import cn.chinacici.service.order.service.ReviewService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {
    private final LoReviewMapper reviewMapper;
    private final LoReviewImageRelaMapper reviewImageRelaMapper;
    private final LoFileMapper fileMapper;
    private final LoOrderMapper orderMapper;

    public ReviewServiceImpl(LoReviewMapper reviewMapper,
                             LoReviewImageRelaMapper reviewImageRelaMapper,
                             LoFileMapper fileMapper,
                             LoOrderMapper orderMapper) {
        this.reviewMapper = reviewMapper;
        this.reviewImageRelaMapper = reviewImageRelaMapper;
        this.fileMapper = fileMapper;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional
    public ReviewRespDto createReview(CreateReviewReqDto dto, Integer userId) {
        // 验证 score 范围
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 10) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "评分范围为1-10");
        }
        // 验证 fileIds 最多3个
        if (dto.getFileIds() != null && dto.getFileIds().size() > 3) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "最多上传3张图片");
        }
        // 验证订单状态
        LoOrder order = orderMapper.selectById(dto.getOrderId());
        if (order == null || order.getIsDeleted() == 1) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "订单不存在");
        }
        if (order.getState() != 3) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "订单未完成，无法评价");
        }
        // 检查是否已评价
        Long existCount = reviewMapper.selectCount(
            new LambdaQueryWrapper<LoReview>()
                .eq(LoReview::getOrderId, dto.getOrderId())
                .eq(LoReview::getIsDeleted, 0)
        );
        if (existCount > 0) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "已评价过该订单");
        }

        int now = (int)(System.currentTimeMillis() / 1000);
        LoReview review = new LoReview();
        review.setOrderId(dto.getOrderId());
        review.setUserId(userId);
        review.setScore(dto.getScore());
        review.setContent(dto.getContent() != null ? dto.getContent() : "");
        review.setIsDeleted(0);
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewMapper.insert(review);

        // 插入图片关联
        if (dto.getFileIds() != null && !dto.getFileIds().isEmpty()) {
            for (int i = 0; i < dto.getFileIds().size(); i++) {
                LoReviewImageRela rela = new LoReviewImageRela();
                rela.setReviewId(review.getId());
                rela.setFileId(dto.getFileIds().get(i));
                rela.setSeq(i + 1);
                reviewImageRelaMapper.insert(rela);
            }
        }

        return buildReviewRespDto(review);
    }

    @Override
    public ReviewRespDto getReviewByOrderId(Integer orderId) {
        LoReview review = reviewMapper.selectOne(
            new LambdaQueryWrapper<LoReview>()
                .eq(LoReview::getOrderId, orderId)
                .eq(LoReview::getIsDeleted, 0)
        );
        if (review == null) return null;
        return buildReviewRespDto(review);
    }

    private ReviewRespDto buildReviewRespDto(LoReview review) {
        ReviewRespDto dto = new ReviewRespDto();
        dto.setId(review.getId());
        dto.setOrderId(review.getOrderId());
        dto.setUserId(review.getUserId());
        dto.setScore(review.getScore());
        dto.setContent(review.getContent());
        dto.setCreatedAt(review.getCreatedAt());

        // 查询图片
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
}
