-- ---------------------------------------------------------------------------
-- Seed data for the modular EHR.
--
-- NOTE: spring.sql.init.mode is "embedded" on the default (Oracle) profile, so
-- this script runs only for the embedded H2 dev profile
-- (mvn spring-boot:run -Dspring-boot.run.profiles=h2). Each table has its own
-- Hibernate sequence, so NEXTVAL produces predictable ids (1, 2, ...) on a fresh
-- create-drop database.
-- ---------------------------------------------------------------------------

-- Module catalog -------------------------------------------------------------
-- The six clinical basics are FREE (tier FREE) — the adoption baseline every
-- institution gets. The remaining entries are paid (PRO / ENTERPRISE) and need
-- an active entitlement before they can be enabled.
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'DEMOGRAPHICS', 'Patient Demographics', 'Patient registration and identifying information.', 'Core', 'patients', 1, 'FREE', 'FREE', NULL);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'ENCOUNTERS', 'Clinical Encounters', 'Visit notes and clinical documentation.', 'Clinical', 'encounters', 1, 'FREE', 'FREE', NULL);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'PROBLEMS', 'Problem List', 'Active and resolved diagnoses.', 'Clinical', 'problems', 1, 'FREE', 'FREE', NULL);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'MEDICATIONS', 'Medications', 'Prescriptions and medication history.', 'Clinical', 'medications', 1, 'FREE', 'FREE', NULL);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'ALLERGIES', 'Allergies', 'Allergies and intolerances.', 'Clinical', 'allergies', 1, 'FREE', 'FREE', NULL);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'VITALS', 'Vital Signs', 'Recorded vital signs.', 'Clinical', 'vitals', 1, 'FREE', 'FREE', NULL);
-- Paid add-on modules (the marketplace storefront).
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'LAB_ORDERS', 'Lab Orders & Results', 'Order laboratory tests and receive structured results.', 'Orders', 'lab-orders', 1, 'PRO', 'SUBSCRIPTION', 4900);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'IMAGING', 'Imaging Orders & Results', 'Radiology orders and report retrieval.', 'Orders', 'imaging', 1, 'PRO', 'SUBSCRIPTION', 5900);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'PATIENT_PORTAL', 'Patient Portal & Messaging', 'Secure patient messaging and self-service record access.', 'Engagement', 'patient-portal', 1, 'PRO', 'SUBSCRIPTION', 3900);
INSERT INTO ehr_modules (id, code, name, description, category, api_path, active, tier, price_model, price_monthly_cents) VALUES
 (EHR_MODULES_ID_SEQ.NEXTVAL, 'ANALYTICS', 'Population Health Analytics', 'Dashboards and population-level reporting across the patient panel.', 'Insights', 'analytics', 1, 'ENTERPRISE', 'SUBSCRIPTION', 19900);

-- Institutions ---------------------------------------------------------------
INSERT INTO institutions (id, name, code, type, address, phone, active, date_created) VALUES
 (INSTITUTIONS_ID_SEQ.NEXTVAL, 'General Hospital', 'GH', 'HOSPITAL', '100 Care Blvd', '555-0100', 1, TIMESTAMP '2024-01-01 09:00:00');
INSERT INTO institutions (id, name, code, type, address, phone, active, date_created) VALUES
 (INSTITUTIONS_ID_SEQ.NEXTVAL, 'Downtown Clinic', 'DTC', 'CLINIC', '42 Main St', '555-0200', 1, TIMESTAMP '2024-01-02 09:00:00');

-- Module enablement (the "pick and choose" model) ----------------------------
-- General Hospital (id 1) enables every module.
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 1, 'DEMOGRAPHICS', 1, TIMESTAMP '2024-01-01 09:00:00');
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 1, 'ENCOUNTERS', 1, TIMESTAMP '2024-01-01 09:00:00');
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 1, 'PROBLEMS', 1, TIMESTAMP '2024-01-01 09:00:00');
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 1, 'MEDICATIONS', 1, TIMESTAMP '2024-01-01 09:00:00');
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 1, 'ALLERGIES', 1, TIMESTAMP '2024-01-01 09:00:00');
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 1, 'VITALS', 1, TIMESTAMP '2024-01-01 09:00:00');
-- General Hospital has purchased Lab Orders, so it may enable it too.
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 1, 'LAB_ORDERS', 1, TIMESTAMP '2024-03-01 09:00:00');
-- Downtown Clinic (id 2) enables only the modules that suit its workflow.
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 2, 'DEMOGRAPHICS', 1, TIMESTAMP '2024-01-02 09:00:00');
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 2, 'PROBLEMS', 1, TIMESTAMP '2024-01-02 09:00:00');
INSERT INTO institution_modules (id, institution_id, module_code, enabled, enabled_date) VALUES (INST_MODULES_ID_SEQ.NEXTVAL, 2, 'MEDICATIONS', 1, TIMESTAMP '2024-01-02 09:00:00');

