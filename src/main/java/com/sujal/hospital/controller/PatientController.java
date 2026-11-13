
package com.sujal.hospital.controller;

import com.sujal.hospital.model.Patient;
import com.sujal.hospital.repository.PatientRepository;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientRepository repository;

    public PatientController(PatientRepository repository) {
        this.repository = repository;
    }

    // Display All Patients or Search Patients
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            Model model) {

        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute(
                    "patients",
                    repository.findByNameContainingIgnoreCase(keyword.trim())
            );
        } else {
            model.addAttribute("patients", repository.findAll());
        }

        model.addAttribute("keyword", keyword);

        return "patients";
    }

    // Open New Patient Form
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("patient", new Patient());
        return "patient-form";
    }

    // Open Edit Patient Form
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {

        Patient patient = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Patient ID"));

        model.addAttribute("patient", patient);

        return "patient-form";
    }

    // Save or Update Patient
    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("patient") Patient patient,
            BindingResult result) {

        if (result.hasErrors()) {
            return "patient-form";
        }

        repository.save(patient);

        return "redirect:/patients";
    }

    // Delete Patient
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/patients";
    }
}
