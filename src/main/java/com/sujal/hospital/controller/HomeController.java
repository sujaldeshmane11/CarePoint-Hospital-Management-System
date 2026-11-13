package com.sujal.hospital.controller;

import com.sujal.hospital.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;

    public HomeController(
            PatientRepository patients,
            DoctorRepository doctors,
            AppointmentRepository appointments) {

        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
    }

    @GetMapping("/")
    public String dashboard(Model model) {

        model.addAttribute("patientCount", patients.count());
        model.addAttribute("doctorCount", doctors.count());
        model.addAttribute("appointmentCount", appointments.count());

        model.addAttribute("completedCount",
                appointments.countByStatus("COMPLETED"));

        model.addAttribute("cancelledCount",
                appointments.countByStatus("CANCELLED"));

        model.addAttribute("bookedCount",
                appointments.countByStatus("BOOKED"));

        // Upcoming Appointments
        model.addAttribute("upcomingAppointments",
                appointments.findByAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(
                        LocalDate.now()
                ).stream()
                        .filter(a -> "BOOKED".equals(a.getStatus()))
                        .toList()
        );

        return "dashboard";
    }
}