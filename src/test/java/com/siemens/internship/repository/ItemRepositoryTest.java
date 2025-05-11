package com.siemens.internship.repository;

import com.siemens.internship.model.Item;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void testFindAllIds() {
        // Given: save a few items
        Item item1 = new Item(1L, "Item 1", "First test item", "NEW", "item1@example.com");
        Item item2 = new Item(2L, "Item 2", "Second test item", "NEW", "item2@example.com");

        item1 = itemRepository.save(item1);
        item2 = itemRepository.save(item2);

        // When: calling the custom query
        List<Long> ids = itemRepository.findAllIds();

        // Then: should contain only the stored IDs
        assertThat(ids).hasSize(2);
        assertThat(ids).containsExactlyInAnyOrder(item1.getId(), item2.getId());
    }
}
