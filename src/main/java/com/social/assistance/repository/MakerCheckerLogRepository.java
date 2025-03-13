package com.social.assistance.repository;

import com.social.assistance.model.MakerCheckerLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MakerCheckerLogRepository extends JpaRepository<MakerCheckerLog, Integer> {

    List<MakerCheckerLog> findByEntityTypeAndEntityId(String entityType, Integer entityId);

    List<MakerCheckerLog> findByMakerId(Integer makerId);
    
    List<MakerCheckerLog> findByStatus(String status);
}
