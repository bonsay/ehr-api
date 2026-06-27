package com.ehrapi.service;

import com.ehrapi.entity.VitalSign;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.VitalSignRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VitalSignService {

    private final VitalSignRepository repository;

    public VitalSignService(VitalSignRepository repository) {
        this.repository = repository;
    }

    public List<VitalSign> getForPatient(Long patientId) {
        return repository.findByPatientIdOrderByRecordedDateDesc(patientId);
    }

    public VitalSign create(Long patientId, VitalSign vital) {
        vital.setId(null);
        vital.setPatientId(patientId);
        return repository.save(vital);
    }

    public VitalSign update(Long id, VitalSign updated) {
        VitalSign existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vital sign not found with id: " + id));
        existing.setRecordedDate(updated.getRecordedDate());
        existing.setBloodPressure(updated.getBloodPressure());
        existing.setHeartRate(updated.getHeartRate());
        existing.setRespiratoryRate(updated.getRespiratoryRate());
        existing.setTemperature(updated.getTemperature());
        existing.setOxygenSaturation(updated.getOxygenSaturation());
        existing.setHeight(updated.getHeight());
        existing.setWeight(updated.getWeight());
        existing.setInstitutionId(updated.getInstitutionId());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Vital sign not found with id: " + id);
        }
        repository.deleteById(id);
    }
}
