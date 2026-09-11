package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.college.elective.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * 院系 Mapper。
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
