package vn.uth.learningservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.service.LearnerService;

import java.util.List;

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
}
