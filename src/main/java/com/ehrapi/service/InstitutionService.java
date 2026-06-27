package com.ehrapi.service;

import com.ehrapi.entity.Institution;
import com.ehrapi.exception.ResourceNotFoundException;
import com.ehrapi.repository.InstitutionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    public List<Institution> getAll() {
        return institutionRepository.findAll();
    }

    public Institution getById(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found with id: " + id));
    }

    public Institution create(Institution institution) {
        institution.setId(null);
        return institutionRepository.save(institution);
    }

    public Institution update(Long id, Institution updated) {
        Institution existing = getById(id);
        existing.setName(updated.getName());
        existing.setCode(updated.getCode());
        existing.setType(updated.getType());
        existing.setAddress(updated.getAddress());
        existing.setPhone(updated.getPhone());
        existing.setActive(updated.isActive());
        return institutionRepository.save(existing);
    }

    public void delete(Long id) {
        if (!institutionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Institution not found with id: " + id);
        }
        institutionRepository.deleteById(id);
    }
}
