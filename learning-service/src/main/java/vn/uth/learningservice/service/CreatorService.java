package vn.uth.learningservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.uth.learningservice.model.Creator;
import vn.uth.learningservice.repository.CreatorRepository;

import java.util.*;

@Service
public class CreatorService {

    @Autowired
    private CreatorRepository creatorRepo;

    public List<Creator> getAllCreators() {
        return creatorRepo.findAll();
    }

    public Creator getCreatorById(UUID creatorId) {
        return creatorRepo.findById(creatorId).orElse(null);
    }

    public void addCreator(Creator creator) {
        creatorRepo.save(creator);
    }

    public void updateCreator(Creator creator) {
        creatorRepo.save(creator);
    }

    public void deleteCreator(UUID creatorId) {
        creatorRepo.deleteById(creatorId);
    }
}
