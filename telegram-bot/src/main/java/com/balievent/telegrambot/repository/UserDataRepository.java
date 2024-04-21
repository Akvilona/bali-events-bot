package com.balievent.telegrambot.repository;

import com.balievent.telegrambot.model.entity.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDataRepository extends JpaRepository<UserData, Long> {
    UserData findUserDataById(Long id);
}
