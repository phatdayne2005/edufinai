package vn.uth.learningservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.learningservice.model.Creator;
import vn.uth.learningservice.repository.CreatorRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CreatorService {

    private final CreatorRepository creatorRepo;

    public Creator getById(UUID id) {
        return creatorRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Creator not found: " + id));
    }

    @Transactional
    public Creator getOrCreate(UUID id) {
        return creatorRepo.findById(id).orElseGet(() -> {
            Creator newCreator = new Creator();
            newCreator.setId(id);
            return creatorRepo.save(newCreator);
        });
    }

    public List<Creator> listAll() {
        return creatorRepo.findAll();
    }

    public long countLessons(UUID creatorId) {
        return creatorRepo.countLessons(creatorId);
    }
}
