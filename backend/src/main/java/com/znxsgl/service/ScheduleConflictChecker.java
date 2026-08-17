package com.znxsgl.service;

import com.znxsgl.entity.*;
import com.znxsgl.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 排课冲突检测引擎
 *
 * 核心数据结构：三张"占用位图"
 *   - classOccupy[classId][dayOfWeek][startNode..endNode]
 *   - teacherOccupy[teacherId][dayOfWeek][startNode..endNode]
 *   - roomOccupy[classroomName][dayOfWeek][startNode..endNode]
 *
 * 每张位图用 BitSet 实现，检查冲突时间复杂度 O(1)。
 *
 * 设计说明：
 *   - dayOfWeek: 1-7 对应周一到周日
 *   - startNode: 节次编号，从 1 开始
 *   - 连堂课 step 节占用 startNode ~ startNode+step-1
 */
@Service
public class ScheduleConflictChecker {

    private final ScheduleMapper scheduleMapper;
    private final ScheduleLockMapper scheduleLockMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final SemesterMapper semesterMapper;

    /** 每天最大节数（艺术学部/汽车学部作息，上午4节+下午2节+晚上2节 = 8节） */
    public static final int MAX_NODES_PER_DAY = 8;
    /** 每周排课天数（周一至周日，7天；节假日补课允许排周日） */
    public static final int MAX_DAYS = 7;
    /** 默认最大教学周数 */
    public static final int DEFAULT_MAX_WEEKS = 20;

    public ScheduleConflictChecker(ScheduleMapper scheduleMapper,
                                    ScheduleLockMapper scheduleLockMapper,
                                    UserMapper userMapper,
                                    CourseMapper courseMapper,
                                    SemesterMapper semesterMapper) {
        this.scheduleMapper = scheduleMapper;
        this.scheduleLockMapper = scheduleLockMapper;
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.semesterMapper = semesterMapper;
    }

    /**
     * 从数据库加载当前学期已排课表，构建三张占用位图。
     * 返回构建好的冲突上下文，供排课算法使用。
     * <p>
     * 自按周拆分 schedule 后，占用位图增加“周”维度，确保第 N 周的课不会与第 M 周误判冲突。
     *
     * @param onlyLocked true=只加载锁定的课（教师调课后的），false=加载所有已排课
     */
    public ConflictContext buildContext(String semester, boolean onlyLocked) {
        ConflictContext ctx = new ConflictContext(resolveMaxWeeks(semester));

        LambdaQueryWrapper<Schedule> qw = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getSemester, semester)
                .eq(Schedule::getStatus, 1)
                .gt(Schedule::getDayOfWeek, 0);  // 排除 day_of_week=0 的占位记录

        if (onlyLocked) {
            // 只加载被锁定的课（schedule_lock 表中登记的）
            List<Long> lockedIds = scheduleLockMapper.selectList(
                    new LambdaQueryWrapper<ScheduleLock>().eq(ScheduleLock::getSemester, semester))
                    .stream().map(ScheduleLock::getScheduleId).toList();
            if (lockedIds.isEmpty()) {
                return ctx; // 没有锁定的课，空上下文
            }
            qw.in(Schedule::getId, lockedIds);
        }

        List<Schedule> schedules = scheduleMapper.selectList(qw);

        for (Schedule s : schedules) {
            // 跳过无效数据
            if (s.getDayOfWeek() == null || s.getStartNode() == null || s.getStep() == null) continue;
            if (s.getUserId() == null) continue;

            List<Integer> weeks = parseWeeks(s.getWeeks());
            if (weeks.isEmpty()) {
                // 兼容旧数据：未设置周次时默认视为全学期（1~maxWeeks）
                weeks = new ArrayList<>();
                for (int w = 1; w <= ctx.maxWeeks; w++) weeks.add(w);
            }

            // 找出这个学生对应的班级
            User user = userMapper.selectById(s.getUserId());
            if (user != null && user.getClassId() != null) {
                for (int week : weeks) {
                    markOccupied(ctx.classOccupy,
                            user.getClassId().toString(),
                            week,
                            s.getDayOfWeek(),
                            s.getStartNode(),
                            s.getStep());
                }
            }

            // 教师占用：通过课程记录获取教师ID
            if (s.getCourseId() != null) {
                Course course = courseMapper.selectById(s.getCourseId());
                if (course != null && course.getTeacherId() != null) {
                    for (int week : weeks) {
                        markOccupied(ctx.teacherOccupy,
                                "t_" + course.getTeacherId(),
                                week,
                                s.getDayOfWeek(),
                                s.getStartNode(),
                                s.getStep());
                    }
                }
            }

            for (int week : weeks) {
                markOccupied(ctx.roomOccupy,
                        s.getClassroom() != null ? s.getClassroom() : "unknown",
                        week,
                        s.getDayOfWeek(),
                        s.getStartNode(),
                        s.getStep());
            }
        }

