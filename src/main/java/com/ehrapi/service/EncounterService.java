package com.ehrapi.service;

import com.ehrapi.entity.Encounter;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.EncounterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EncounterService {

    private final EncounterRepository repository;

    public EncounterService(EncounterRepository repository) {
        this.repository = repository;
    }

    public List<Encounter> getForPatient(Long patientId) {
        return repository.findByPatientIdOrderByEncounterDateDesc(patientId);
    }

    public Encounter create(Long patientId, Encounter encounter) {
        encounter.setId(null);
        encounter.setPatientId(patientId);
        return repository.save(encounter);
    }

    public Encounter update(Long id, Encounter updated) {
        Encounter existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with id: " + id));
        existing.setEncounterDate(updated.getEncounterDate());
        existing.setType(updated.getType());
        existing.setReason(updated.getReason());
        existing.setProviderName(updated.getProviderName());
        existing.setNotes(updated.getNotes());
        existing.setStatus(updated.getStatus());
        existing.setInstitutionId(updated.getInstitutionId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Encounter not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
