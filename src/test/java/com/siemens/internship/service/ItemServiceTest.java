package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.task.AsyncTaskExecutor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock
    private ItemRepository itemRepository;

    private ExecutorService executorService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this).close();

        executorService = Executors.newFixedThreadPool(2);
        AsyncTaskExecutor mockExecutor = executorService::execute;

        Field executorField = ItemService.class.getDeclaredField("taskExecutor");
        executorField.setAccessible(true);
        executorField.set(itemService, mockExecutor);
    }

    @AfterEach
    void tearDown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // or shutdown()
        }
    }

    private Item mockItem(Long id) {
        return new Item(
                id,
                "Test name",
                "Test description",
                "NEW",
                "test@example.com"
        );
    }

    @Test
    void testFindAll() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(mockItem(1L), mockItem(2L)));

        List<Item> result = itemService.findAll();
        assertEquals(2, result.size());
    }

    @Test
    void testFindById() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(mockItem(1L)));

        Optional<Item> result = itemService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("Test name", result.get().getName());
    }

    @Test
    void testSave() {
        Item item = mockItem(1L);
        when(itemRepository.save(item)).thenReturn(item);

        Item saved = itemService.save(item);
        assertEquals("Test name", saved.getName());
    }

    @Test
    void testDeleteById() {
        itemService.deleteById(1L);
        verify(itemRepository, times(1)).deleteById(1L);
    }

    @Test
    void testProcessItemsAsync_success() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(List.of(1L));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(mockItem(1L)));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get();

        assertEquals(1, result.size());
        assertEquals("PROCESSED", result.get(0).getStatus());
    }

    @Test
    void testProcessItemsAsync_notFound() {
        when(itemRepository.findAllIds()).thenReturn(List.of(1L));
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        Exception ex = assertThrows(Exception.class, future::get);
        assertInstanceOf(EntityNotFoundException.class, ex.getCause());
    }
}
