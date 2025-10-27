package vn.uth.learningservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.repository.LearnerRepository;
import java.util.List;

@Service
public class LearnerService {

    @Autowired
    private LearnerRepository learnerRepo;

    public List<Learner> getAllLearners() {
        return learnerRepo.findAll();
    }
}
