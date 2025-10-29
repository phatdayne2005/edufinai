package vn.uth.learningservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.uth.learningservice.model.Moderator;
import vn.uth.learningservice.repository.ModeratorRepository;

import java.util.*;

@Service
public class ModeratorService {

    @Autowired
    private ModeratorRepository moderatorRepo;

    public List<Moderator> getAllModerators() {
        return moderatorRepo.findAll();
    }

    public Moderator getModeratorById(UUID moderatorId) {
        return moderatorRepo.findById(moderatorId).orElse(null);
    }

    public void addModerator(Moderator moderator) {
        moderatorRepo.save(moderator);
    }

    public void updateModerator(Moderator moderator) {
        moderatorRepo.save(moderator);
    }

    public void deleteModerator(UUID moderatorId) {
        moderatorRepo.deleteById(moderatorId);
    }
}
