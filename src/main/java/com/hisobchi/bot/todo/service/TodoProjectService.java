package com.hisobchi.bot.todo.service;

import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.common.exception.UnauthorizedAccessException;
import com.hisobchi.bot.todo.dto.ProjectSummaryDto;
import com.hisobchi.bot.todo.dto.TodoTaskDto;
import com.hisobchi.bot.todo.entity.TodoProject;
import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.entity.TodoSubtask;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.repository.TodoProjectRepository;
import com.hisobchi.bot.todo.repository.TodoSubtaskRepository;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoProjectService {

    private final TodoProjectRepository projectRepository;
    private final TodoTaskRepository taskRepository;
    private final TodoSubtaskRepository subtaskRepository;
    private final TodoService todoService;

    @Transactional
    public TodoProject createProject(User user, String name, String description, String color) {
        TodoProject project = TodoProject.builder()
                .user(user)
                .name(name.trim())
                .description(description)
                .color(color != null && !color.isBlank() ? color : "📁")
                .archived(false)
                .build();
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectSummaryDto> getUserProjects(Long userId) {
        List<TodoProject> projects = projectRepository.findByUserIdAndArchivedFalseOrderByCreatedAtDesc(userId);
        return projects.stream().map(p -> {
            long total = taskRepository.countByProjectId(p.getId());
            long completed = taskRepository.countByProjectIdAndStatus(p.getId(), TodoStatus.COMPLETED);
            BigDecimal planned = taskRepository.sumPlannedAmountByProjectId(p.getId());
            BigDecimal actual = taskRepository.sumActualAmountByProjectId(p.getId());

            return ProjectSummaryDto.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .description(p.getDescription())
                    .color(p.getColor())
                    .totalTasks(total)
                    .completedTasks(completed)
                    .plannedAmount(planned)
                    .actualAmount(actual)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<TodoTaskDto> getTasksByProject(Long projectId, Long userId, ZoneId zoneId) {
        getProjectOrThrow(projectId, userId);
        List<TodoTask> tasks = taskRepository.findByUserIdAndProjectIdAndStatusInOrderByDueDateAscCreatedAtDesc(
                userId, projectId, List.of(TodoStatus.OPEN, TodoStatus.IN_PROGRESS, TodoStatus.COMPLETED));
        LocalDate today = LocalDate.now(zoneId);
        LocalTime now = LocalTime.now(zoneId);
        return tasks.stream().map(t -> todoService.toDto(t, today, now)).toList();
    }

    @Transactional
    public TodoSubtask addSubtask(Long taskId, Long userId, String title) {
        TodoTask task = todoService.getTaskOrThrow(taskId, userId);
        long currentCount = subtaskRepository.countByTaskId(taskId);
        TodoSubtask subtask = TodoSubtask.builder()
                .task(task)
                .title(title.trim())
                .completed(false)
                .orderIndex((int) currentCount + 1)
                .build();
        return subtaskRepository.save(subtask);
    }

    @Transactional
    public TodoSubtask toggleSubtask(Long subtaskId, Long userId) {
        TodoSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new EntityNotFoundException("Kichik vazifa topilmadi: " + subtaskId));
        if (!subtask.getTask().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Kirish huquqiga ega emassiz.");
        }
        subtask.setCompleted(!subtask.getCompleted());
        return subtaskRepository.save(subtask);
    }

    @Transactional
    public void deleteSubtask(Long subtaskId, Long userId) {
        TodoSubtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new EntityNotFoundException("Kichik vazifa topilmadi: " + subtaskId));
        if (!subtask.getTask().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Kirish huquqiga ega emassiz.");
        }
        subtaskRepository.delete(subtask);
    }

    @Transactional(readOnly = true)
    public List<TodoSubtask> getSubtasks(Long taskId, Long userId) {
        todoService.getTaskOrThrow(taskId, userId);
        return subtaskRepository.findByTaskIdOrderByOrderIndexAsc(taskId);
    }

    public TodoProject getProjectOrThrow(Long projectId, Long userId) {
        TodoProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Loyiha topilmadi: " + projectId));
        if (!project.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("Ushbu loyihaga kirish huquqingiz yo‘q.");
        }
        return project;
    }
}
