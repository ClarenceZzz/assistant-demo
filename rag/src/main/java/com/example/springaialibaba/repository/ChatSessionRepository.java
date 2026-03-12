package com.example.springaialibaba.repository;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.example.springaialibaba.model.entity.ChatSession;

@Repository
public interface ChatSessionRepository
        extends CrudRepository<ChatSession, Long>, PagingAndSortingRepository<ChatSession, Long> {

    List<ChatSession> findByUserIdOrderByCreatedAtDesc(String userId);
}
