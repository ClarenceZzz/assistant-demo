package com.example.springaialibaba.chat.history;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository
        extends CrudRepository<ChatSession, Long>, PagingAndSortingRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByCreatedAtDesc(String userId);
}
