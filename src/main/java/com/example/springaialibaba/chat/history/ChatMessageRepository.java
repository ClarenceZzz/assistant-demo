package com.example.springaialibaba.chat.history;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository
        extends CrudRepository<ChatMessage, Long>, PagingAndSortingRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
}
