package com.springboot.module.projectwork.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.common.ErrorCode;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.file.FileResourceMapper;
import com.springboot.mapper.projectwork.*;
import com.springboot.mapper.system.StudentMapper;
import com.springboot.model.dto.projectwork.AssignmentCreateRequest;
import com.springboot.model.dto.projectwork.ReviewRequest;
import com.springboot.model.entity.projectwork.*;
import com.springboot.model.entity.system.Student;
import com.springboot.model.vo.projectwork.*;
import com.springboot.module.projectwork.eval.EvalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectworkService {

    private final ProjectAssignmentMapper assignmentMapper;
    private final ProjectSubmissionMapper submissionMapper;
    private final ProjectGroupMapper groupMapper;
    private final ProjectGroupMemberMapper groupMemberMapper;
    private final AgentEvalReportMapper evalReportMapper;
    private final AgentEvalTaskMapper evalTaskMapper;
    private final ProjectScoreMapper scoreMapper;
    private final StudentMapper studentMapper;
    private final EvalTaskService evalTaskService;
    private final FileResourceMapper fileResourceMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String STORAGE_ROOT = System.getProperty("user.home") + "/storage";

    // =========================================================================
    // Assignment CRUD
    // =========================================================================

    public List<AssignmentVO> listAssignments(Long courseId) {
        List<Map<String, Object>> rows = assignmentMapper.selectWithStatsByCourse(courseId);
        return rows.stream().map(this::mapToAssignmentVO).collect(Collectors.toList());
    }

    public AssignmentVO getAssignment(Long id) {
        ProjectAssignment entity = assignmentMapper.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Assignment not found");
        AssignmentVO vo = new AssignmentVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setRequirement(entity.getRequirement());
        vo.setStatus(entity.getStatus());
        vo.setStartTime(entity.getStartTime());
        vo.setDeadline(entity.getDeadline());
        vo.setFullScore(entity.getFullScore());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setScoreItems(parseScoreItems(entity.getRubricJson()));
        vo.setSubmissionCount(0);
        vo.setEvalCount(0);
        return vo;
    }

    @Transactional
    public Long createAssignment(AssignmentCreateRequest req, Long createdByUserId) {
        ProjectAssignment entity = new ProjectAssignment();
        entity.setCourseId(req.getCourseId() != null ? req.getCourseId() : 1L);
        entity.setTitle(req.getTitle());
        entity.setDescription(req.getDescription());
        entity.setRequirement(req.getRequirement());
        entity.setFullScore(BigDecimal.valueOf(100));
        entity.setStatus("DRAFT");
        entity.setCreatedBy(createdByUserId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (req.getStartTime() != null && !req.getStartTime().isEmpty()) {
            entity.setStartTime(parseLocalDateTime(req.getStartTime()));
        }
        if (req.getDeadline() != null && !req.getDeadline().isEmpty()) {
            entity.setDeadline(parseLocalDateTime(req.getDeadline()));
        }
        entity.setRubricJson(buildRubricJson(req.getScoreItems()));
        assignmentMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void updateAssignment(Long id, AssignmentCreateRequest req) {
        ProjectAssignment entity = assignmentMapper.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Assignment not found");
        entity.setTitle(req.getTitle());
        entity.setDescription(req.getDescription());
        entity.setRequirement(req.getRequirement());
        entity.setUpdatedAt(LocalDateTime.now());
        if (req.getStartTime() != null && !req.getStartTime().isEmpty()) {
            entity.setStartTime(parseLocalDateTime(req.getStartTime()));
        }
        if (req.getDeadline() != null && !req.getDeadline().isEmpty()) {
            entity.setDeadline(parseLocalDateTime(req.getDeadline()));
        }
        entity.setRubricJson(buildRubricJson(req.getScoreItems()));
        assignmentMapper.updateById(entity);
    }

    @Transactional
    public void publishAssignment(Long id) {
        ProjectAssignment entity = assignmentMapper.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Assignment not found");
        entity.setStatus("PUBLISHED");
        entity.setUpdatedAt(LocalDateTime.now());
        assignmentMapper.updateById(entity);
    }

    // =========================================================================
    // Submission management (teacher)
    // =========================================================================

    public List<SubmissionVO> listSubmissions(Long assignmentId) {
        List<Map<String, Object>> rows = submissionMapper.selectDetailsByAssignment(assignmentId);
        Map<Long, Integer> groupNoMap = new LinkedHashMap<>();
        int[] counter = {1};
        List<SubmissionVO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            SubmissionVO vo = mapToSubmissionVO(row);
            if (vo.getGroupId() != null) {
                groupNoMap.computeIfAbsent(vo.getGroupId(), k -> counter[0]++);
                vo.setGroupNo(groupNoMap.get(vo.getGroupId()));
                vo.setMembers(loadMembers(vo.getGroupId(), vo.getSubmitStudentId()));
            }
            result.add(vo);
        }
        return result;
    }

    public SubmissionVO getSubmission(Long assignmentId, Long submissionId) {
        Map<String, Object> row = submissionMapper.selectDetailById(submissionId);
        if (row == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Submission not found");
        SubmissionVO vo = mapToSubmissionVO(row);
        if (vo.getGroupId() != null) {
            vo.setMembers(loadMembers(vo.getGroupId(), vo.getSubmitStudentId()));
        }
        return vo;
    }

    // =========================================================================
    // Student submission upload
    // =========================================================================

    @Transactional
    public Long createSubmission(Long assignmentId, Long userId, MultipartFile zipFile,
                                 List<Long> memberStudentIds) {
        ProjectAssignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Assignment not found");
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Assignment not open for submission");
        }

        Student submitter = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (submitter == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Student not found");

        // Save ZIP file
        Long fileId = saveZipFile(zipFile, assignmentId, userId);

        // Find or create group
        Long groupId = findOrCreateGroup(assignmentId, submitter.getId(), memberStudentIds);

        // Create submission
        ProjectSubmission submission = new ProjectSubmission();
        submission.setAssignmentId(assignmentId);
        submission.setGroupId(groupId);
        submission.setSubmitStudentId(submitter.getId());
        submission.setZipFileId(fileId);
        submission.setSubmitStatus("SUBMITTED");
        submission.setSubmitTime(LocalDateTime.now());
        submission.setCreatedAt(LocalDateTime.now());
        submission.setUpdatedAt(LocalDateTime.now());
        submissionMapper.insert(submission);

        // Init project_score rows for all members
        List<Map<String, Object>> members = groupMemberMapper.selectMembersWithInfo(groupId);
        for (Map<String, Object> m : members) {
            Long sid = toLong(m.get("student_id"));
            LambdaQueryWrapper<ProjectScore> existCheck = new LambdaQueryWrapper<ProjectScore>()
                    .eq(ProjectScore::getAssignmentId, assignmentId)
                    .eq(ProjectScore::getStudentId, sid);
            if (scoreMapper.selectCount(existCheck) == 0) {
                ProjectScore score = new ProjectScore();
                score.setAssignmentId(assignmentId);
                score.setGroupId(groupId);
                score.setStudentId(sid);
                score.setSubmissionId(submission.getId());
                score.setScoreStatus("DRAFT");
                score.setCreatedAt(LocalDateTime.now());
                score.setUpdatedAt(LocalDateTime.now());
                scoreMapper.insert(score);
            }
        }

        return submission.getId();
    }

    // =========================================================================
    // Eval status
    // =========================================================================

    public EvalStatusVO getEvalStatus(Long assignmentId, Long submissionId) {
        LambdaQueryWrapper<AgentEvalTask> qw = new LambdaQueryWrapper<AgentEvalTask>()
                .eq(AgentEvalTask::getRelatedId, submissionId)
                .eq(AgentEvalTask::getTaskType, "PROJECT")
                .orderByDesc(AgentEvalTask::getId)
                .last("LIMIT 1");
        AgentEvalTask task = evalTaskMapper.selectOne(qw);

        ProjectSubmission sub = submissionMapper.selectById(submissionId);

        EvalStatusVO vo = new EvalStatusVO();
        vo.setSubmitStatus(sub != null ? sub.getSubmitStatus() : "SUBMITTED");

        if (task == null) {
            vo.setTaskStatus("NONE");
            return vo;
        }

        vo.setTaskId(task.getId());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setErrorMessage(task.getErrorMessage());

        // Parse steps from output_json
        if (task.getOutputJson() != null) {
            try {
                Map<String, Object> output = objectMapper.readValue(task.getOutputJson(),
                        new TypeReference<Map<String, Object>>() {});
                Object stepsObj = output.get("steps");
                if (stepsObj != null) {
                    List<Map<String, Object>> stepMaps = objectMapper.convertValue(stepsObj,
                            new TypeReference<List<Map<String, Object>>>() {});
                    List<EvalStatusVO.StepVO> steps = stepMaps.stream().map(s -> {
                        EvalStatusVO.StepVO sv = new EvalStatusVO.StepVO();
                        sv.setName(str(s.get("name")));
                        sv.setStatus(str(s.get("status")));
                        sv.setDuration(str(s.get("duration")));
                        sv.setLog(str(s.get("log")));
                        return sv;
                    }).collect(Collectors.toList());
                    vo.setSteps(steps);
                }
                Object shots = output.get("screenshot_urls");
                if (shots instanceof List) {
                    List<EvalReportVO.ScreenshotVO> screenshots = new ArrayList<>();
                    int i = 1;
                    for (Object url : (List<?>) shots) {
                        EvalReportVO.ScreenshotVO s = new EvalReportVO.ScreenshotVO();
                        s.setUrl(url.toString());
                        s.setLabel("截图 " + i++);
                        screenshots.add(s);
                    }
                    vo.setScreenshots(screenshots);
                }
            } catch (Exception e) {
                log.warn("[ProjectworkService] Failed to parse output_json for task {}", task.getId());
            }
        }
        return vo;
    }

    // =========================================================================
    // Eval report
    // =========================================================================

    public EvalReportVO getEvalReport(Long assignmentId, Long submissionId) {
        AgentEvalReport report = evalReportMapper.selectLatestBySubmission(submissionId);
        if (report == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Report not found");

        LambdaQueryWrapper<ProjectScore> sq = new LambdaQueryWrapper<ProjectScore>()
                .eq(ProjectScore::getSubmissionId, submissionId)
                .last("LIMIT 1");
        ProjectScore score = scoreMapper.selectOne(sq);

        EvalReportVO vo = new EvalReportVO();
        vo.setId(report.getId());
        vo.setAgentTaskId(report.getAgentTaskId());
        vo.setProjectSubmissionId(report.getProjectSubmissionId());
        vo.setReportTitle(report.getReportTitle());
        vo.setSummary(report.getSummary());
        vo.setAdvantage(report.getAdvantage());
        vo.setProblem(report.getProblem());
        vo.setSuggestion(report.getSuggestion());
        vo.setAgentScore(report.getAgentScore());
        vo.setReportJson(report.getReportJson());

        // Parse report_json for dimensions / pros / cons / screenshots
        if (report.getReportJson() != null) {
            try {
                Map<String, Object> rj = objectMapper.readValue(report.getReportJson(),
                        new TypeReference<Map<String, Object>>() {});
                vo.setDimensions(parseDimensions(rj.get("dimensions")));
                vo.setPros(parseStringList(rj.get("advantage")));
                vo.setCons(parseStringList(rj.get("problem")));
            } catch (Exception e) {
                log.warn("[ProjectworkService] Failed to parse report_json for report {}", report.getId());
            }
        }

        // Fallback: parse advantage / problem text as bullet lines
        if (vo.getPros() == null || vo.getPros().isEmpty()) {
            vo.setPros(splitLines(report.getAdvantage()));
        }
        if (vo.getCons() == null || vo.getCons().isEmpty()) {
            vo.setCons(splitLines(report.getProblem()));
        }

        // Get screenshots from eval task output_json
        if (report.getAgentTaskId() != null) {
            AgentEvalTask task = evalTaskMapper.selectById(report.getAgentTaskId());
            if (task != null && task.getOutputJson() != null) {
                try {
                    Map<String, Object> out = objectMapper.readValue(task.getOutputJson(),
                            new TypeReference<Map<String, Object>>() {});
                    Object shots = out.get("screenshot_urls");
                    if (shots instanceof List) {
                        List<EvalReportVO.ScreenshotVO> screenshots = new ArrayList<>();
                        int i = 1;
                        for (Object u : (List<?>) shots) {
                            EvalReportVO.ScreenshotVO s = new EvalReportVO.ScreenshotVO();
                            s.setUrl(u.toString());
                            s.setLabel("截图 " + i++);
                            screenshots.add(s);
                        }
                        vo.setScreenshots(screenshots);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Teacher review
        if (score != null) {
            vo.setReviewed("REVIEWED".equals(score.getScoreStatus()) || "CONFIRMED".equals(score.getScoreStatus()));
            vo.setTeacherScore(score.getTeacherScore());
            vo.setReviewComment(score.getReviewComment());
            vo.setScoreStatus(score.getScoreStatus());
        }

        return vo;
    }

    // =========================================================================
    // Teacher review
    // =========================================================================

    @Transactional
    public void saveReview(Long assignmentId, Long submissionId, ReviewRequest req) {
        ProjectSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Submission not found");

        LambdaQueryWrapper<ProjectScore> sq = new LambdaQueryWrapper<ProjectScore>()
                .eq(ProjectScore::getSubmissionId, submissionId)
                .last("LIMIT 1");
        ProjectScore score = scoreMapper.selectOne(sq);

        if (score == null) {
            score = new ProjectScore();
            score.setAssignmentId(assignmentId);
            score.setStudentId(sub.getSubmitStudentId());
            score.setGroupId(sub.getGroupId());
            score.setSubmissionId(submissionId);
            score.setCreatedAt(LocalDateTime.now());
        }
        score.setTeacherScore(req.getTeacherScore());
        score.setFinalScore(req.getTeacherScore());
        score.setReviewComment(req.getReviewComment());
        score.setScoreStatus("REVIEWED");
        score.setUpdatedAt(LocalDateTime.now());

        if (score.getId() == null) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }

        sub.setSubmitStatus("REVIEWED");
        sub.setUpdatedAt(LocalDateTime.now());
        submissionMapper.updateById(sub);
    }

    // =========================================================================
    // Grades summary
    // =========================================================================

    public List<GradeVO> listGrades(Long assignmentId) {
        List<Map<String, Object>> rows = scoreMapper.selectGradesByAssignment(assignmentId);
        List<GradeVO> result = new ArrayList<>();
        int groupCounter = 1;
        Map<Long, Integer> groupNoMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long gid = toLong(row.get("group_id"));
            if (gid != null && !groupNoMap.containsKey(gid)) {
                groupNoMap.put(gid, groupCounter++);
            }

            GradeVO vo = new GradeVO();
            vo.setStudentId(toLong(row.get("student_id")));
            vo.setStudentNo(str(row.get("student_no")));
            vo.setStudentName(str(row.get("student_name")));
            vo.setGroupId(gid);
            vo.setGroupNo(gid != null ? groupNoMap.get(gid) : null);
            vo.setSubmissionId(toLong(row.get("submission_id")));
            vo.setAgentScore(toBigDecimal(row.get("agent_score")));
            vo.setTeacherScore(toBigDecimal(row.get("teacher_score")));
            vo.setFinalScore(toBigDecimal(row.get("final_score")));
            vo.setScoreStatus(str(row.get("score_status")));
            vo.setReviewComment(str(row.get("review_comment")));
            result.add(vo);
        }
        return result;
    }

    // =========================================================================
    // Student view
    // =========================================================================

    public AssignmentVO getStudentAssignment(Long assignmentId) {
        return getAssignment(assignmentId);
    }

    public SubmissionVO getStudentSubmission(Long assignmentId, Long userId) {
        Long submissionId = submissionMapper.findLatestIdByAssignmentAndUser(assignmentId, userId);
        if (submissionId == null) return null;
        Map<String, Object> row = submissionMapper.selectDetailById(submissionId);
        if (row == null) return null;
        SubmissionVO vo = mapToSubmissionVO(row);
        if (vo.getGroupId() != null) {
            vo.setMembers(loadMembers(vo.getGroupId(), vo.getSubmitStudentId()));
        }
        return vo;
    }

    public EvalReportVO getStudentReport(Long assignmentId, Long userId) {
        Long submissionId = submissionMapper.findLatestIdByAssignmentAndUser(assignmentId, userId);
        if (submissionId == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "No submission found");
        return getEvalReport(assignmentId, submissionId);
    }

    // =========================================================================
    // Student search
    // =========================================================================

    public List<StudentSearchVO> searchStudents(String query) {
        LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<Student>()
                .and(w -> w.like(Student::getStudentName, query).or().like(Student::getStudentNo, query))
                .last("LIMIT 20");
        return studentMapper.selectList(qw).stream().map(s -> {
            StudentSearchVO vo = new StudentSearchVO();
            vo.setId(s.getId());
            vo.setStudentNo(s.getStudentNo());
            vo.setStudentName(s.getStudentName());
            vo.setGender(s.getGender());
            return vo;
        }).collect(Collectors.toList());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Long saveZipFile(MultipartFile file, Long assignmentId, Long userId) {
        try {
            String dir = STORAGE_ROOT + "/uploads/project_zip/" + assignmentId;
            Files.createDirectories(Paths.get(dir));
            String storedName = System.currentTimeMillis() + "_" + userId + ".zip";
            Path dest = Paths.get(dir, storedName);
            file.transferTo(dest.toFile());
            com.springboot.model.entity.file.FileResource fr = new com.springboot.model.entity.file.FileResource();
            fr.setBizType("PROJECT_ZIP");
            fr.setOriginalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : storedName);
            fr.setStoredName(storedName);
            fr.setFileExt("zip");
            fr.setMimeType("application/zip");
            fr.setFileSize(file.getSize());
            fr.setStoragePath(dest.toString());
            fr.setUploadUserId(userId);
            fr.setCreatedAt(new Date());
            fileResourceMapper.insert(fr);
            return fr.getId();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save ZIP: " + e.getMessage());
        }
    }

    private Long findOrCreateGroup(Long assignmentId, Long leaderId, List<Long> memberIds) {
        // Create new group
        ProjectGroup group = new ProjectGroup();
        group.setAssignmentId(assignmentId);
        group.setGroupName("小组");
        group.setLeaderStudentId(leaderId);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        groupMapper.insert(group);
        Long groupId = group.getId();

        // Add leader
        Set<Long> allMembers = new LinkedHashSet<>();
        allMembers.add(leaderId);
        if (memberIds != null) allMembers.addAll(memberIds);

        for (Long sid : allMembers) {
            LambdaQueryWrapper<ProjectGroupMember> exists = new LambdaQueryWrapper<ProjectGroupMember>()
                    .eq(ProjectGroupMember::getGroupId, groupId)
                    .eq(ProjectGroupMember::getStudentId, sid);
            if (groupMemberMapper.selectCount(exists) == 0) {
                ProjectGroupMember member = new ProjectGroupMember();
                member.setGroupId(groupId);
                member.setStudentId(sid);
                member.setRoleName(sid.equals(leaderId) ? "组长" : "组员");
                member.setContributionRatio(BigDecimal.ONE);
                member.setCreatedAt(LocalDateTime.now());
                groupMemberMapper.insert(member);
            }
        }
        return groupId;
    }

    private List<SubmissionVO.MemberVO> loadMembers(Long groupId, Long leaderId) {
        List<Map<String, Object>> rows = groupMemberMapper.selectMembersWithInfo(groupId);
        return rows.stream().map(r -> {
            SubmissionVO.MemberVO m = new SubmissionVO.MemberVO();
            m.setStudentId(toLong(r.get("student_id")));
            m.setStudentName(str(r.get("student_name")));
            m.setStudentNo(str(r.get("student_no")));
            m.setRoleName(str(r.get("role_name")));
            m.setLeader(Objects.equals(m.getStudentId(), leaderId));
            return m;
        }).collect(Collectors.toList());
    }

    private SubmissionVO mapToSubmissionVO(Map<String, Object> row) {
        SubmissionVO vo = new SubmissionVO();
        vo.setId(toLong(row.get("id")));
        vo.setAssignmentId(toLong(row.get("assignment_id")));
        vo.setGroupId(toLong(row.get("group_id")));
        Object gnObj = row.get("group_no");
        if (gnObj != null) {
            try { vo.setGroupNo(Integer.parseInt(gnObj.toString())); } catch (Exception ignored) {}
        }
        vo.setSubmitStudentId(toLong(row.get("submit_student_id")));
        vo.setZipFileId(toLong(row.get("zip_file_id")));
        vo.setFileName(str(row.get("file_name")));
        vo.setFileSize(formatFileSize(row.get("file_size")));
        vo.setSubmitStatus(str(row.get("submit_status")));
        Object submitTime = row.get("submit_time");
        if (submitTime instanceof java.sql.Timestamp) {
            vo.setSubmitTime(new Date(((java.sql.Timestamp) submitTime).getTime()));
        }
        vo.setRunLog(str(row.get("run_log")));
        vo.setErrorMessage(str(row.get("error_message")));
        vo.setEvalTaskId(toLong(row.get("eval_task_id")));
        vo.setEvalStatus(str(row.get("eval_status")));
        vo.setEvalOutputJson(str(row.get("eval_output_json")));
        vo.setAgentScore(toBigDecimal(row.get("agent_score")));
        vo.setTeacherScore(toBigDecimal(row.get("teacher_score")));
        vo.setReviewComment(str(row.get("review_comment")));
        vo.setScoreStatus(str(row.get("score_status")));
        return vo;
    }

    private AssignmentVO mapToAssignmentVO(Map<String, Object> row) {
        AssignmentVO vo = new AssignmentVO();
        vo.setId(toLong(row.get("id")));
        vo.setCourseId(toLong(row.get("course_id")));
        vo.setTitle(str(row.get("title")));
        vo.setDescription(str(row.get("description")));
        vo.setRequirement(str(row.get("requirement")));
        vo.setStatus(str(row.get("status")));
        vo.setFullScore(toBigDecimal(row.get("full_score")));
        Object st = row.get("start_time");
        Object dl = row.get("deadline");
        Object cat = row.get("created_at");
        Object uat = row.get("updated_at");
        if (st instanceof java.sql.Timestamp) vo.setStartTime(((java.sql.Timestamp) st).toLocalDateTime());
        if (dl instanceof java.sql.Timestamp) vo.setDeadline(((java.sql.Timestamp) dl).toLocalDateTime());
        if (cat instanceof java.sql.Timestamp) vo.setCreatedAt(((java.sql.Timestamp) cat).toLocalDateTime());
        if (uat instanceof java.sql.Timestamp) vo.setUpdatedAt(((java.sql.Timestamp) uat).toLocalDateTime());
        Object sc = row.get("submission_count");
        Object ec = row.get("eval_count");
        vo.setSubmissionCount(sc != null ? Integer.parseInt(sc.toString()) : 0);
        vo.setEvalCount(ec != null ? Integer.parseInt(ec.toString()) : 0);
        vo.setScoreItems(parseScoreItems(str(row.get("rubric_json"))));
        return vo;
    }

    private List<AssignmentVO.ScoreItemVO> parseScoreItems(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            return list.stream().map(m -> {
                AssignmentVO.ScoreItemVO item = new AssignmentVO.ScoreItemVO();
                item.setName(str(m.get("name")));
                Object w = m.get("weight");
                item.setWeight(w != null ? Integer.parseInt(w.toString()) : 0);
                return item;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String buildRubricJson(List<AssignmentCreateRequest.ScoreItemDTO> items) {
        if (items == null || items.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<EvalReportVO.DimensionVO> parseDimensions(Object obj) {
        if (obj == null) return new ArrayList<>();
        try {
            List<Map<String, Object>> list = objectMapper.convertValue(obj,
                    new TypeReference<List<Map<String, Object>>>() {});
            return list.stream().map(m -> {
                EvalReportVO.DimensionVO d = new EvalReportVO.DimensionVO();
                d.setName(str(m.get("name")));
                Object sc = m.get("score");
                d.setScore(sc != null ? new BigDecimal(sc.toString()) : BigDecimal.ZERO);
                Object tot = m.get("total");
                d.setTotal(tot != null ? Integer.parseInt(tot.toString()) : 100);
                d.setReason(str(m.get("reason")));
                return d;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object obj) {
        if (obj == null) return new ArrayList<>();
        if (obj instanceof List) return (List<String>) obj;
        if (obj instanceof String) return splitLines((String) obj);
        return new ArrayList<>();
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        return Arrays.stream(text.split("[\\n。；;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private LocalDateTime parseLocalDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.replace("T", " "),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private String formatFileSize(Object bytes) {
        if (bytes == null) return null;
        try {
            long b = Long.parseLong(bytes.toString());
            if (b >= 1024 * 1024) return String.format("%.1f MB", b / (1024.0 * 1024));
            if (b >= 1024) return String.format("%.1f KB", b / 1024.0);
            return b + " B";
        } catch (Exception e) {
            return bytes.toString();
        }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }

    private String str(Object v) {
        return v == null ? null : v.toString();
    }
}
