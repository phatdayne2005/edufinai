package vn.uth.gamificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.gamificationservice.model.Reward;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    Reward save(Reward newReward);
}
