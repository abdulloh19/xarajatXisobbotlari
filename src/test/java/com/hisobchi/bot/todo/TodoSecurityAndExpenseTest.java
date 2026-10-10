package com.hisobchi.bot.todo;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.service.CategoryService;
import com.hisobchi.bot.common.exception.UnauthorizedAccessException;
import com.hisobchi.bot.todo.entity.TodoStatus;
import com.hisobchi.bot.todo.entity.TodoTask;
import com.hisobchi.bot.todo.repository.TodoProjectRepository;
import com.hisobchi.bot.todo.repository.TodoSubtaskRepository;
import com.hisobchi.bot.todo.repository.TodoTaskRepository;
import com.hisobchi.bot.todo.service.TodoService;
import com.hisobchi.bot.transaction.dto.TransactionDto;
import com.hisobchi.bot.transaction.entity.Transaction;
import com.hisobchi.bot.transaction.entity.TransactionSource;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.transaction.service.TransactionService;
import com.hisobchi.bot.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoSecurityAndExpenseTest {

    @Mock
    private TodoTaskRepository taskRepository;
    @Mock
    private TodoSubtaskRepository subtaskRepository;
    @Mock
    private TodoProjectRepository projectRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TodoService todoService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).timezone("Asia/Tashkent").build();
        user2 = User.builder().id(2L).timezone("Asia/Tashkent").build();
    }

    @Test
    @DisplayName("User A cannot access or complete User B's task (Security check)")
    void testUnauthorizedAccessThrowsException() {
        TodoTask user2Task = TodoTask.builder()
                .id(99L)
                .user(user2)
                .title("User 2 secret task")
                .status(TodoStatus.OPEN)
                .build();

        when(taskRepository.findById(99L)).thenReturn(Optional.of(user2Task));

        assertThrows(UnauthorizedAccessException.class, () -> todoService.markCompleted(99L, 1L));
        assertThrows(UnauthorizedAccessException.class, () -> todoService.deleteTask(99L, 1L));
        assertThrows(UnauthorizedAccessException.class, () -> todoService.getTaskOrThrow(99L, 1L));
    }

    @Test
    @DisplayName("Convert planned amount to actual expense creates transaction and is idempotent")
    void testRecordTaskExpenseIdempotency() {
        Category cat = Category.builder().id(5L).name("Internet").emoji("🌐").build();
        TodoTask task = TodoTask.builder()
                .id(100L)
                .user(user1)
                .title("Internet to‘lash")
                .plannedAmount(new BigDecimal("150000"))
                .category(cat)
                .status(TodoStatus.COMPLETED)
                .build();

        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));

        Transaction createdTx = Transaction.builder()
                .id(501L)
                .user(user1)
                .amount(new BigDecimal("150000"))
                .type(TransactionType.EXPENSE)
                .category(cat)
                .transactionDate(LocalDate.now())
                .build();

        when(transactionService.createTransaction(eq(user1), eq(TransactionType.EXPENSE), eq(new BigDecimal("150000")), eq(cat), any(), eq(TransactionSource.MANUAL), any()))
                .thenReturn(new TransactionDto(501L, 1L, 5L, "Internet", "🌐", TransactionType.EXPENSE, new BigDecimal("150000"), "UZS", "desc", TransactionSource.MANUAL, LocalDate.now(), null));
        when(transactionService.getByIdAndUser(501L, 1L)).thenReturn(createdTx);

        // 1st call: records expense
        Transaction tx1 = todoService.recordTaskExpense(100L, 1L, null, null);
        assertNotNull(tx1);
        assertEquals(501L, tx1.getId());
        assertEquals(task.getLinkedTransaction(), tx1);
        assertEquals(new BigDecimal("150000"), task.getActualAmount());

        // 2nd call: should return existing linked transaction without creating another one
        Transaction tx2 = todoService.recordTaskExpense(100L, 1L, null, null);
        assertEquals(tx1, tx2);
        verify(transactionService, times(1)).createTransaction(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("User can modify actual amount before recording expense")
    void testRecordExpenseWithEditedAmount() {
        Category cat = Category.builder().id(5L).name("Internet").emoji("🌐").build();
        TodoTask task = TodoTask.builder()
                .id(101L)
                .user(user1)
                .title("Internet to‘lash")
                .plannedAmount(new BigDecimal("150000"))
                .category(cat)
                .status(TodoStatus.COMPLETED)
                .build();

        when(taskRepository.findById(101L)).thenReturn(Optional.of(task));

        Transaction createdTx = Transaction.builder()
                .id(502L)
                .user(user1)
                .amount(new BigDecimal("130000"))
                .type(TransactionType.EXPENSE)
                .category(cat)
                .build();

        when(transactionService.createTransaction(eq(user1), eq(TransactionType.EXPENSE), eq(new BigDecimal("130000")), eq(cat), any(), eq(TransactionSource.MANUAL), any()))
                .thenReturn(new TransactionDto(502L, 1L, 5L, "Internet", "🌐", TransactionType.EXPENSE, new BigDecimal("130000"), "UZS", "desc", TransactionSource.MANUAL, LocalDate.now(), null));
        when(transactionService.getByIdAndUser(502L, 1L)).thenReturn(createdTx);

        // User edited amount to 130 000
        Transaction tx = todoService.recordTaskExpense(101L, 1L, new BigDecimal("130000"), null);
        assertEquals(new BigDecimal("130000"), tx.getAmount());
        assertEquals(new BigDecimal("130000"), task.getActualAmount());
    }

    @Test
    @DisplayName("Undo task expense cleanly reverses transaction and unlinks")
    void testUndoTaskExpense() {
        Transaction tx = Transaction.builder().id(777L).build();
        TodoTask task = TodoTask.builder()
                .id(102L)
                .user(user1)
                .linkedTransaction(tx)
                .actualAmount(new BigDecimal("150000"))
                .build();

        when(taskRepository.findById(102L)).thenReturn(Optional.of(task));

        todoService.undoTaskExpense(102L, 1L);

        assertNull(task.getLinkedTransaction());
        assertNull(task.getActualAmount());
        verify(transactionService, times(1)).deleteTransaction(777L, 1L);
    }
}
