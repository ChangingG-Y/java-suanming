package cn.chinacici.service.order.service.impl;

import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.order.dto.CategoryRespDto;
import cn.chinacici.service.order.dto.DishRespDto;
import cn.chinacici.service.order.dto.SaveCategoryReqDto;
import cn.chinacici.service.order.dto.SaveDishReqDto;
import cn.chinacici.service.order.entity.LoDish;
import cn.chinacici.service.order.entity.LoDishCategory;
import cn.chinacici.service.order.mapper.LoDishCategoryMapper;
import cn.chinacici.service.order.mapper.LoDishMapper;
import cn.chinacici.service.order.service.DishService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl implements DishService {
    private final LoDishCategoryMapper categoryMapper;
    private final LoDishMapper dishMapper;

    public DishServiceImpl(LoDishCategoryMapper categoryMapper, LoDishMapper dishMapper) {
        this.categoryMapper = categoryMapper;
        this.dishMapper = dishMapper;
    }

    @Override
    public List<CategoryRespDto> getCategoryList(Integer tenantId) {
        LambdaQueryWrapper<LoDishCategory> qw = new LambdaQueryWrapper<LoDishCategory>()
            .eq(LoDishCategory::getState, 1)
            .eq(LoDishCategory::getIsDeleted, 0)
            .orderByAsc(LoDishCategory::getSeq);
        if (tenantId != null) qw.eq(LoDishCategory::getTenantId, tenantId);
        return categoryMapper.selectList(qw).stream().map(this::toCategoryRespDto).collect(Collectors.toList());
    }

    @Override
    public List<DishRespDto> getDishList(Integer categoryId, Integer tenantId) {
        LambdaQueryWrapper<LoDish> qw = new LambdaQueryWrapper<LoDish>()
            .eq(LoDish::getState, 1)
            .eq(LoDish::getIsDeleted, 0)
            .orderByAsc(LoDish::getSeq);
        if (categoryId != null) qw.eq(LoDish::getCategoryId, categoryId);
        if (tenantId != null) qw.eq(LoDish::getTenantId, tenantId);
        return dishMapper.selectList(qw).stream().map(this::toDishRespDto).collect(Collectors.toList());
    }

    @Override
    public List<CategoryRespDto> getAdminCategoryList(Integer tenantId) {
        LambdaQueryWrapper<LoDishCategory> qw = new LambdaQueryWrapper<LoDishCategory>()
            .eq(LoDishCategory::getIsDeleted, 0)
            .orderByAsc(LoDishCategory::getSeq);
        if (tenantId != null) qw.eq(LoDishCategory::getTenantId, tenantId);
        return categoryMapper.selectList(qw).stream().map(this::toCategoryRespDto).collect(Collectors.toList());
    }

    @Override
    public List<DishRespDto> getAdminDishList(Integer categoryId, Integer tenantId) {
        LambdaQueryWrapper<LoDish> qw = new LambdaQueryWrapper<LoDish>()
            .eq(LoDish::getIsDeleted, 0)
            .orderByAsc(LoDish::getSeq);
        if (categoryId != null) qw.eq(LoDish::getCategoryId, categoryId);
        if (tenantId != null) qw.eq(LoDish::getTenantId, tenantId);
        return dishMapper.selectList(qw).stream().map(this::toDishRespDto).collect(Collectors.toList());
    }

    @Override
    public CategoryRespDto saveCategory(SaveCategoryReqDto dto, Integer operatorId, Integer tenantId) {
        int now = (int)(System.currentTimeMillis() / 1000);
        LoDishCategory category = new LoDishCategory();
        category.setTenantId(tenantId);
        category.setName(dto.getName());
        category.setState(dto.getState() != null ? dto.getState() : 1);
        category.setIsDeleted(0);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        categoryMapper.insert(category);
        category.setSeq(dto.getSeq() != null ? dto.getSeq() : category.getId());
        categoryMapper.updateById(category);
        return toCategoryRespDto(category);
    }

    @Override
    public void updateCategory(Integer id, SaveCategoryReqDto dto, Integer operatorId) {
        LoDishCategory category = categoryMapper.selectById(id);
        if (category == null || category.getIsDeleted() == 1) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        LambdaUpdateWrapper<LoDishCategory> uw = new LambdaUpdateWrapper<LoDishCategory>()
            .eq(LoDishCategory::getId, id)
            .set(LoDishCategory::getUpdatedAt, now);
        if (dto.getName() != null) uw.set(LoDishCategory::getName, dto.getName());
        if (dto.getSeq() != null) uw.set(LoDishCategory::getSeq, dto.getSeq());
        if (dto.getState() != null) uw.set(LoDishCategory::getState, dto.getState());
        categoryMapper.update(null, uw);
    }

    @Override
    public void deleteCategory(Integer id) {
        LoDishCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "分类不存在");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        categoryMapper.update(null, new LambdaUpdateWrapper<LoDishCategory>()
            .eq(LoDishCategory::getId, id)
            .set(LoDishCategory::getIsDeleted, 1)
            .set(LoDishCategory::getUpdatedAt, now));
    }

    @Override
    public DishRespDto saveDish(SaveDishReqDto dto, Integer operatorId, Integer tenantId) {
        int now = (int)(System.currentTimeMillis() / 1000);
        LoDish dish = new LoDish();
        dish.setTenantId(tenantId);
        dish.setCategoryId(dto.getCategoryId());
        dish.setName(dto.getName());
        dish.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        dish.setImageFileId(dto.getImageFileId() != null ? dto.getImageFileId() : 0);
        dish.setState(dto.getState() != null ? dto.getState() : 1);
        dish.setPrice(dto.getPrice());
        dish.setIsDeleted(0);
        dish.setCreatedAt(now);
        dish.setUpdatedAt(now);
        dishMapper.insert(dish);
        dish.setSeq(dto.getSeq() != null ? dto.getSeq() : dish.getId());
        dishMapper.updateById(dish);
        return toDishRespDto(dish);
    }

    @Override
    public void updateDish(Integer id, SaveDishReqDto dto, Integer operatorId) {
        LoDish dish = dishMapper.selectById(id);
        if (dish == null || dish.getIsDeleted() == 1) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "菜品不存在");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        LambdaUpdateWrapper<LoDish> uw = new LambdaUpdateWrapper<LoDish>()
            .eq(LoDish::getId, id)
            .set(LoDish::getUpdatedAt, now);
        if (dto.getCategoryId() != null) uw.set(LoDish::getCategoryId, dto.getCategoryId());
        if (dto.getName() != null) uw.set(LoDish::getName, dto.getName());
        if (dto.getDescription() != null) uw.set(LoDish::getDescription, dto.getDescription());
        if (dto.getImageFileId() != null) uw.set(LoDish::getImageFileId, dto.getImageFileId());
        if (dto.getState() != null) uw.set(LoDish::getState, dto.getState());
        if (dto.getSeq() != null) uw.set(LoDish::getSeq, dto.getSeq());
        if (dto.getPrice() != null) uw.set(LoDish::getPrice, dto.getPrice());
        dishMapper.update(null, uw);
    }

    @Override
    public void deleteDish(Integer id) {
        LoDish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new ServiceException(ResultCode.DATA_NOT_FOUND, "菜品不存在");
        }
        int now = (int)(System.currentTimeMillis() / 1000);
        dishMapper.update(null, new LambdaUpdateWrapper<LoDish>()
            .eq(LoDish::getId, id)
            .set(LoDish::getIsDeleted, 1)
            .set(LoDish::getUpdatedAt, now));
    }

    private CategoryRespDto toCategoryRespDto(LoDishCategory c) {
        CategoryRespDto dto = new CategoryRespDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setSeq(c.getSeq());
        dto.setState(c.getState());
        return dto;
    }

    private DishRespDto toDishRespDto(LoDish d) {
        DishRespDto dto = new DishRespDto();
        dto.setId(d.getId());
        dto.setCategoryId(d.getCategoryId());
        dto.setName(d.getName());
        dto.setDescription(d.getDescription());
        dto.setImageFileId(d.getImageFileId());
        dto.setState(d.getState());
        dto.setSeq(d.getSeq());
        dto.setPrice(d.getPrice());
        if (d.getImageFileId() != null && d.getImageFileId() > 0) {
            dto.setImageUrl("/api/order/file/" + d.getImageFileId());
            dto.setThumbnailUrl("/api/order/file/" + d.getImageFileId() + "/thumbnail");
        }
        return dto;
    }
}
