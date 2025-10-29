package vn.uth.learningservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.repository.LearnerRepository;

import java.util.*;

@Service
public class LearnerService {

    @Autowired
    private LearnerRepository learnerRepo;

    public List<Learner> getAllLearners() {
        return learnerRepo.findAll();
    }

    public Learner getLearnerById(UUID learnerId) {
        return learnerRepo.findById(learnerId).orElse(null);
    }

    public void addLearner(Learner learner) {
        learnerRepo.save(learner);
    }

    public void updateLearner(Learner learner) {
        learnerRepo.save(learner);
    }

    public void deleteLearner(UUID learnerId) {
        learnerRepo.deleteById(learnerId);
    }
}
