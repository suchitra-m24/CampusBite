package com.campusbite.trust;

import com.campusbite.common.exception.ResourceNotFoundException;
import com.campusbite.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrustScoreService {

    private final TrustScoreRepository trustScoreRepository;

    public TrustScoreService(TrustScoreRepository trustScoreRepository) {
        this.trustScoreRepository = trustScoreRepository;
    }

    @Transactional
    public TrustScore initializeFor(User user) {
        return trustScoreRepository.findByUser(user)
                .orElseGet(() -> trustScoreRepository.save(new TrustScore(user)));
    }

    public TrustScore getFor(User user) {
        return trustScoreRepository.findByUser(user)
                .orElseGet(() -> initializeFor(user));
    }

    public TrustScore getByUserId(Long userId) {
        return trustScoreRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No trust score found for this student"));
    }

    @Transactional
    public void recordNoShow(User user) {
        TrustScore score = getFor(user);
        score.recordNoShow();
        trustScoreRepository.save(score);
    }

    @Transactional
    public void recordLateCancel(User user) {
        TrustScore score = getFor(user);
        score.recordLateCancel();
        trustScoreRepository.save(score);
    }

    @Transactional
    public void recordSuccessfulPickup(User user) {
        TrustScore score = getFor(user);
        score.recordSuccessfulPickup();
        trustScoreRepository.save(score);
    }

    public boolean isRestricted(User user) {
        return trustScoreRepository.findByUser(user).map(TrustScore::isRestricted).orElse(false);
    }
}
