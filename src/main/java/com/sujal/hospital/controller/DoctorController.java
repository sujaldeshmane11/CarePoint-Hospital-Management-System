
package com.sujal.hospital.controller;

import com.sujal.hospital.model.Doctor;
import com.sujal.hospital.repository.DoctorRepository;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorRepository repository;

    public DoctorController(DoctorRepository repository) {
        this.repository = repository;
    }

    // Display All Doctors / Search Doctors
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            Model model) {

        if (keyword != null && !keyword.trim().isEmpty()) {
            String search = keyword.trim();

            model.addAttribute("doctors",
                    repository.findByNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(
                            search, search));
        } else {
            model.addAttribute("doctors", repository.findAll());
        }

        model.addAttribute("keyword", keyword);
        return "doctors";
    }

    // Open New Doctor Form
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("doctor", new Doctor());
        return "doctor-form";
    }

    // Open Edit Doctor Form
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {

        Doctor doctor = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Doctor ID"));

        model.addAttribute("doctor", doctor);
        return "doctor-form";
    }

    // Save or Update Doctor
    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("doctor") Doctor doctor,
            BindingResult result) {

        if (result.hasErrors()) {
            return "doctor-form";
        }

        repository.save(doctor);
        return "redirect:/doctors";
    }

    // Delete Doctor
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/doctors";
    }
}
