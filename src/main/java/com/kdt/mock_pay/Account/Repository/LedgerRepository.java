package com.kdt.mock_pay.Account.Repository;

import com.kdt.mock_pay.Account.Entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {

}


