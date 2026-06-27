package com.ehrapi.service;

import com.ehrapi.entity.Patient;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    public List<Patient> search(String term) {
        if (term == null || term.isBlank()) {
            return patientRepository.findAll();
        }
        return patientRepository.search(term.trim());
    }

    public List<Patient> getByInstitution(Long institutionId) {
        return patientRepository.findByHomeInstitutionId(institutionId);
    }

    public Patient getById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    public Patient create(Patient patient) {
        patient.setId(null);
        return patientRepository.save(patient);
    }

    public Patient update(Long id, Patient updated) {
        Patient existing = getById(id);
        existing.setMrn(updated.getMrn());
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setGender(updated.getGender());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setHomeInstitutionId(updated.getHomeInstitutionId());
        return patientRepository.save(existing);
    }

    public void delete(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Patient not found with id: " + id);
        }
        patientRepository.deleteById(id);
    }
}
