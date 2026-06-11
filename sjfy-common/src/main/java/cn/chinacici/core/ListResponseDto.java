package cn.chinacici.core;

import java.util.Collections;
import java.util.List;

/**
 * 列表响应封装。
 */
public class ListResponseDto<T> {
    private List<T> list;

    public static <T> ListResponseDto<T> empty() {
        return list(Collections.emptyList());
    }

    public static <T> ListResponseDto<T> list(List<T> list) {
        ListResponseDto<T> responseDto = new ListResponseDto<>();
        responseDto.setList(list);
        return responseDto;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
