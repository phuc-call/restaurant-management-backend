package com.example.shop.repository;

import com.example.shop.entity.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AIConversationRepository
        extends JpaRepository<AIConversation, Long> {

    Optional<AIConversation>
    findFirstBySessionIdOrderByUpdatedAtDesc(String sessionId);





    @Query("""
                SELECT
                    a.sessionId,
                    MIN(a.lastQuestion),
                    MAX(a.updatedAt)
                FROM AIConversation a
                GROUP BY a.sessionId
                ORDER BY MAX(a.updatedAt) DESC
            """)
    List<Object[]> getChatSidebar();
    @Query("""
            SELECT a
            FROM AIConversation a
            WHERE a.sessionId=:sessionId
            ORDER BY a.updatedAt ASC""")
    List<AIConversation> getChatBySession(String sessionId);

}