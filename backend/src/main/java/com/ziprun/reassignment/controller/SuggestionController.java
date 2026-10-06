package com.ziprun.reassignment.controller;

import com.ziprun.reassignment.dto.UpdateSuggestionStatusRequest;
import com.ziprun.reassignment.model.ReassignmentSuggestion;
import com.ziprun.reassignment.model.SuggestionStatus;
import com.ziprun.reassignment.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/suggestions", "/api/reassignments/suggestions"})
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public ResponseEntity<List<ReassignmentSuggestion>> getSuggestions(
            @RequestParam(value = "status", required = false) SuggestionStatus status) {
        return ResponseEntity.ok(suggestionService.getSuggestions(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReassignmentSuggestion> getSuggestionById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(suggestionService.getSuggestionById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReassignmentSuggestion> updateSuggestionStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateSuggestionStatusRequest request) {
        ReassignmentSuggestion updated = suggestionService.updateSuggestionStatus(id, request.getStatus());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ReassignmentSuggestion> acceptSuggestion(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(suggestionService.acceptSuggestion(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ReassignmentSuggestion> rejectSuggestion(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(suggestionService.rejectSuggestion(id));
    }
}
