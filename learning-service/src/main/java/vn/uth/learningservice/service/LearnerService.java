package vn.uth.learningservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Learner;
import vn.uth.learningservice.repository.LearnerRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearnerService {

    private final LearnerRepository learnerRepo;

    public Learner getById(UUID id) {
        return learnerRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Learner not found: " + id));
    }

    @Transactional
    public Learner getOrCreate(UUID id) {
        return learnerRepo.findById(id).orElseGet(() -> {
            Learner newLearner = new Learner();
            newLearner.setId(id);
            return learnerRepo.save(newLearner);
        });
    }

    public List<Learner> listAll() {
        return learnerRepo.findAll();
    }

    public List<Learner> listByLevel(Learner.Level level) {
        return learnerRepo.findByLevel(level);
    }

    @Transactional
    public void addPoints(UUID learnerId, int delta) {
        if (delta <= 0)
            return; // chỉ cộng điểm nếu delta > 0
        int updated = learnerRepo.addLearningPoints(learnerId, delta);
        if (updated == 0)
            throw new EntityNotFoundException("Learner not found: " + learnerId);
    }
}
