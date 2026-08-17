package com.znxsgl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.znxsgl.dto.*;
import com.znxsgl.entity.Course;
import com.znxsgl.entity.CourseChapter;
import com.znxsgl.entity.CourseLesson;
import com.znxsgl.entity.DocumentVector;
import com.znxsgl.entity.Teacher;
import com.znxsgl.entity.TeachingTask;
import com.znxsgl.entity.User;
import com.znxsgl.mapper.CourseChapterMapper;
import com.znxsgl.mapper.CourseLessonMapper;
import com.znxsgl.mapper.CourseMapper;
import com.znxsgl.mapper.DocumentVectorMapper;
import com.znxsgl.mapper.TeacherMapper;
import com.znxsgl.mapper.TeachingTaskMapper;
import com.znxsgl.mapper.UserMapper;
import com.znxsgl.util.WordChapterParser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程章节与资源服务
 * 支持课程-章节-课时三级结构管理
 */
@Service
public class CourseChapterService {

    private final CourseChapterMapper chapterMapper;
    private final CourseLessonMapper lessonMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final TeacherMapper teacherMapper;
    private final TeachingTaskMapper teachingTaskMapper;
    private final JdbcTemplate jdbc;
    private final LlmService llmService;
    private final EmbeddingService embeddingService;
    private final DocumentVectorMapper docMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourseChapterService(CourseChapterMapper chapterMapper,
                                CourseLessonMapper lessonMapper,
                                CourseMapper courseMapper,
                                UserMapper userMapper,
                                TeacherMapper teacherMapper,
                                TeachingTaskMapper teachingTaskMapper,
                                JdbcTemplate jdbc,
                                LlmService llmService,
                                EmbeddingService embeddingService,
                                DocumentVectorMapper docMapper) {
        this.chapterMapper = chapterMapper;
        this.lessonMapper = lessonMapper;
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.teacherMapper = teacherMapper;
        this.teachingTaskMapper = teachingTaskMapper;
        this.jdbc = jdbc;
        this.llmService = llmService;
        this.embeddingService = embeddingService;
        this.docMapper = docMapper;
    }

    /**
     * 查询课程的章节列表（含课时）
     *
     * @param courseId 课程ID
     * @param userId   当前用户ID
     * @param isAdmin  是否管理员
     */
    public List<ChapterDTO> listChaptersByCourse(Long courseId, Long userId, boolean isAdmin) {
        if (!canAccessCourse(courseId, userId, isAdmin)) {
            return Collections.emptyList();
        }

        User currentUser = userMapper.selectById(userId);
        boolean isStudent = !isAdmin && currentUser != null && currentUser.getRole() != null && currentUser.getRole() == 1;

        LambdaQueryWrapper<CourseChapter> chapterQw = new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)
                .orderByAsc(CourseChapter::getSortOrder)
                .orderByAsc(CourseChapter::getChapterNo);
        // 学生只展示启用章节；管理员/教师需要管理全部章节
        if (isStudent) {
            chapterQw.eq(CourseChapter::getStatus, 1);
        }
        List<CourseChapter> chapters = chapterMapper.selectList(chapterQw);

        if (CollectionUtils.isEmpty(chapters)) {
            return Collections.emptyList();
        }

