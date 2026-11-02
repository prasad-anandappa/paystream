package com.prasad.paystream.core.repository;

import com.prasad.paystream.core.domain.Direction;
import com.prasad.paystream.core.domain.Posting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostingRepository extends JpaRepository<Posting, Long> {

    /** Sum of posting amounts for one account in one direction. Used by reconciliation. */
    @Query("select coalesce(sum(p.amountMinor), 0) from Posting p "
            + "where p.account.id = :accountId and p.direction = :direction")
    long sumByAccountAndDirection(@Param("accountId") Long accountId,
                                  @Param("direction") Direction direction);

    /** Platform-wide total for one direction. Debits must equal credits, always. */
    @Query("select coalesce(sum(p.amountMinor), 0) from Posting p where p.direction = :direction")
    long sumByDirection(@Param("direction") Direction direction);
}
