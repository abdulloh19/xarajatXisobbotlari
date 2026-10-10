package com.hisobchi.bot.todo;

import com.hisobchi.bot.todo.entity.TodoRecurrenceType;
import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.repository.TodoProjectRepository;
import com.hisobchi.bot.todo.repository.TodoSubtaskRepository;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.todo.service.TodoService;
import com.hisobchi.bot.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoRecurrenceTest {

    @Mock
    private TodoTaskRepository taskRepository;
    @Mock
    private TodoSubtaskRepository subtaskRepository;
    @Mock
    private TodoProjectRepository projectRepository;

    @InjectMocks
    private TodoService todoService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .timezone("Asia/Tashkent")
                .build();
    }

    @Test
    @DisplayName("Completing daily recurring task generates next occurrence tomorrow")
    void testDailyRecurrenceGeneration() {
        LocalDate today = LocalDate.of(2026, 10, 10);
        TodoTask current = TodoTask.builder()
                .id(1L)
                .user(testUser)
                .title("Ertalabki badantarbiya")
                .dueDate(today)
                .status(TodoStatus.OPEN)
                .recurrenceType(TodoRecurrenceType.DAILY)
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(current));
        when(taskRepository.save(any(TodoTask.class))).thenAnswer(i -> i.getArgument(0));

        todoService.markCompleted(1L, 1L);

        // Verify taskRepository.save called twice: 1 for completing current, 1 for saving next occurrence
        ArgumentCaptor<TodoTask> captor = ArgumentCaptor.forClass(TodoTask.class);
        verify(taskRepository, times(2)).save(captor.capture());

        TodoTask nextOccurrence = captor.getAllValues().get(1);
        assertEquals(TodoStatus.OPEN, nextOccurrence.getStatus());
        assertEquals(LocalDate.of(2026, 10, 11), nextOccurrence.getDueDate());
        assertEquals("Ertalabki badantarbiya", nextOccurrence.getTitle());
    }

    @Test
    @DisplayName("Monthly recurring task on 31st safely resolves to 28th in February (no DateTimeException)")
    void testMonthlyRecurrenceShortMonth() {
        LocalDate jan31 = LocalDate.of(2026, 1, 31);
        TodoTask current = TodoTask.builder()
                .id(2L)
                .user(testUser)
                .title("Oylik hisobot topshirish")
                .dueDate(jan31)
                .status(TodoStatus.OPEN)
                .recurrenceType(TodoRecurrenceType.MONTHLY)
                .recurrenceDayOfMonth(31)
                .plannedAmount(new BigDecimal("500000"))
                .build();

        when(taskRepository.findById(2L)).thenReturn(Optional.of(current));
        when(taskRepository.save(any(TodoTask.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> todoService.markCompleted(2L, 1L));

        ArgumentCaptor<TodoTask> captor = ArgumentCaptor.forClass(TodoTask.class);
        verify(taskRepository, times(2)).save(captor.capture());

        TodoTask nextOccurrence = captor.getAllValues().get(1);
        assertEquals(TodoStatus.OPEN, nextOccurrence.getStatus());
        // February 2026 has 28 days -> safely resolves to 28
        assertEquals(LocalDate.of(2026, 2, 28), nextOccurrence.getDueDate());
        assertEquals(new BigDecimal("500000"), nextOccurrence.getPlannedAmount());
    }
}