-- Module entitlements (the commercial layer) --------------------------------
-- General Hospital (id 1) has a paid LAB_ORDERS subscription and is trialing
-- ANALYTICS. IMAGING and PATIENT_PORTAL remain unlicensed, so the marketplace
-- offers them for trial/purchase. Downtown Clinic holds no paid entitlements.
INSERT INTO module_entitlements (id, institution_id, module_code, status, source, starts_at, expires_at) VALUES
 (MODULE_ENTITLEMENTS_ID_SEQ.NEXTVAL, 1, 'LAB_ORDERS', 'ACTIVE', 'LOCAL_PURCHASE', TIMESTAMP '2024-03-01 09:00:00', NULL);
INSERT INTO module_entitlements (id, institution_id, module_code, status, source, starts_at, expires_at) VALUES
 (MODULE_ENTITLEMENTS_ID_SEQ.NEXTVAL, 1, 'ANALYTICS', 'TRIAL', 'TRIAL', TIMESTAMP '2026-06-01 09:00:00', TIMESTAMP '2026-12-31 09:00:00');

-- Patients (registered at General Hospital, id 1) ----------------------------
INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, gender, email, phone, address, home_institution_id, date_created) VALUES
 (PATIENTS_ID_SEQ.NEXTVAL, 'MRN-1001', 'John', 'Carter', DATE '1979-04-12', 'MALE', 'john.carter@example.com', '555-1001', '7 Oak Lane', 1, TIMESTAMP '2024-02-01 10:00:00');
INSERT INTO patients (id, mrn, first_name, last_name, date_of_birth, gender, email, phone, address, home_institution_id, date_created) VALUES
 (PATIENTS_ID_SEQ.NEXTVAL, 'MRN-1002', 'Maria', 'Lopez', DATE '1990-09-23', 'FEMALE', 'maria.lopez@example.com', '555-1002', '15 Pine Ave', 1, TIMESTAMP '2024-02-03 11:00:00');

-- Clinical data for John Carter (patient id 1) at General Hospital (id 1) -----
INSERT INTO problems (id, patient_id, institution_id, code, description, status, onset_date, recorded_date) VALUES
 (PROBLEMS_ID_SEQ.NEXTVAL, 1, 1, 'E11.9', 'Type 2 diabetes mellitus', 'ACTIVE', DATE '2020-06-01', DATE '2024-02-01');
INSERT INTO problems (id, patient_id, institution_id, code, description, status, onset_date, recorded_date) VALUES
 (PROBLEMS_ID_SEQ.NEXTVAL, 1, 1, 'I10', 'Essential hypertension', 'ACTIVE', DATE '2019-03-15', DATE '2024-02-01');

INSERT INTO medications (id, patient_id, institution_id, name, dosage, frequency, route, status, start_date, end_date, prescriber) VALUES
 (MEDICATIONS_ID_SEQ.NEXTVAL, 1, 1, 'Metformin', '500 mg', 'Twice daily', 'Oral', 'ACTIVE', DATE '2024-02-01', NULL, 'Dr. Adams');
INSERT INTO medications (id, patient_id, institution_id, name, dosage, frequency, route, status, start_date, end_date, prescriber) VALUES
 (MEDICATIONS_ID_SEQ.NEXTVAL, 1, 1, 'Lisinopril', '10 mg', 'Once daily', 'Oral', 'ACTIVE', DATE '2024-02-01', NULL, 'Dr. Adams');

INSERT INTO allergies (id, patient_id, institution_id, allergen, reaction, severity, status, recorded_date) VALUES
 (ALLERGIES_ID_SEQ.NEXTVAL, 1, 1, 'Penicillin', 'Hives', 'MODERATE', 'ACTIVE', DATE '2024-02-01');

INSERT INTO vital_signs (id, patient_id, institution_id, recorded_date, blood_pressure, heart_rate, respiratory_rate, temperature, oxygen_saturation, height, weight) VALUES
 (VITALS_ID_SEQ.NEXTVAL, 1, 1, TIMESTAMP '2024-02-01 10:15:00', '128/82', 76, 16, 36.8, 98, 178.0, 84.5);

INSERT INTO encounters (id, patient_id, institution_id, encounter_date, type, reason, provider_name, notes, status) VALUES
 (ENCOUNTERS_ID_SEQ.NEXTVAL, 1, 1, TIMESTAMP '2024-02-01 10:00:00', 'Office Visit', 'Routine diabetes follow-up', 'Dr. Adams', 'Patient stable. Continue current regimen. Reinforced diet and exercise.', 'COMPLETED');

-- Cross-institution consent --------------------------------------------------
-- John Carter (patient 1) lets Downtown Clinic (institution 2) see only his
-- problems and medications. Encounters, allergies and vitals stay private.
INSERT INTO patient_consents (id, patient_id, granted_to_institution_id, scope, status, granted_date, expiry_date, revoked_date) VALUES
 (CONSENTS_ID_SEQ.NEXTVAL, 1, 2, 'PROBLEMS,MEDICATIONS', 'ACTIVE', TIMESTAMP '2024-02-05 09:00:00', NULL, NULL);

COMMIT;
