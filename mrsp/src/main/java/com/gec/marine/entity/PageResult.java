package com.gec.marine.entity;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;    // 数据列表
    private Long total;         // 总记录数
    private Long current;       // 当前页码
    private Long size;          // 每页显示数量

    public PageResult(List<T> records, Long total, Long current, Long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }
}
