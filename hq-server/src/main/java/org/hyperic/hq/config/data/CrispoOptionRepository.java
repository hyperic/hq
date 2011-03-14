package org.hyperic.hq.config.data;

import java.util.List;

import org.hyperic.hq.config.domain.CrispoOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CrispoOptionRepository extends JpaRepository<CrispoOption, Integer> {

    // TODO this requires user to pass in values in format %key%. Remove that
    // restriction?
    List<CrispoOption> findByKeyLike(String key);

    // TODO use JPQL "member of" when this Hibernate bug is resolved
    // http://opensource.atlassian.com/projects/hibernate/browse/HHH-5209
    @Transactional(readOnly = true)
    @Query("select o from CrispoOption o where o.val = :val or :val in elements(o.array)")
    List<CrispoOption> findByValue(@Param("val") String value);
}
