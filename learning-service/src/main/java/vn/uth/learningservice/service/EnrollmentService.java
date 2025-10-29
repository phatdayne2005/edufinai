package vn.uth.learningservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.uth.learningservice.model.Enrollment;
import vn.uth.learningservice.repository.EnrollmentRepository;

import java.util.*;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollRepo;

    public List<Enrollment> getAllEnrollments() {
        return enrollRepo.findAll();
    }

    public Enrollment getEnrollById(UUID enrollmentId) {
        return enrollRepo.findById(enrollmentId).orElse(null);
    }

    public void addEnrollment(Enrollment enrollment) {
        enrollRepo.save(enrollment);
    }

    public void updateEnrollment(Enrollment enrollment) {
        enrollRepo.save(enrollment);
    }

    public void deleteEnrollment(UUID enrollmentId) {
        enrollRepo.deleteById(enrollmentId);
    }
}
