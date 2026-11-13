
package com.sujal.hospital.repository;

import com.sujal.hospital.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // Search Doctor by Name
    List<Doctor> findByNameContainingIgnoreCase(String name);

    // Search Doctor by Specialization
    List<Doctor> findBySpecializationContainingIgnoreCase(String specialization);

    // Search by Name OR Specialization
    List<Doctor> findByNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(
            String name, String specialization);
}
