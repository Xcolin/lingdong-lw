package com.lingdong.payroll.domain.batch;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PayrollBatchItemMapper extends BaseMapper<PayrollBatchItem> {

    @Delete("delete from payroll_batch_item where batch_id = #{batchId}")
    void deleteByBatchId(Long batchId);

    @Select("select * from payroll_batch_item where batch_id = #{batchId} order by row_no asc")
    List<PayrollBatchItem> selectByBatchId(Long batchId);
}
