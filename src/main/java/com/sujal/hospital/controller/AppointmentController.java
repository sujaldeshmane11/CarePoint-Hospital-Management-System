
package com.sujal.hospital.controller;

import com.sujal.hospital.model.Appointment;
import com.sujal.hospital.model.Doctor;
import com.sujal.hospital.repository.AppointmentRepository;
import com.sujal.hospital.repository.PatientRepository;
import com.sujal.hospital.repository.DoctorRepository;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorRepository doctors;

    public AppointmentController(
            AppointmentRepository appointments,
            PatientRepository patients,
            DoctorRepository doctors) {

        this.appointments = appointments;
        this.patients = patients;
        this.doctors = doctors;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            Model model) {

        List<Appointment> result = appointments.findAll();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String search = keyword.trim().toLowerCase();

            result = result.stream()
                    .filter(a ->
                            (a.getPatient() != null &&
                             a.getPatient().getName().toLowerCase().contains(search))
                            ||
                            (a.getDoctor() != null &&
                             a.getDoctor().getName().toLowerCase().contains(search)))
                    .collect(Collectors.toList());
        }

        if (date != null && !date.isBlank()) {
            LocalDate selectedDate = LocalDate.parse(date);

            result = result.stream()
                    .filter(a -> selectedDate.equals(a.getAppointmentDate()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            result = result.stream()
                    .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("appointments", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("date", date);
        model.addAttribute("status", status);

        return "appointments";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("appointment", new Appointment());
        model.addAttribute("patients", patients.findAll());
        model.addAttribute("doctors", doctors.findAll());
        return "appointment-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {

        Appointment appointment = appointments.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Appointment ID"));

        model.addAttribute("appointment", appointment);
        model.addAttribute("patients", patients.findAll());
        model.addAttribute("doctors", doctors.findAll());

        return "appointment-form";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("appointment") Appointment appointment,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            return showFormWithError(model, appointment,
                    "Please enter valid appointment details.");
        }

        if (appointment.getPatient() == null || appointment.getPatient().getId() == null) {
            return showFormWithError(model, appointment,
                    "Please select a patient.");
        }

        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            return showFormWithError(model, appointment,
                    "Please select a doctor.");
        }

        Doctor doctor = doctors.findById(appointment.getDoctor().getId())
                .orElse(null);

        if (doctor == null) {
            return showFormWithError(model, appointment,
                    "Selected doctor was not found.");
        }

        if (!patients.existsById(appointment.getPatient().getId())) {
            return showFormWithError(model, appointment,
                    "Selected patient was not found.");
        }

        if (!doctor.isAvailable()) {
            return showFormWithError(model, appointment,
                    "This doctor is currently not available.");
        }

        LocalDate date = appointment.getAppointmentDate();
        LocalTime time = appointment.getAppointmentTime();

        if (date == null || time == null) {
            return showFormWithError(model, appointment,
                    "Appointment date and time are required.");
        }

        // PAST DATE VALIDATION
        if (date.isBefore(LocalDate.now())) {
            return showFormWithError(model, appointment,
                    "Past date appointment is not allowed.");
        }

        if (doctor.getStartTime() == null || doctor.getEndTime() == null) {
            return showFormWithError(model, appointment,
                    "Doctor timing has not been configured.");
        }

        if (time.isBefore(doctor.getStartTime()) ||
                time.isAfter(doctor.getEndTime())) {

            return showFormWithError(
                    model, appointment,
                    "Appointment time must be between "
                    + doctor.getStartTime() + " and "
                    + doctor.getEndTime() + "."
            );
        }

        if (doctor.getWorkingDays() == null ||
                doctor.getWorkingDays().isBlank()) {

            return showFormWithError(model, appointment,
                    "Doctor working days have not been configured.");
        }

        DayOfWeek selectedDay = date.getDayOfWeek();
        String dayName = selectedDay.name();

        boolean isWorkingDay = List.of(doctor.getWorkingDays().split(","))
                .stream()
                .map(String::trim)
                .anyMatch(day -> day.equalsIgnoreCase(dayName));

        if (!isWorkingDay) {
            return showFormWithError(
                    model, appointment,
                    "Doctor is not available on " + dayName + "."
            );
        }

        List<Appointment> sameDayAppointments =
                appointments.findByDoctor_IdAndAppointmentDate(
                        doctor.getId(), date);

        boolean alreadyBooked = sameDayAppointments.stream()
                .anyMatch(existing ->
                        "BOOKED".equalsIgnoreCase(existing.getStatus())
                        && time.equals(existing.getAppointmentTime())
                        && (appointment.getId() == null
                            || !appointment.getId().equals(existing.getId()))
                );

        if (alreadyBooked) {
            return showFormWithError(
                    model, appointment,
                    "This doctor already has an appointment at this date and time."
            );
        }

        appointment.setDoctor(doctor);

        appointment.setPatient(
                patients.findById(appointment.getPatient().getId()).orElseThrow()
        );

        if (appointment.getStatus() == null ||
                appointment.getStatus().isBlank()) {
            appointment.setStatus("BOOKED");
        }

        appointments.save(appointment);

        return "redirect:/appointments";
    }

    private String showFormWithError(
            Model model,
            Appointment appointment,
            String message) {

        model.addAttribute("appointment", appointment);
        model.addAttribute("patients", patients.findAll());
        model.addAttribute("doctors", doctors.findAll());
        model.addAttribute("errorMessage", message);

        return "appointment-form";
    }

    @PostMapping("/complete/{id}")
    public String complete(@PathVariable Long id) {

        Appointment appointment = appointments.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Appointment ID"));

        appointment.setStatus("COMPLETED");
        appointments.save(appointment);

        return "redirect:/appointments";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable Long id) {

        Appointment appointment = appointments.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Appointment ID"));

        appointment.setStatus("CANCELLED");
        appointments.save(appointment);

        return "redirect:/appointments";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        appointments.deleteById(id);
        return "redirect:/appointments";
    }
}
