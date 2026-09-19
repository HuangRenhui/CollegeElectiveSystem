package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.PageResult;
import com.college.elective.common.RedisKeys;
import com.college.elective.common.ResultCode;
import com.college.elective.entity.Course;
import com.college.elective.entity.Semester;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.SemesterMapper;
import com.college.elective.service.SemesterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学期服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, Semester> implements SemesterService {

    private final CourseMapper courseMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<Semester> pageSemesters(Long pageNum, Long pageSize, String keyword) {
        IPage<Semester> page = page(new Page<>(pageNum, pageSize), Wrappers.<Semester>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), Semester::getSemesterName, keyword)
                .orderByDesc(Semester::getIsCurrent)
                .orderByDesc(Semester::getStartDate));
        return PageResult.of(page);
    }

    @Override
    public List<Semester> listAll() {
        return list(Wrappers.<Semester>lambdaQuery()
                .orderByDesc(Semester::getIsCurrent)
                .orderByDesc(Semester::getStartDate));
    }

    @Override
    public Semester getCurrentSemester() {
        return getOne(Wrappers.<Semester>lambdaQuery()
                .eq(Semester::getIsCurrent, 1)
                .last("LIMIT 1"));
    }

    @Override
    public Long getCurrentSemesterId() {
        Semester semester = getCurrentSemester();
        return semester == null ? null : semester.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSemester(Semester semester) {
        if (semester.getStartDate() != null && semester.getEndDate() != null
                && semester.getStartDate().isAfter(semester.getEndDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        if (semester.getSelectStartTime() != null && semester.getSelectEndTime() != null
                && semester.getSelectStartTime().isAfter(semester.getSelectEndTime())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "选课开始时间不能晚于选课结束时间");
        }

        Long count = baseMapper.selectCount(Wrappers.<Semester>lambdaQuery()
                .eq(Semester::getSemesterCode, semester.getSemesterCode())
                .ne(semester.getId() != null, Semester::getId, semester.getId()));
        BusinessException.throwIf(count != null && count > 0, ResultCode.DATA_ALREADY_EXISTS,
                "学期编码 " + semester.getSemesterCode() + " 已存在");

        if (semester.getTotalWeeks() == null) {
            semester.setTotalWeeks(20);
        }
        if (semester.getIsCurrent() == null) {
            semester.setIsCurrent(0);
        }
        if (semester.getStatus() == null) {
            semester.setStatus(Constants.SEMESTER_STATUS_NOT_START);
        }

        if (semester.getId() == null) {
            save(semester);
        } else {
            updateById(semester);
        }

        if (Integer.valueOf(1).equals(semester.getIsCurrent())) {
            setCurrent(semester.getId());
        }
        clearSemesterCache();
        log.info("保存学期成功: {}", semester.getSemesterName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCurrent(Long id) {
        Semester semester = getById(id);
        BusinessException.throwIf(semester == null, ResultCode.SEMESTER_NOT_FOUND);

        // 清除其他学期的当前标记
        update(Wrappers.<Semester>lambdaUpdate()
                .set(Semester::getIsCurrent, 0)
                .eq(Semester::getIsCurrent, 1));

        update(Wrappers.<Semester>lambdaUpdate()
                .set(Semester::getIsCurrent, 1)
                .eq(Semester::getId, id));

        clearSemesterCache();
        log.info("当前学期已设置为: {}", semester.getSemesterName());
    }

    /**
     * 清理学期相关缓存。学期变更会导致统计维度与选课窗口判断发生变化，
     * 因此需要移除缓存，使后续请求重新从数据库加载。
     */
    private void clearSemesterCache() {
        stringRedisTemplate.delete(RedisKeys.CURRENT_SEMESTER);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSemester(Long id) {
        Semester semester = getById(id);
        BusinessException.throwIf(semester == null, ResultCode.SEMESTER_NOT_FOUND);
        BusinessException.throwIf(Integer.valueOf(1).equals(semester.getIsCurrent()),
                ResultCode.OPERATION_FORBIDDEN, "当前学期不允许删除");

        Long courseCount = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getSemesterId, id));
        if (courseCount != null && courseCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FORBIDDEN,
                    "该学期下存在 " + courseCount + " 门课程，无法删除");
        }

        removeById(id);
        clearSemesterCache();
        log.info("删除学期成功: {}", semester.getSemesterName());
    }
}
