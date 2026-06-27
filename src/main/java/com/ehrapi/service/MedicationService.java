package com.ehrapi.service;

import com.ehrapi.entity.Medication;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.MedicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicationService {

    private final MedicationRepository repository;

    public MedicationService(MedicationRepository repository) {
        this.repository = repository;
    }

    public List<Medication> getForPatient(Long patientId) {
        return repository.findByPatientIdOrderByStartDateDesc(patientId);
    }

    public Medication create(Long patientId, Medication medication) {
        medication.setId(null);
        medication.setPatientId(patientId);
        return repository.save(medication);
    }

    public Medication update(Long id, Medication updated) {
        Medication existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication not found with id: " + id));
        existing.setName(updated.getName());
        existing.setDosage(updated.getDosage());
        existing.setFrequency(updated.getFrequency());
        existing.setRoute(updated.getRoute());
        existing.setStatus(updated.getStatus());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setPrescriber(updated.getPrescriber());
        existing.setInstitutionId(updated.getInstitutionId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Medication not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
