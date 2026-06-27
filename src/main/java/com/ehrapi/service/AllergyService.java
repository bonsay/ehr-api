package com.ehrapi.service;

import com.ehrapi.entity.Allergy;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.AllergyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AllergyService {

    private final AllergyRepository repository;

    public AllergyService(AllergyRepository repository) {
        this.repository = repository;
    }

    public List<Allergy> getForPatient(Long patientId) {
        return repository.findByPatientIdOrderByRecordedDateDesc(patientId);
    }

    public Allergy create(Long patientId, Allergy allergy) {
        allergy.setId(null);
        allergy.setPatientId(patientId);
        return repository.save(allergy);
    }

    public Allergy update(Long id, Allergy updated) {
        Allergy existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allergy not found with id: " + id));
        existing.setAllergen(updated.getAllergen());
        existing.setReaction(updated.getReaction());
        existing.setSeverity(updated.getSeverity());
        existing.setStatus(updated.getStatus());
        existing.setInstitutionId(updated.getInstitutionId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Allergy not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
