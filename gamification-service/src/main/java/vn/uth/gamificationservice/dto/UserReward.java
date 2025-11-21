package vn.uth.gamificationservice.dto;

import lombok.*;
import vn.uth.gamificationservice.model.Reward;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserReward {
    private UUID userId;
    private Double totalScore;
    private List<Reward> rewardDetail;
}
