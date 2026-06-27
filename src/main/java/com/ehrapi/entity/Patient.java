package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patient demographics. A patient is registered at a home institution but may
 * have their record shared with others through {@link PatientConsent}.
 */
@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "patients_seq")
    @SequenceGenerator(name = "patients_seq", sequenceName = "PATIENTS_ID_SEQ", allocationSize = 1)
    private Long id;

    /** Medical Record Number. */
    @Column(nullable = false, unique = true, length = 50)
    private String mrn;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String gender;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String address;

    /** Institution where the patient is primarily registered. */
    @Column(name = "home_institution_id")
    private Long homeInstitutionId;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    public Patient() {
        this.dateCreated = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMrn() { return mrn; }
    public void setMrn(String mrn) { this.mrn = mrn; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Long getHomeInstitutionId() { return homeInstitutionId; }
    public void setHomeInstitutionId(Long homeInstitutionId) { this.homeInstitutionId = homeInstitutionId; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
}
