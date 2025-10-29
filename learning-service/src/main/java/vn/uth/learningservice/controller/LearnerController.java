package vn.uth.learningservice.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.uth.learningservice.dto.LearnerDto;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.service.LearnerService;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@Controller
@RequestMapping("/api/learner")
public class LearnerController {

    @Autowired
    private LearnerService service;

    @GetMapping("/home")
    public String displayLearnerList(Model model) {
        List<Learner> learnerList = service.getAllLearners();
        model.addAttribute("learnerList", learnerList);
        return "learner/home";
    }

    @GetMapping("/create")
    public String displayCreatePage(Model model) {
        LearnerDto learnerDto = new LearnerDto();
        model.addAttribute("learnerDto", learnerDto);
        return "learner/createLearner";
    }

    @PostMapping("/create")
    public String createLearner(@Valid @ModelAttribute LearnerDto learnerDto,
                              BindingResult result) {
        if (result.hasErrors()) {
            return "learner/createLearner";
        }

        //Save image file
        MultipartFile image = learnerDto.getAvatar();
        String fileName = image.getOriginalFilename();

        try {
            String uploadDir = "public/images";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, Paths.get(uploadDir + fileName),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            System.out.println("Exception " + ex.getMessage());
        }

        Learner learner = new Learner();
        learner.setDisplayName(learnerDto.getDisplayName());
        learner.setDob(learnerDto.getDob());
        learner.setLevel(learnerDto.getLevel());
        learner.setAvatar(fileName);
        learner.setBio(learnerDto.getBio());
        learner.setTotalPoints();
        learner.setCreatedAt();
        learner.setUpdatedAt();

        service.addLearner(learner);

        return "redirect:/learner/home";
    }

    @GetMapping("/view")
    public String displayViewPage(Model model, @RequestParam UUID learnerId) {
        try {
            Learner learner = service.getLearnerById(learnerId);
            model.addAttribute("learner", learner);

        } catch (Exception ex) {
            System.out.println("Exception " + ex.getMessage());
            return  "redirect:/learner/home";
        }

        return "learner/viewLearner";
    }

    @GetMapping("/update")
    public String displayUpdatePage(Model model, @RequestParam UUID learnerId) {
        try {
            Learner learner = service.getLearnerById(learnerId);
            model.addAttribute("learner", learner);

            LearnerDto learnerDto = new LearnerDto();
            learnerDto.setDisplayName(learner.getDisplayName());
            learnerDto.setDob(learner.getDob());
            learnerDto.setLevel(learner.getLevel());
            learnerDto.setBio(learner.getBio());

            model.addAttribute("learnerDto", learnerDto);
        } catch (Exception ex) {
            System.out.println("Exception " + ex.getMessage());
            return  "redirect:/learner/home";
        }

        return "learner/updateLearner";
    }

    @PostMapping("/update")
    public String updateLearner(Model model, @RequestParam UUID learnerId,
                              @Valid @ModelAttribute LearnerDto learnerDto,
                              BindingResult result) {
        try {
            Learner learner = service.getLearnerById(learnerId);
            model.addAttribute("learner", learner);

            if (result.hasErrors()) {
                return "learner/updateLearner";
            }

            if (!learnerDto.getAvatar().isEmpty()) {
                // Delete old image
                String uploadDir = "public/images/";
                Path oldUploadPath = Paths.get(uploadDir + learner.getAvatar());

                try {
                    Files.delete(oldUploadPath);
                } catch (Exception ex) {
                    System.out.println("Exception " + ex.getMessage());
                }

                // Save new image file
                MultipartFile image = learnerDto.getAvatar();
                String fileName = image.getOriginalFilename();

                try (InputStream inputStream = image.getInputStream()) {
                    Files.copy(inputStream, Paths.get(uploadDir + fileName),
                            StandardCopyOption.REPLACE_EXISTING);
                }
                learner.setAvatar(fileName);
            }

            learnerDto.setDisplayName(learner.getDisplayName());
            learnerDto.setDob(learner.getDob());
            learnerDto.setLevel(learner.getLevel());
            learnerDto.setBio(learner.getBio());

            service.updateLearner(learner);
        } catch (Exception ex) {
            System.out.println("Exception " + ex.getMessage());
        }

        return "redirect:/learner/home";
    }

    @GetMapping("/delete")
    public String deleteLearner(@RequestParam UUID learnerId) {
        try {
            Learner learner = service.getLearnerById(learnerId);

            // Delete learner avatar
            Path imagePath = Paths.get("public/images/" + learner.getAvatar());

            try {
                Files.delete(imagePath);
            } catch (Exception ex) {
                System.out.println("Exception " + ex.getMessage());
            }

            // Delete the learner
            service.deleteLearner(learnerId);
        } catch (Exception ex) {
            System.out.println("Exception " + ex.getMessage());
        }

        return "redirect:/learner/home";
    }
}
