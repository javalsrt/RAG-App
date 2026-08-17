package com.znxsgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.znxsgl.dto.SemesterDTO;
import com.znxsgl.dto.SemesterSaveRequest;
import com.znxsgl.entity.Semester;
import com.znxsgl.mapper.SemesterMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学期服务
 *
 * 核心职责：
 * 1. 提供当前学期名称（替代代码中硬编码的 "2025-2026-2"）
 * 2. 管理员手动切换当前学期
 * 3. 学生端"当前学期 + 假期提示"查询
 *
 * 切换规则：is_current=1 全局唯一，切换时先把旧 current 置 0，再设新 current=1。
 */
@Service
public class SemesterService {

    private final SemesterMapper semesterMapper;
    private final JdbcTemplate jdbc;

    public SemesterService(SemesterMapper semesterMapper, JdbcTemplate jdbc) {
        this.semesterMapper = semesterMapper;
        this.jdbc = jdbc;
    }

    /**
     * 获取当前学期名称（供其他 Service 调用）。
     * 若未配置当前学期，返回 null，调用方需自行兜底。
     */
    public String getCurrentSemesterName() {
        Semester current = getCurrentSemester();
        return current != null ? current.getName() : null;
    }

    /**
     * 获取当前学期实体（含起止日期）。
     */
    public Semester getCurrentSemester() {
        return semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1)
                .last("LIMIT 1"));
    }

    /**
     * 学生端查询当前学期 + 假期提示。
     *
     * 三种情况：
     * 1. 已到开学日期 → 返回当前学期，status=ongoing
     * 2. 未到开学日期 → 返回当前学期，status=before，notice="距开学还有 X 天"
     * 3. 已过结束日期 → 返回当前学期，status=ended，notice="本学期已结束"
     * 4. 无当前学期 → 返回 null，调用方提示"新学期安排待定"
     */
    public SemesterDTO getCurrentSemesterWithStatus() {
        Semester current = getCurrentSemester();
        if (current == null) {
            return null;
        }
        return toDtoWithStatus(current);
    }

    /**
     * 根据学期名称查询学期状态。名称为空时回退到当前学期。
     */
    public SemesterDTO getSemesterWithStatusByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getCurrentSemesterWithStatus();
        }
        Semester s = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getName, name));
        if (s == null) {
            return null;
        }
        return toDtoWithStatus(s);
    }

    /**
     * 查询所有学期列表（按开学日期倒序，最新的在前）。
     */
    public List<SemesterDTO> listAll() {
        return semesterMapper.selectList(new LambdaQueryWrapper<Semester>()
                        .orderByDesc(Semester::getStartDate))
                .stream()
                .map(this::toDtoWithStatus)
                .collect(Collectors.toList());
    }

    /**
     * 新增学期。不允许与已有学期同名。
     * 如果是 EXTRA 类型，保存关联的班级列表。
     */
    @Transactional
    public Long create(SemesterSaveRequest request) {
        Long exists = semesterMapper.selectCount(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getName, request.getName()));
        if (exists > 0) {
            throw new IllegalArgumentException("学期名称已存在：" + request.getName());
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException("结束日期必须晚于开学日期");
        }

        Semester s = new Semester();
        s.setName(request.getName());
        s.setStartDate(request.getStartDate());
        s.setEndDate(request.getEndDate());
        s.setWeekCount(calculateWeekCount(request.getStartDate(), request.getEndDate()));
        s.setSemesterType(request.getSemesterType() != null ? request.getSemesterType() : "NORMAL");
        s.setIsCurrent(0); // 新增默认非当前
        semesterMapper.insert(s);

        // 保存班级关联（仅 EXTRA 类型）
        if ("EXTRA".equals(s.getSemesterType()) && request.getClassIds() != null) {
            for (Long classId : request.getClassIds()) {
                jdbc.update("INSERT IGNORE INTO semester_class (semester_id, class_id) VALUES (?, ?)",
                        s.getId(), classId);
            }
        }

        return s.getId();
    }

    /**
     * 修改学期信息（名称、起止日期、周数、类型、关联班级）。
     * 不能修改当前学期的名称会导致全局引用断裂，因此名称修改需谨慎。
     */
    @Transactional
    public void update(Long semesterId, SemesterSaveRequest request) {
        Semester s = semesterMapper.selectById(semesterId);
        if (s == null) {
            throw new IllegalArgumentException("学期不存在");
        }

        // 名称唯一性：排除自己
        Long exists = semesterMapper.selectCount(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getName, request.getName())
                .ne(Semester::getId, semesterId));
        if (exists > 0) {
            throw new IllegalArgumentException("学期名称已存在：" + request.getName());
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException("结束日期必须晚于开学日期");
        }

        s.setName(request.getName());
        s.setStartDate(request.getStartDate());
        s.setEndDate(request.getEndDate());
        s.setWeekCount(calculateWeekCount(request.getStartDate(), request.getEndDate()));
        s.setSemesterType(request.getSemesterType() != null ? request.getSemesterType() : "NORMAL");
        semesterMapper.updateById(s);

        // 更新班级关联：先删除旧关联，再写入新关联
        jdbc.update("DELETE FROM semester_class WHERE semester_id = ?", s.getId());
        if ("EXTRA".equals(s.getSemesterType()) && request.getClassIds() != null) {
            for (Long classId : request.getClassIds()) {
                jdbc.update("INSERT IGNORE INTO semester_class (semester_id, class_id) VALUES (?, ?)",
                        s.getId(), classId);
            }
        }
    }

    /**
     * 切换当前学期。旧 current 自动置 0，新 current 置 1。
     * 同时负责"开启新学期"：通常在新学期开学前由管理员手动切换。
     */
    @Transactional
    public void switchCurrent(Long semesterId) {
        Semester target = semesterMapper.selectById(semesterId);
        if (target == null) {
            throw new IllegalArgumentException("学期不存在：id=" + semesterId);
        }

        // 1. 把所有 is_current=1 置 0
        semesterMapper.update(null, new LambdaUpdateWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1)
                .set(Semester::getIsCurrent, 0));

        // 2. 把目标学期置为 current
        semesterMapper.update(null, new LambdaUpdateWrapper<Semester>()
                .eq(Semester::getId, semesterId)
                .set(Semester::getIsCurrent, 1));
    }

    /**
     * 删除学期。禁止删除当前学期。
     */
    @Transactional
    public void delete(Long semesterId) {
        Semester s = semesterMapper.selectById(semesterId);
        if (s == null) {
            throw new IllegalArgumentException("学期不存在");
        }
        if (s.getIsCurrent() != null && s.getIsCurrent() == 1) {
            throw new IllegalArgumentException("不能删除当前生效的学期");
        }
        semesterMapper.deleteById(semesterId);
    }

    /**
     * 根据起止日期自动计算总周数。
     * 规则：结束日期与开始日期之间相隔的周数 + 1（首尾各算一周）。
     */
    private Integer calculateWeekCount(LocalDate startDate, LocalDate endDate) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (days < 0) {
            return 0;
        }
        return (int) (days / 7 + 1);
    }

    /**
     * 计算学期状态和提示语。
     */
    private SemesterDTO toDtoWithStatus(Semester s) {
        SemesterDTO dto = new SemesterDTO();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setStartDate(s.getStartDate());
        dto.setEndDate(s.getEndDate());
        dto.setWeekCount(s.getWeekCount());
        dto.setIsCurrent(s.getIsCurrent() != null && s.getIsCurrent() == 1);
        dto.setSemesterType(s.getSemesterType() != null ? s.getSemesterType() : "NORMAL");

        // 填充关联班级（仅 EXTRA 类型）
        if ("EXTRA".equals(dto.getSemesterType())) {
            List<Map<String, Object>> classRows = jdbc.queryForList(
                    "SELECT sc.class_id, ci.class_name FROM semester_class sc " +
                    "JOIN class_info ci ON ci.id = sc.class_id " +
                    "WHERE sc.semester_id = ?", s.getId());
            List<Long> cids = new ArrayList<>();
            List<String> cnames = new ArrayList<>();
            for (Map<String, Object> row : classRows) {
                cids.add(((Number) row.get("class_id")).longValue());
                cnames.add((String) row.get("class_name"));
            }
            dto.setClassIds(cids);
            dto.setClassNames(cnames);
        }

        LocalDate today = LocalDate.now();
        if ("EXTRA".equals(s.getSemesterType())) {
            if (today.isBefore(s.getStartDate())) {
                dto.setStatus("before");
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, s.getStartDate());
                dto.setNotice("培训尚未开始，距培训还有 " + days + " 天");
            } else if (today.isAfter(s.getEndDate())) {
                dto.setStatus("ended");
                dto.setNotice("培训已结束");
            } else {
                dto.setStatus("ongoing");
                long weekPassed = java.time.temporal.ChronoUnit.WEEKS.between(s.getStartDate(), today) + 1;
                dto.setNotice("培训进行中（第 " + weekPassed + " 周）");
            }
        } else {
            if (today.isBefore(s.getStartDate())) {
                dto.setStatus("before");
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, s.getStartDate());
                dto.setNotice("距开学还有 " + days + " 天（" + s.getStartDate() + " 开学）");
            } else if (today.isAfter(s.getEndDate())) {
                dto.setStatus("ended");
                dto.setNotice("本学期已结束");
            } else {
                dto.setStatus("ongoing");
                long weekPassed = java.time.temporal.ChronoUnit.WEEKS.between(s.getStartDate(), today) + 1;
                dto.setNotice("第 " + weekPassed + " 教学周");
            }
        }
        return dto;
    }

    /**
     * 获取指定班级当前生效的所有学期名称。
     * - 正常学期（NORMAL）：is_current=1，所有班级通用
     * - 假期培训（EXTRA）：is_current=1 且该班级已关联；或日期在范围内且该班级已关联
     *
     * @param classId 学生所在班级ID
     * @return 生效学期名称列表
     */
    public List<String> getActiveSemesterNamesByClassId(Long classId) {
        List<String> result = new ArrayList<>();

        // 1. 当前正常学期（所有班级通用）
        Semester currentNormal = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1)
                .eq(Semester::getSemesterType, "NORMAL")
                .last("LIMIT 1"));
        if (currentNormal != null) {
            result.add(currentNormal.getName());
        }

        // 2. 当前假期培训学期（is_current=1 且班级已关联，不限制日期，便于提前排课）
        if (classId != null) {
            List<Map<String, Object>> currentExtraRows = jdbc.queryForList(
                    "SELECT s.name FROM semester s " +
                    "JOIN semester_class sc ON sc.semester_id = s.id " +
                    "WHERE s.semester_type = 'EXTRA' " +
                    "AND s.is_current = 1 " +
                    "AND sc.class_id = ?",
                    classId);
            for (Map<String, Object> row : currentExtraRows) {
                String name = (String) row.get("name");
                if (!result.contains(name)) {
                    result.add(name);
                }
            }
        }

        // 3. 当前正在进行的、且关联了该班级的假期培训学期
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT s.name FROM semester s " +
                "JOIN semester_class sc ON sc.semester_id = s.id " +
                "WHERE s.semester_type = 'EXTRA' " +
                "AND s.start_date <= ? AND s.end_date >= ? " +
                "AND sc.class_id = ?",
                today, today, classId);
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("name");
            if (!result.contains(name)) {
                result.add(name);
            }
        }

        return result;
    }

    /**
     * 获取所有当前生效的学期名称（兼容旧调用，不按班级过滤）。
     */
    public List<String> getActiveSemesterNames() {
        List<String> result = new ArrayList<>();

        // 1. 当前正常学期
        Semester currentNormal = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1)
                .eq(Semester::getSemesterType, "NORMAL")
                .last("LIMIT 1"));
        if (currentNormal != null) {
            result.add(currentNormal.getName());
        }

        // 2. 当前假期培训学期（is_current=1，不限制日期）
        Semester currentExtra = semesterMapper.selectOne(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getIsCurrent, 1)
                .eq(Semester::getSemesterType, "EXTRA")
                .last("LIMIT 1"));
        if (currentExtra != null) {
            result.add(currentExtra.getName());
        }

        // 3. 当前正在进行的假期培训学期
        LocalDate today = LocalDate.now();
        List<Semester> activeExtras = semesterMapper.selectList(new LambdaQueryWrapper<Semester>()
                .eq(Semester::getSemesterType, "EXTRA")
                .le(Semester::getStartDate, today)
                .ge(Semester::getEndDate, today));
        for (Semester s : activeExtras) {
            if (!result.contains(s.getName())) {
                result.add(s.getName());
            }
        }

        return result;
    }
}
