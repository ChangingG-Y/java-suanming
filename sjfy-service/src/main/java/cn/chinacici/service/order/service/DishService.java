package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CategoryRespDto;
import cn.chinacici.service.order.dto.DishRespDto;
import cn.chinacici.service.order.dto.SaveCategoryReqDto;
import cn.chinacici.service.order.dto.SaveDishReqDto;

import java.util.List;

public interface DishService {
    List<CategoryRespDto> getCategoryList(Integer tenantId);
    List<DishRespDto> getDishList(Integer categoryId, Integer tenantId);

    // Admin CRUD
    List<CategoryRespDto> getAdminCategoryList(Integer tenantId);
    List<DishRespDto> getAdminDishList(Integer categoryId, Integer tenantId);
    CategoryRespDto saveCategory(SaveCategoryReqDto dto, Integer operatorId, Integer tenantId);
    void updateCategory(Integer id, SaveCategoryReqDto dto, Integer operatorId);
    void deleteCategory(Integer id);
    DishRespDto saveDish(SaveDishReqDto dto, Integer operatorId, Integer tenantId);
    void updateDish(Integer id, SaveDishReqDto dto, Integer operatorId);
    void deleteDish(Integer id);
}
