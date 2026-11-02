package com.prasad.paystream.core.repository;

import com.prasad.paystream.core.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByName(String name);
}
