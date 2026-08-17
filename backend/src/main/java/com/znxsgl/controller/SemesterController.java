package com.znxsgl.controller;

import com.znxsgl.dto.SemesterDTO;
import com.znxsgl.dto.SemesterSaveRequest;
import com.znxsgl.service.SemesterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学期管理接口
 *
 * 权限说明：
 * - 学生/教师：仅可查询当前学期和学期列表（用于显示课表、判断假期）
 * - 管理员：可新增学期、切换当前学期、删除学期
 *
 * 切换学期的操作通常在新学期开学前 1-2 周由管理员执行。
 */
@RestController
@RequestMapping("/api/semester")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    /**
     * 查询当前学期（含状态和提示语）。
     * 所有人可访问。
     *
     * 响应示例（假期）：
     * {
     *   "name": "2026-2027-1",
     *   "startDate": "2026-09-01",
     *   "status": "before",
     *   "notice": "距开学还有 39 天（2026-09-01 开学）"
     * }
     */
    @GetMapping("/current")
    public ResponseEntity<SemesterDTO> getCurrent() {
        SemesterDTO current = semesterService.getCurrentSemesterWithStatus();
        return ResponseEntity.ok(current);
    }

    /**
     * 查询所有学期列表（按开学日期倒序）。
     * 学生/教师/管理员均可访问，用于查看历史和未来学期。
     */
    @GetMapping("/list")
    public ResponseEntity<List<SemesterDTO>> list() {
        return ResponseEntity.ok(semesterService.listAll());
    }

    /**
     * 新增学期。
     * 仅管理员可调用。新增的学期默认 is_current=0，需后续手动切换为当前。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody SemesterSaveRequest request) {
        Long id = semesterService.create(request);
        return ResponseEntity.ok(Map.of("id", id, "message", "学期创建成功"));
    }

    /**
     * 修改学期信息。
     * 仅管理员可调用。
     */
    @PutMapping("/{semesterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> update(@PathVariable Long semesterId,
                                                      @Valid @RequestBody SemesterSaveRequest request) {
        semesterService.update(semesterId, request);
        return ResponseEntity.ok(Map.of("message", "学期信息已更新"));
    }

    /**
     * 切换当前学期（开启新学期）。
     * 仅管理员可调用。
     *
     * 切换后：
     * - 学生端课表自动切换到新学期数据
     * - 旧学期数据保留可查（通过历史学期接口访问）
     * - 课程-班级关联、课表、聊天记录按 semester 字段隔离
     */
    @PutMapping("/switch/{semesterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> switchCurrent(@PathVariable Long semesterId) {
        semesterService.switchCurrent(semesterId);
        return ResponseEntity.ok(Map.of("message", "当前学期已切换"));
    }

    /**
     * 删除学期。禁止删除当前生效的学期。
     * 仅管理员可调用。
     */
    @DeleteMapping("/{semesterId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long semesterId) {
        semesterService.delete(semesterId);
        return ResponseEntity.ok(Map.of("message", "学期已删除"));
    }
}
