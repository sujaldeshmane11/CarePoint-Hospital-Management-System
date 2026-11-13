
package com.sujal.hospital.repository;

import com.sujal.hospital.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Search Patient by Name
    List<Patient> findByNameContainingIgnoreCase(String name);

}
