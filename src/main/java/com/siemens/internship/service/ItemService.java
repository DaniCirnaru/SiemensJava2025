package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Revised ItemService with proper asynchronous processing and thread safety.
 * Original issues:
 * - Method returned a List<Item> immediately before any async tasks completed.
 * - Shared mutable fields (processedItems, processedCount) without synchronization.
 * - Used a static ExecutorService instead of Spring's managed TaskExecutor.
 * - No error propagation: exceptions were caught and only printed.
 * - Incorrect use of @Async: method signature did not return Future or CompletableFuture.
 * Changes:
 * 1. Method signature changed to return CompletableFuture<List<Item>>, so Spring
 * recognizes and handles it as an async operation.
 * 2. Injected Spring's AsyncTaskExecutor instead of manual ExecutorService.
 * 3. Local thread-safe list (Collections.synchronizedList) to collect processed items.
 * 4. Composed individual CompletableFutures and used CompletableFuture.allOf to wait
 * for all tasks before completing the returned future.
 * 5. Exceptions during processing now cause the CompletableFuture to complete exceptionally,
 * ensuring errors are propagated to the caller.
 */
@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    /**
     * Asynchronously processes all items and returns a CompletableFuture
     * that completes when all items have been processed.
     *
     * @return CompletableFuture<List < Item>> containing all processed items
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // Thread-safe list to collect processed items
        List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());

        // Create a CompletableFuture for each item processing task
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Long id : itemIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processSingleItem(id, processedItems), taskExecutor);
            futures.add(future);
        }

        // Combine all futures: complete when all individual tasks are done
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems);
    }

    /**
     * Processes a single item: loads it, updates its status, saves it,
     * and adds the result to the provided list.
     * Runtime exceptions will cause the CompletableFuture to complete exceptionally.
     *
     * @param id             the ID of the item to process
     * @param processedItems the collection to which the processed item is added
     */
    private void processSingleItem(Long id, List<Item> processedItems) {
        try {
            Thread.sleep(100);

            Item item = itemRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Item not found: " + id));

            item.setStatus("PROCESSED");
            Item savedItem = itemRepository.save(item);

            processedItems.add(savedItem);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted for item " + id, e);
        }
    }
}
