package com.ziprun.reassignment.repository;

import com.ziprun.reassignment.model.ReassignmentSuggestion;
import com.ziprun.reassignment.model.SuggestionStatus;
import com.ziprun.reassignment.model.TriggerReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SuggestionRepository extends JpaRepository<ReassignmentSuggestion, UUID> {

    boolean existsByOrderIdAndTriggerReasonAndStatus(String orderId, TriggerReason triggerReason, SuggestionStatus status);

    List<ReassignmentSuggestion> findByStatus(SuggestionStatus status);

    List<ReassignmentSuggestion> findByOrderId(String orderId);

    List<ReassignmentSuggestion> findByOrderIdAndStatus(String orderId, SuggestionStatus status);
}