        List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).collect(Collectors.toList());
        LambdaQueryWrapper<CourseLesson> lessonQw = new LambdaQueryWrapper<CourseLesson>()
                .in(CourseLesson::getChapterId, chapterIds)
                .orderByAsc(CourseLesson::getSortOrder)
                .orderByAsc(CourseLesson::getLessonNo);
        // 学生只展示启用课时
        if (isStudent) {
            lessonQw.eq(CourseLesson::getStatus, 1);
        }
        List<CourseLesson> allLessons = lessonMapper.selectList(lessonQw);

        return chapters.stream().map(chapter -> {
            ChapterDTO dto = new ChapterDTO();
            dto.setId(chapter.getId());
            dto.setCourseId(chapter.getCourseId());
            dto.setChapterNo(chapter.getChapterNo());
            dto.setChapterName(chapter.getChapterName());
            dto.setDescription(chapter.getDescription());
            dto.setSortOrder(chapter.getSortOrder());
            dto.setStatus(chapter.getStatus());

            Course course = courseMapper.selectById(chapter.getCourseId());
            if (course != null) {
                dto.setCourseName(course.getCourseName());
            }

            List<LessonDTO> lessons = allLessons.stream()
                    .filter(l -> l.getChapterId().equals(chapter.getId()))
                    .map(this::convertLesson)
                    .collect(Collectors.toList());
            dto.setLessons(lessons);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 查询单个章节详情（含课时）
     */
    public ChapterDTO getChapterDetail(Long chapterId, Long userId, boolean isAdmin) {
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null || chapter.getStatus() == 0) {
            return null;
        }
        if (!canAccessCourse(chapter.getCourseId(), userId, isAdmin)) {
            return null;
        }
        return buildChapterDTO(chapter);
    }

    /**
     * 保存章节（新增/更新）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveChapter(ChapterSaveRequest request, Long userId, boolean isAdmin) {
        if (!canAccessCourse(request.getCourseId(), userId, isAdmin)) {
            throw new RuntimeException("无权限操作该课程的章节");
        }
        Course course = courseMapper.selectById(request.getCourseId());
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        if (request.getChapterName() == null || request.getChapterName().trim().isEmpty()) {
            throw new RuntimeException("章节名称不能为空");
        }
        if (request.getChapterName().length() > 200) {
            throw new RuntimeException("章节名称不能超过 200 个字符");
        }

        CourseChapter chapter = new CourseChapter();
        chapter.setId(request.getId());
        chapter.setCourseId(request.getCourseId());
        chapter.setChapterNo(request.getChapterNo());
        chapter.setChapterName(request.getChapterName());
        chapter.setDescription(request.getDescription());
        chapter.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : request.getChapterNo());
        chapter.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        if (chapter.getId() == null) {
            chapterMapper.insert(chapter);
        } else {
            CourseChapter exist = chapterMapper.selectById(chapter.getId());
            if (exist == null) {
                throw new RuntimeException("章节不存在");
            }
            if (!exist.getCourseId().equals(chapter.getCourseId())) {
                throw new RuntimeException("不允许修改章节所属课程");
            }
            chapterMapper.updateById(chapter);
        }
    }

    /**
     * 删除章节（逻辑删除，同时删除下属课时）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteChapter(Long chapterId, Long userId, boolean isAdmin) {
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }
        if (!canAccessCourse(chapter.getCourseId(), userId, isAdmin)) {
            throw new RuntimeException("无权限删除该章节");
        }
        chapterMapper.deleteById(chapterId);
        // 同步逻辑删除下属课时，保持数据一致性（MyBatis-Plus 会根据 @TableLogic 自动转为 UPDATE deleted=1）
        lessonMapper.delete(new LambdaQueryWrapper<CourseLesson>().eq(CourseLesson::getChapterId, chapterId));
    }

    /**
     * 保存课时（新增/更新）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLesson(LessonSaveRequest request, Long userId, boolean isAdmin) {
        CourseChapter chapter = chapterMapper.selectById(request.getChapterId());
        if (chapter == null || chapter.getStatus() == 0) {
            throw new RuntimeException("章节不存在或已禁用");
        }
        if (!canAccessCourse(chapter.getCourseId(), userId, isAdmin)) {
            throw new RuntimeException("无权限操作该课时");
        }

        CourseLesson lesson = new CourseLesson();
        lesson.setId(request.getId());
        lesson.setChapterId(request.getChapterId());
        lesson.setLessonNo(request.getLessonNo());
        lesson.setLessonName(request.getLessonName());
        lesson.setResourceType(request.getResourceType());
        lesson.setResourceUrl(request.getResourceUrl());
        lesson.setDuration(request.getDuration());
        lesson.setContent(request.getContent());
        lesson.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : request.getLessonNo());
        lesson.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        if (lesson.getId() == null) {
            lessonMapper.insert(lesson);
        } else {
            CourseLesson exist = lessonMapper.selectById(lesson.getId());
            if (exist == null) {
                throw new RuntimeException("课时不存在");
            }
            if (!exist.getChapterId().equals(lesson.getChapterId())) {
                throw new RuntimeException("不允许修改课时所属章节");
            }
            lessonMapper.updateById(lesson);
        }

        // content 有就向量化；没有就 AI 生成通用内容后保存 + 向量化（异步，失败不阻断主流程）
        if (StringUtils.hasText(lesson.getContent())) {
            vectorizeLessonAsync(chapter.getCourseId(), lesson.getChapterId(), lesson.getId(), lesson.getLessonName(), lesson.getContent(), false);
        } else if ("document".equalsIgnoreCase(lesson.getResourceType())) {
            Course course = courseMapper.selectById(chapter.getCourseId());
            String courseName = course != null ? course.getCourseName() : "";
            String chapterName = chapter.getChapterName();
            generateContentAndSaveAsync(chapter.getCourseId(), courseName, lesson.getChapterId(), lesson.getId(),
                    chapterName, lesson.getLessonName());
        }
    }

    /**
     * AI 生成通用课时内容，异步：生成 → update lesson.content → 向量化
     * 内容格式：Markdown 正文，约 300-500 字，分层结构化、段落间留空行
     */
    private void generateContentAndSaveAsync(Long courseId, String courseName, Long chapterId, Long lessonId,
                                              String chapterName, String lessonName) {
        new Thread(() -> {
            try {
                String systemPrompt = "你是资深大学教师，擅长编写结构化教学内容。只输出正文，不要标题和解释文字。";
                String userPrompt = String.format(
                        "请为《%s》课程的第「%s」章节中的「%s」课时编写通用教学内容。\n" +
                                "要求：\n" +
                                "1）内容分层结构化：使用 Markdown 二级标题（##）划分 2-3 个核心知识点小节；\n" +
                                "2）每个知识点先讲概念，再举 1 个简短例子，例子必须换行单独成段；\n" +
                                "3）所有英文函数名、变量名、类名、命令行、代码片段都必须用 Markdown 行内代码（`code`）或 fenced code block（```）包裹，禁止裸写英文函数名；例如 `np.sin`、`np.exp`、`print()`、`arr[arr > 5]`；\n" +
                                "4）较长或较复杂的代码示例必须用 fenced code block（```python ... ```）独占一行或多行，代码块内部除原始代码自带的换行外，严禁自动换行；单行代码必须完整显示在一行内；\n" +
                                "5）列举多个函数、特性、步骤时，必须使用 Markdown 列表（- 或 1.）展示，每个列表项独占一行，列表项中的英文函数名仍用 `code` 包裹；\n" +
                                "6）段落之间必须留空行，禁止大段纯文本堆砌，保持视觉呼吸感；\n" +
                                "7）关键术语使用加粗（**term**），整体 300-500 字，不要加一级标题。\n" +
                                "注意：JSON 字符串中的英文双引号必须正确转义，内容中尽量避免直接使用英文双引号，可用中文「」代替。",
                        courseName, chapterName, lessonName);
                String content = llmService.chat(systemPrompt, userPrompt);
                if (content == null || content.trim().isEmpty()) {
                    return;
                }
                content = content.trim();
                // 更新 lesson 表 content
                CourseLesson upd = new CourseLesson();
                upd.setId(lessonId);
                upd.setContent(content);
                lessonMapper.updateById(upd);
                vectorizeLessonAsync(courseId, chapterId, lessonId, lessonName, content, true);
            } catch (Exception e) {
                System.out.println("=== AI 生成课时内容失败: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 异步把课时内容向量化写入 document_vector（按课时分块，不拆 500 字）
     */
    private void vectorizeLessonAsync(Long courseId, Long chapterId, Long lessonId, String lessonName, String content, boolean isGen) {
        new Thread(() -> {
            try {
                String courseName = jdbc.queryForObject(
                        "SELECT course_name FROM course WHERE id = ?", String.class, courseId);
                // 先删除该课时的旧向量
                jdbc.update("DELETE FROM document_vector WHERE chapter_id = ? AND doc_name = ?",
                        chapterId, lessonName);
                float[] embedding = embeddingService.embed(content);
                DocumentVector dv = new DocumentVector();
                dv.setCourseName(courseName);
                dv.setChapterId(chapterId);
                dv.setDocName(lessonName);
                dv.setContentChunk(content);
                if (embedding != null) {
                    dv.setEmbedding(objectMapper.writeValueAsString(embedding));
                }
                dv.setCreatedAt(LocalDateTime.now());
                docMapper.insert(dv);
            } catch (Exception e) {
                System.out.println("=== 课时向量化失败: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 删除课时（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteLesson(Long lessonId, Long userId, boolean isAdmin) {
        CourseLesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw new RuntimeException("课时不存在");
        }
        CourseChapter chapter = chapterMapper.selectById(lesson.getChapterId());
        if (chapter == null) {
            throw new RuntimeException("章节不存在");
        }
        if (!canAccessCourse(chapter.getCourseId(), userId, isAdmin)) {
            throw new RuntimeException("无权限删除该课时");
        }
        // 清理该课时的向量数据
        try {
            jdbc.update("DELETE FROM document_vector WHERE chapter_id = ? AND doc_name = ?",
                    lesson.getChapterId(), lesson.getLessonName());
        } catch (Exception ignored) {}
        lessonMapper.deleteById(lessonId);
    }

    /**
     * 判断用户是否有权访问某课程数据
     * - 管理员：全部
     * - 教师：自己教授的课程
     * - 学生：自己已选课程（通过 schedule 表关联）
     */
    private boolean canAccessCourse(Long courseId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return false;
        }

        // 教师：优先按教学任务表判断该教师是否实际教授该课程；无记录时回退到 course.teacher_id
        if (user.getRole() != null && user.getRole() == 2) {
            Teacher teacher = teacherMapper.selectOne(
                    new LambdaQueryWrapper<Teacher>().eq(Teacher::getRealName, user.getRealName()));
            if (teacher == null) {
                return false;
            }
            Long teacherId = teacher.getId();
            Long courseIdVal = course.getId();
            Long taskCount = teachingTaskMapper.selectCount(
                    new LambdaQueryWrapper<TeachingTask>()
                            .eq(TeachingTask::getTeacherId, teacherId)
                            .eq(TeachingTask::getCourseId, courseIdVal));
            if (taskCount != null && taskCount > 0) {
                return true;
            }
            // 教学任务表无数据时回退到 course.teacher_id（兼容旧数据）
            return teacherId.equals(course.getTeacherId());
        }

        // 学生：通过 schedule 表判断是否选了该课程
        if (user.getRole() != null && user.getRole() == 1) {
            return studentHasCourse(userId, courseId);
        }

        return false;
    }

    private boolean studentHasCourse(Long userId, Long courseId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM schedule WHERE user_id = ? AND course_id = ? LIMIT 1",
                Integer.class, userId, courseId);
        return count != null && count > 0;
    }

    private ChapterDTO buildChapterDTO(CourseChapter chapter) {
        ChapterDTO dto = new ChapterDTO();
        dto.setId(chapter.getId());
        dto.setCourseId(chapter.getCourseId());
        dto.setChapterNo(chapter.getChapterNo());
        dto.setChapterName(chapter.getChapterName());
        dto.setDescription(chapter.getDescription());
        dto.setSortOrder(chapter.getSortOrder());
        dto.setStatus(chapter.getStatus());

        Course course = courseMapper.selectById(chapter.getCourseId());
        if (course != null) {
            dto.setCourseName(course.getCourseName());
        }

        List<LessonDTO> lessons = lessonMapper.selectList(
                new LambdaQueryWrapper<CourseLesson>()
                        .eq(CourseLesson::getChapterId, chapter.getId())
                        .eq(CourseLesson::getStatus, 1)
                        .orderByAsc(CourseLesson::getSortOrder)
                        .orderByAsc(CourseLesson::getLessonNo))
                .stream().map(this::convertLesson).collect(Collectors.toList());
        dto.setLessons(lessons);

        return dto;
    }

    private LessonDTO convertLesson(CourseLesson lesson) {
        LessonDTO dto = new LessonDTO();
        dto.setId(lesson.getId());
        dto.setChapterId(lesson.getChapterId());
        dto.setLessonNo(lesson.getLessonNo());
        dto.setLessonName(lesson.getLessonName());
        dto.setResourceType(lesson.getResourceType());
        dto.setResourceUrl(lesson.getResourceUrl());
        dto.setDuration(lesson.getDuration());
        dto.setContent(lesson.getContent());
        dto.setSortOrder(lesson.getSortOrder());
        dto.setStatus(lesson.getStatus());
        return dto;
    }

    /**
     * 从 Excel 导入课程章节和课时数据
     *
     * @param courseId   课程ID
     * @param excelInput Excel 文件输入流
     * @param userId     当前用户ID
     * @param isAdmin    是否管理员
     * @return 导入结果统计
     */
    @Transactional(rollbackFor = Exception.class)
    public ChapterImportResultDTO importFromExcel(Long courseId, InputStream excelInput,
                                                  Long userId, boolean isAdmin) {
        checkCourseAccess(courseId, userId, isAdmin);
        ChapterImportResultDTO result = new ChapterImportResultDTO();
        List<ChapterImportRowDTO> rows = parseExcel(excelInput, result);
        return saveImportRows(courseId, rows, result);
    }

    /**
     * 从 Word 导入课程章节和课时数据
     *
     * @param courseId  课程ID
     * @param wordInput Word 文件输入流
     * @param userId    当前用户ID
     * @param isAdmin   是否管理员
     * @return 导入结果统计
     */
    @Transactional(rollbackFor = Exception.class)
    public ChapterImportResultDTO importFromWord(Long courseId, InputStream wordInput,
                                                 Long userId, boolean isAdmin) {
        checkCourseAccess(courseId, userId, isAdmin);
        ChapterImportResultDTO result = new ChapterImportResultDTO();
        WordChapterParser parser = new WordChapterParser();
        List<ChapterImportRowDTO> rows = parser.parse(wordInput);
        if (!parser.getFailures().isEmpty()) {
            result.getFailures().addAll(parser.getFailures());
            result.setFailCount(result.getFailures().size());
            return result;
        }
        return saveImportRows(courseId, rows, result);
    }

    private void checkCourseAccess(Long courseId, Long userId, boolean isAdmin) {
        if (!canAccessCourse(courseId, userId, isAdmin)) {
            throw new RuntimeException("无权限操作该课程");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
    }

    private ChapterImportResultDTO saveImportRows(Long courseId, List<ChapterImportRowDTO> rows,
                                                  ChapterImportResultDTO result) {
        if (!result.getFailures().isEmpty()) {
            result.setFailCount(result.getFailures().size());
            return result;
        }
        if (rows.isEmpty()) {
            return result;
        }
        if (rows.size() > 1000) {
            throw new RuntimeException("单次导入最多支持1000行数据");
        }

        Map<Integer, List<ChapterImportRowDTO>> chapterGroup = rows.stream()
                .collect(Collectors.groupingBy(ChapterImportRowDTO::getChapterNo));

        List<CourseChapter> existChapters = chapterMapper.selectList(
                new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, courseId)
                        .in(CourseChapter::getChapterNo, chapterGroup.keySet()));
        Map<Integer, CourseChapter> chapterNoMap = existChapters.stream()
                .collect(Collectors.toMap(CourseChapter::getChapterNo, c -> c));

        for (Map.Entry<Integer, List<ChapterImportRowDTO>> entry : chapterGroup.entrySet()) {
            Integer chapterNo = entry.getKey();
            List<ChapterImportRowDTO> chapterRows = entry.getValue();

            CourseChapter chapter = chapterNoMap.get(chapterNo);
            boolean isNewChapter = chapter == null;
            if (isNewChapter) {
                chapter = new CourseChapter();
                chapter.setCourseId(courseId);
                chapter.setChapterNo(chapterNo);
                chapter.setStatus(1);
            }
            chapter.setChapterName(chapterRows.get(0).getChapterName());
            chapter.setDescription(chapterRows.get(0).getDescription());
            chapter.setSortOrder(chapterNo);

            if (isNewChapter) {
                chapterMapper.insert(chapter);
                result.setChapterCount(result.getChapterCount() + 1);
            } else {
                chapterMapper.updateById(chapter);
            }

            List<Integer> lessonNos = chapterRows.stream()
                    .map(ChapterImportRowDTO::getLessonNo)
                    .collect(Collectors.toList());
            List<CourseLesson> existLessons = lessonMapper.selectList(
                    new LambdaQueryWrapper<CourseLesson>()
                            .eq(CourseLesson::getChapterId, chapter.getId())
                            .in(CourseLesson::getLessonNo, lessonNos));
            Map<Integer, CourseLesson> lessonNoMap = existLessons.stream()
                    .collect(Collectors.toMap(CourseLesson::getLessonNo, l -> l));

            for (ChapterImportRowDTO row : chapterRows) {
                CourseLesson lesson = lessonNoMap.get(row.getLessonNo());
                boolean isNewLesson = lesson == null;
                if (isNewLesson) {
                    lesson = new CourseLesson();
                    lesson.setChapterId(chapter.getId());
                    lesson.setLessonNo(row.getLessonNo());
                    lesson.setStatus(1);
                }
                lesson.setLessonName(row.getLessonName());
                lesson.setResourceType(row.getResourceType());
                lesson.setResourceUrl(row.getResourceUrl());
                lesson.setDuration(row.getDuration());
                lesson.setContent(row.getContent());
                lesson.setSortOrder(row.getLessonNo());

                if (isNewLesson) {
                    lessonMapper.insert(lesson);
                } else {
                    lessonMapper.updateById(lesson);
                }

                // 导入后 content 为空且是 document 类型，AI 生成通用内容 + 向量化
                if (!StringUtils.hasText(lesson.getContent()) && "document".equalsIgnoreCase(lesson.getResourceType())) {
                    Course course = courseMapper.selectById(courseId);
                    String courseName = course != null ? course.getCourseName() : "";
                    generateContentAndSaveAsync(courseId, courseName, chapter.getId(), lesson.getId(),
                            chapter.getChapterName(), lesson.getLessonName());
                } else if (StringUtils.hasText(lesson.getContent())) {
                    vectorizeLessonAsync(courseId, chapter.getId(), lesson.getId(), lesson.getLessonName(),
                            lesson.getContent(), false);
                }
            }
            result.setLessonCount(result.getLessonCount() + chapterRows.size());
        }

        return result;
    }

    /**
     * 解析 Excel 文件，收集校验错误到结果中
     */
    private List<ChapterImportRowDTO> parseExcel(InputStream input, ChapterImportResultDTO result) {
        List<ChapterImportRowDTO> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                ChapterImportRowDTO dto = new ChapterImportRowDTO();
                dto.setChapterNo(getIntCellValue(row.getCell(0)));
                dto.setChapterName(getStringCellValue(row.getCell(1)));
                dto.setDescription(getStringCellValue(row.getCell(2)));
                dto.setLessonNo(getIntCellValue(row.getCell(3)));
                dto.setLessonName(getStringCellValue(row.getCell(4)));
                dto.setResourceType(getStringCellValue(row.getCell(5)));
                dto.setResourceUrl(getStringCellValue(row.getCell(6)));
                dto.setDuration(getIntCellValue(row.getCell(7)));
                dto.setContent(getStringCellValue(row.getCell(8)));

                if (isEmptyRow(dto)) {
                    continue;
                }

                if (dto.getChapterNo() == null) {
                    result.getFailures().add(new ImportFailureDTO(i + 1, "章节序号不能为空"));
                    continue;
                }
                if (dto.getChapterNo() <= 0) {
                    result.getFailures().add(new ImportFailureDTO(i + 1, "章节序号必须大于0"));
                    continue;
                }
                if (!StringUtils.hasText(dto.getChapterName())) {
                    result.getFailures().add(new ImportFailureDTO(i + 1, "章节名称不能为空"));
                    continue;
                }
                if (dto.getLessonNo() == null) {
                    result.getFailures().add(new ImportFailureDTO(i + 1, "课时序号不能为空"));
                    continue;
                }
                if (dto.getLessonNo() <= 0) {
                    result.getFailures().add(new ImportFailureDTO(i + 1, "课时序号必须大于0"));
                    continue;
                }
                if (!StringUtils.hasText(dto.getLessonName())) {
                    result.getFailures().add(new ImportFailureDTO(i + 1, "课时名称不能为空"));
                    continue;
                }
                if (!StringUtils.hasText(dto.getResourceType())) {
                    dto.setResourceType("video");
                }
                String rt = dto.getResourceType().trim().toLowerCase();
                if (!Arrays.asList("video", "document", "quiz", "link").contains(rt)) {
                    result.getFailures().add(new ImportFailureDTO(i + 1, "资源类型不合法：" + dto.getResourceType()));
                    continue;
                }
                dto.setResourceType(rt);

                rows.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel 解析失败：" + e.getMessage(), e);
        }
        return rows;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                String v = cell.getStringCellValue();
                return v == null ? null : v.trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Integer getIntCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String v = getStringCellValue(cell);
        if (!StringUtils.hasText(v)) {
            return null;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isEmptyRow(ChapterImportRowDTO dto) {
        return dto.getChapterNo() == null
                && !StringUtils.hasText(dto.getChapterName())
                && dto.getLessonNo() == null
                && !StringUtils.hasText(dto.getLessonName());
    }

    /**
     * AI 一键生成整课程章节+课时+内容
     * 同步调用：规划章节结构（一次AI）→ 批量写入 → 每个课时异步向量化
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateCourseChapters(Long courseId, Long userId, boolean isAdmin) {
        if (!canAccessCourse(courseId, userId, isAdmin)) {
            throw new RuntimeException("无权限操作该课程");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }
        String courseName = course.getCourseName();

        // 1. AI 规划章节结构 + 生成课时内容（一次调用返回全部 JSON）
        String plan = generatePlan(courseName);
        if (plan == null || plan.trim().isEmpty()) {
            throw new RuntimeException("AI 生成失败，无返回内容");
        }
        plan = stripJsonMarkers(plan);

        // 2. 解析 JSON 为 chapters 列表
        List<Map<String, Object>> chapters;
        try {
            chapters = objectMapper.readValue(plan,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            System.out.println("=== AI 生成章节解析失败: courseName=" + courseName + ", error=" + e.getMessage());
            throw new RuntimeException("AI 返回格式解析失败：" + e.getMessage());
        }
        if (chapters == null || chapters.isEmpty()) {
            throw new RuntimeException("AI 未返回任何章节");
        }
        System.out.println("=== AI 生成章节解析成功: chapters=" + chapters.size());
        for (int i = 0; i < chapters.size(); i++) {
            Map<String, Object> ch = chapters.get(i);
            Object less = ch.getOrDefault("lessons", ch.get("items"));
            int lessSize = less instanceof List ? ((List<?>) less).size() : 0;
            System.out.println("=== AI 生成章节结构: idx=" + i + ", chapterNo=" + ch.get("chapterNo")
                    + ", chapterName=" + ch.get("chapterName") + ", lessons=" + lessSize);
        }

        // 3. 已存在章节则跳过（课程下已有章节且非空，提示先清空）
        Long existCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM course_chapter WHERE course_id = ? AND deleted = 0",
                Long.class, courseId);
        System.out.println("=== AI 生成章节已存在检查: courseId=" + courseId + ", existCount=" + existCount);
        if (existCount != null && existCount > 0) {
            throw new RuntimeException("该课程已存在 " + existCount + " 个章节，请先删除后再生成");
        }

        // 4. 按章节序遍历，逐章写入
        int chapterCount = 0;
        int lessonCount = 0;
        int lessonNoCursor = 0;
        for (int ci = 0; ci < chapters.size(); ci++) {
            Map<String, Object> ch = chapters.get(ci);
            Number chNoNum = (Number) ch.getOrDefault("chapterNo", ch.get("no"));
            if (chNoNum == null) chNoNum = ci + 1;
            int chapterNo = chNoNum.intValue();
            String chapterName = (String) ch.getOrDefault("chapterName", ch.get("name"));
            if (!StringUtils.hasText(chapterName)) chapterName = "第" + chapterNo + "章";
            String description = (String) ch.getOrDefault("description", "");

            CourseChapter chapter = new CourseChapter();
            chapter.setCourseId(courseId);
            chapter.setChapterNo(chapterNo);
            chapter.setChapterName(chapterName.trim());
            chapter.setDescription(description != null ? description.trim() : "");
            chapter.setSortOrder(chapterNo);
            chapter.setStatus(1);
            chapterMapper.insert(chapter);
            chapterCount++;

            // 5. 写入课时
            Object lessListObj = ch.getOrDefault("lessons", ch.get("items"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lessList = lessListObj instanceof List ? (List<Map<String, Object>>) lessListObj : java.util.Collections.emptyList();
            for (int li = 0; li < lessList.size(); li++) {
                Map<String, Object> ls = lessList.get(li);
                Number lsNoNum = (Number) ls.getOrDefault("lessonNo", ls.get("no"));
                int lessonNo = lsNoNum != null ? lsNoNum.intValue() : li + 1;
                String lessonName = (String) ls.getOrDefault("lessonName", ls.get("name"));
                if (!StringUtils.hasText(lessonName)) lessonName = "第" + lessonNo + "节";
                String content = (String) ls.getOrDefault("content", "");
                if (content == null) content = "";
                content = content.trim();

                CourseLesson lesson = new CourseLesson();
                lesson.setChapterId(chapter.getId());
                lesson.setLessonNo(lessonNo);
                lesson.setLessonName(lessonName.trim());
                lesson.setResourceType("document");
                lesson.setContent(content);
                lesson.setSortOrder(lessonNo);
                lesson.setStatus(1);
                lessonMapper.insert(lesson);
                lessonCount++;

                // 6. 异步向量化（content 非空）
                if (StringUtils.hasText(content)) {
                    vectorizeLessonAsync(courseId, chapter.getId(), lesson.getId(), lesson.getLessonName(), content, true);
                }
            }
        }

        System.out.println("=== AI 生成章节写入完成: courseId=" + courseId + ", chapterCount=" + chapterCount + ", lessonCount=" + lessonCount);
        return Map.of("chapterCount", chapterCount, "lessonCount", lessonCount);
    }

    /**
     * 调用 AI 规划章节结构+内容，返回 JSON 数组
     * JSON 格式：[{chapterNo, chapterName, description, lessons:[{lessonNo, lessonName, content}]}]
     */
    private String generatePlan(String courseName) {
        String systemPrompt = "你是资深大学教学大纲设计师。严格只输出合法 JSON 数组，不要任何解释文字、不要用 Markdown 代码块包裹 JSON。";
        String userPrompt = "请为《" + courseName + "》这门大学课程规划完整章节结构，并同时生成每个课时的正文教学内容。\n" +
                "硬性输出规则（必须严格遵守，否则返回 JSON 非法）：\n" +
                "1）5-8 个章节，每章 2-3 个课时；\n" +
                "2）章节按学习难度递进排序；\n" +
                "3）每个课时 content 写 300-500 字，必须采用 Markdown 格式，要求：\n" +
                "   - 使用二级标题（##）划分 2-3 个核心知识点小节，实现分层结构化；\n" +
                "   - 段落之间必须留空行，禁止大段纯文本堆砌，保持视觉呼吸感；\n" +
                "   - 所有英文函数名、变量名、类名、命令行、代码片段都必须用 Markdown 行内代码（`code`）或 fenced code block（```）包裹，禁止裸写英文函数名；例如 `np.sin`、`np.exp`、`print()`、`arr[arr > 5]`；\n" +
                "   - 较长或较复杂的代码示例必须用 fenced code block（```python ... ```）独占一行或多行，代码块内部除原始代码自带的换行外，严禁自动换行；单行代码必须完整显示在一行内；\n" +
                "   - 列举多个函数、特性、步骤时，必须使用 Markdown 列表（- 或 1.）展示，每个列表项独占一行，列表项中的英文函数名仍用 `code` 包裹；\n" +
                "   - 关键术语使用加粗（**term**），每个知识点讲解后必须换行给出 1 个简短例子；\n" +
                "4）content 中绝对禁止出现未转义的英文双引号（\"）；如需引号请用中文「」或确保 JSON 转义；禁止出现未转义的反斜杠（\\）；\n" +
                "5）content 里如果需要引用代码变量名如 JAVA_HOME，写成 `JAVA_HOME` 或纯文字 JAVA_HOME，不要加英文引号包裹；\n" +
                "6）所有字符串（章节名、课时名、content）中必须把英文双引号替换为中文「」或直接去掉，JSON 关键结构引号除外；\n" +
                "7）只输出合法 JSON 数组，格式示例：\n" +
                "[{\"chapterNo\":1,\"chapterName\":\"章节名\",\"description\":\"简述\",\"lessons\":[{\"lessonNo\":1,\"lessonName\":\"课时名\",\"content\":\"## 知识点一\\n\\n概念说明...\\n\\n例子：...\\n\\n```python\\nprint('hello')\\n```\"}]}]";
        System.out.println("=== AI 生成章节请求: courseName=" + courseName);
        String raw = llmService.chat(systemPrompt, userPrompt);
        System.out.println("=== AI 生成章节原始响应长度=" + (raw == null ? 0 : raw.length()));
        if (raw != null && raw.length() > 200) {
            System.out.println("=== AI 生成章节原始响应前200字=" + raw.substring(0, 200));
        } else if (raw != null) {
            System.out.println("=== AI 生成章节原始响应=" + raw);
        }
        if (raw == null) return null;
        if (raw.length() > 60000) raw = raw.substring(0, 60000);
        // 兜底清洗：JSON 值文本内容中残留的未转义英文 " 改为 「 」
        return sanitizeJsonQuotes(raw);
    }

    /**
     * 容错清洗：把 JSON 值文本内容中残留的未转义英文 " 改为 「 」
     * 启发式：按行遍历，识别 "key":"value" 结构，value 中如果还有多余 " 就替换成「/」交替
     */
    private String sanitizeJsonQuotes(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean isKey = false;
        int valueDepth = 0; // 0=不在value内，1=在简单value内，2+在嵌套value
        int colonAfterKey = 0; // 记录 "key": 冒号位置
        char prev = '\0';
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\"') {
                if (!inString) {
                    inString = true;
                    isKey = true;
                    sb.append(c);
                } else {
                    // 判断是字符串结束还是值内容内部
                    // 向后看非空白下一个字符是否是 : , } ] \n 等 JSON 结构
                    int j = i + 1;
                    while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;
                    char next = j < s.length() ? s.charAt(j) : '\0';
                    // 向前看是否是转义（不应该有，但防护）
                    boolean endOfString = (next == ':' || next == ',' || next == '}' || next == ']'
                            || next == '\n' || next == '\r' || j == s.length());
                    if (endOfString) {
                        inString = false;
                        isKey = false;
                        sb.append(c);
                    } else if (next == '\"' && prev == '\\') {
                        // 已转义，保留
                        sb.append(c);
                    } else {
                        // 值内部残留的 "，替换为 「/」 交替
                        sb.append(valueDepth % 2 == 0 ? '「' : '」');
                        valueDepth++;
                    }
                }
            } else if (c == ':' && inString == false && isKey == false) {
                colonAfterKey = 1;
                sb.append(c);
            } else {
                sb.append(c);
            }
            prev = c;
        }
        return sb.toString();
    }

    /**
     * 剥去 AI 可能输出的 ```json ... ``` 包裹，并补齐截断的 JSON 尾部
     */
    private String stripJsonMarkers(String s) {
        if (s == null) return null;
        int start = s.indexOf("[");
        int end = s.lastIndexOf("]");
        String body;
        if (start < 0) {
            int so = s.indexOf("{");
            int eo = s.lastIndexOf("}");
            if (so < 0 || eo < 0 || so >= eo) return s.trim();
            body = "[" + s.substring(so, eo + 1) + "]";
        } else if (end < 0 || start >= end) {
            // 只有起始 [，后面被截断，尝试从 [ 开始补齐
            body = completeTruncatedJson(s.substring(start));
        } else {
            body = s.substring(start, end + 1);
        }
        // 再校验括号平衡，如不平衡则补全
        return completeTruncatedJson(body);
    }

    /**
     * 补齐 JSON 字符串末尾缺失的 } 和 ]，按计数法闭合。
     * 处理策略：遇到数组/对象内部字符串截断时，先闭合最近的字符串，再逐层补括号。
     */
    private String completeTruncatedJson(String s) {
        if (s == null) return null;
        int objDepth = 0;
        int arrDepth = 0;
        boolean inStr = false;
        boolean esc = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (esc) { esc = false; continue; }
            if (inStr) {
                if (c == '\\') esc = true;
                else if (c == '"') inStr = false;
                continue;
            }
            switch (c) {
                case '"': inStr = true; break;
                case '{': objDepth++; break;
                case '}': if (objDepth > 0) objDepth--; break;
                case '[': arrDepth++; break;
                case ']': if (arrDepth > 0) arrDepth--; break;
            }
        }
        StringBuilder sb = new StringBuilder(s);
        // 若还在字符串内，先闭合字符串
        if (inStr) sb.append('"');
        // 逐层补对象/数组括号
        while (objDepth-- > 0) sb.append('}');
        while (arrDepth-- > 0) sb.append(']');
        return sb.toString();
    }
}
