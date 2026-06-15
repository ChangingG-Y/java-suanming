package cn.chinacici.service.order.service;

import cn.chinacici.service.order.dto.CategoryRespDto;
import cn.chinacici.service.order.dto.DishRespDto;
import cn.chinacici.service.order.dto.SaveCategoryReqDto;
import cn.chinacici.service.order.dto.SaveDishReqDto;

import java.util.List;

public interface DishService {
    List<CategoryRespDto> getCategoryList();
    List<DishRespDto> getDishList(Integer categoryId);

    // Admin CRUD
    CategoryRespDto saveCategory(SaveCategoryReqDto dto, Integer operatorId);
    void updateCategory(Integer id, SaveCategoryReqDto dto, Integer operatorId);
    void deleteCategory(Integer id);
    DishRespDto saveDish(SaveDishReqDto dto, Integer operatorId);
    void updateDish(Integer id, SaveDishReqDto dto, Integer operatorId);
    void deleteDish(Integer id);
    List<DishRespDto> getAdminDishList(Integer categoryId);
    List<CategoryRespDto> getAdminCategoryList();
}