        return ctx;
    }

    /**
     * 检测某节课在指定周是否有冲突。
     *
     * @param classId    班级ID（字符串键）
     * @param teacherId  教师ID（字符串键，可为 null 表示不检测教师）
     * @param roomName   教室名（字符串键，可为 null 表示不检测教室）
     * @param week       教学周（从1开始）
     * @param dayOfWeek  星期几
     * @param startNode  起始节次
     * @param step       连堂节数
     * @return ConflictResult（含冲突类型和详情）
     */
    public ConflictResult checkConflict(ConflictContext ctx,
                                        String classId,
                                        Long teacherId,
                                        String roomName,
                                        int week,
                                        int dayOfWeek,
                                        int startNode,
                                        int step) {
        ConflictResult result = new ConflictResult();
        result.conflict = false;

        if (week < 1 || week > ctx.maxWeeks) {
            result.conflict = true;
            result.details.add("教学周无效：" + week + "（应在 1~" + ctx.maxWeeks + " 之间）");
            return result;
        }
        if (dayOfWeek < 1 || dayOfWeek > MAX_DAYS) {
            result.conflict = true;
            result.details.add("星期几无效：" + dayOfWeek);
            return result;
        }
        if (startNode < 1 || startNode + step - 1 > MAX_NODES_PER_DAY) {
            result.conflict = true;
            result.details.add("节次范围无效：第" + startNode + "节起" + step + "节");
            return result;
        }

        // 1. 班级冲突
        if (isOccupied(ctx.classOccupy, classId, week, dayOfWeek, startNode, step)) {
            result.conflict = true;
            result.details.add("班级冲突：该周该时段班级已有课程");
        }

        // 2. 教师冲突
        if (teacherId != null) {
            String teacherKey = "t_" + teacherId;
            if (isOccupied(ctx.teacherOccupy, teacherKey, week, dayOfWeek, startNode, step)) {
                result.conflict = true;
                result.details.add("教师冲突：该周该时段教师已有其他课程");
            }
        }

        // 3. 教室冲突
        if (roomName != null && !roomName.isEmpty()) {
            if (isOccupied(ctx.roomOccupy, roomName, week, dayOfWeek, startNode, step)) {
                result.conflict = true;
                result.details.add("教室冲突：该周该时段教室已被占用");
            }
        }

        return result;
    }

    /**
     * 标记某节课为"已占用"（成功排课后调用）。
     */
    public void markOccupied(ConflictContext ctx,
                             String classId,
                             Long teacherId,
                             String roomName,
                             int week,
                             int dayOfWeek,
                             int startNode,
                             int step) {
        markOccupied(ctx.classOccupy, classId, week, dayOfWeek, startNode, step);
        if (teacherId != null) {
            markOccupied(ctx.teacherOccupy, "t_" + teacherId, week, dayOfWeek, startNode, step);
        }
        if (roomName != null && !roomName.isEmpty()) {
            markOccupied(ctx.roomOccupy, roomName, week, dayOfWeek, startNode, step);
        }
    }

    /**
     * 取消某节课的占用标记（回溯/调课时调用）。
     */
    public void unmarkOccupied(ConflictContext ctx,
                               String classId,
                               Long teacherId,
                               String roomName,
                               int week,
                               int dayOfWeek,
                               int startNode,
                               int step) {
        unmarkOccupied(ctx.classOccupy, classId, week, dayOfWeek, startNode, step);
        if (teacherId != null) {
            unmarkOccupied(ctx.teacherOccupy, "t_" + teacherId, week, dayOfWeek, startNode, step);
        }
        if (roomName != null && !roomName.isEmpty()) {
            unmarkOccupied(ctx.roomOccupy, roomName, week, dayOfWeek, startNode, step);
        }
    }

    /**
     * 解析 weeks JSON 字符串，如 "[1,2,3,4]" → [1,2,3,4]。
     */
    public static List<Integer> parseWeeks(String weeksJson) {
        List<Integer> list = new ArrayList<>();
        if (weeksJson == null || weeksJson.trim().isEmpty()) {
            return list;
        }
        String trimmed = weeksJson.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return list;
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return list;
        }
        for (String s : inner.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try {
                list.add(Integer.parseInt(s));
            } catch (NumberFormatException ignored) {
                // 忽略非法数字
            }
        }
        return list;
    }

    /** 根据学期名称查询最大教学周数。 */
    private int resolveMaxWeeks(String semester) {
        Semester s = semesterMapper.selectOne(
                new LambdaQueryWrapper<Semester>().eq(Semester::getName, semester));
        if (s != null && s.getWeekCount() != null && s.getWeekCount() > 0) {
            return s.getWeekCount();
        }
        return DEFAULT_MAX_WEEKS;
    }

    // ===== 私有工具方法 =====

    private boolean isOccupied(Map<String, Map<Integer, BitSet[]>> map, String key,
                               int week, int dayOfWeek, int startNode, int step) {
        Map<Integer, BitSet[]> weeks = map.get(key);
        if (weeks == null) return false;
        BitSet[] days = weeks.get(week);
        if (days == null) return false;
        BitSet dayBits = days[dayOfWeek - 1];
        if (dayBits == null) return false;
        // 检查 [startNode-1, startNode+step-2] 区间（BitSet 0-based）
        for (int i = startNode - 1; i < startNode + step - 1; i++) {
            if (dayBits.get(i)) return true;
        }
        return false;
    }

    private void markOccupied(Map<String, Map<Integer, BitSet[]>> map, String key,
                              int week, int dayOfWeek, int startNode, int step) {
        Map<Integer, BitSet[]> weeks = map.computeIfAbsent(key, k -> new HashMap<>());
        BitSet[] days = weeks.computeIfAbsent(week, k -> new BitSet[MAX_DAYS]);
        if (days[dayOfWeek - 1] == null) {
            days[dayOfWeek - 1] = new BitSet(MAX_NODES_PER_DAY);
        }
        for (int i = startNode - 1; i < startNode + step - 1; i++) {
            days[dayOfWeek - 1].set(i);
        }
    }

    private void unmarkOccupied(Map<String, Map<Integer, BitSet[]>> map, String key,
                                int week, int dayOfWeek, int startNode, int step) {
        Map<Integer, BitSet[]> weeks = map.get(key);
        if (weeks == null) return;
        BitSet[] days = weeks.get(week);
        if (days == null || days[dayOfWeek - 1] == null) return;
        for (int i = startNode - 1; i < startNode + step - 1; i++) {
            days[dayOfWeek - 1].clear(i);
        }
        // 如果该周该天已全部清空，可释放内存（可选）
        if (days[dayOfWeek - 1].isEmpty()) {
            days[dayOfWeek - 1] = null;
        }
    }

    // ===== 内部数据结构 =====

    /**
     * 冲突检测上下文（三张位图，增加"周"维度）。
     */
    public static class ConflictContext {
        public final int maxWeeks;
        /** 班级占用：key=classId(String)，value=每周每天的 BitSet */
        public final Map<String, Map<Integer, BitSet[]>> classOccupy = new HashMap<>();
        /** 教师占用：key=t_teacherId，value=每周每天的 BitSet */
        public final Map<String, Map<Integer, BitSet[]>> teacherOccupy = new HashMap<>();
        /** 教室占用：key=教室名，value=每周每天的 BitSet */
        public final Map<String, Map<Integer, BitSet[]>> roomOccupy = new HashMap<>();

        public ConflictContext(int maxWeeks) {
            this.maxWeeks = maxWeeks;
        }
    }

    /**
     * 冲突检测结果
     */
    public static class ConflictResult {
        public boolean conflict;
        public List<String> details = new ArrayList<>();
    }
}
