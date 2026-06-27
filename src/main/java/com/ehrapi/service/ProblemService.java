package com.ehrapi.service;

import com.ehrapi.entity.Problem;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.ProblemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProblemService {

    private final ProblemRepository repository;

    public ProblemService(ProblemRepository repository) {
        this.repository = repository;
    }

    public List<Problem> getForPatient(Long patientId) {
        return repository.findByPatientIdOrderByRecordedDateDesc(patientId);
    }

    public Problem create(Long patientId, Problem problem) {
        problem.setId(null);
        problem.setPatientId(patientId);
        return repository.save(problem);
    }

    public Problem update(Long id, Problem updated) {
        Problem existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + id));
        existing.setCode(updated.getCode());
        existing.setDescription(updated.getDescription());
        existing.setStatus(updated.getStatus());
        existing.setOnsetDate(updated.getOnsetDate());
        existing.setInstitutionId(updated.getInstitutionId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Problem not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
