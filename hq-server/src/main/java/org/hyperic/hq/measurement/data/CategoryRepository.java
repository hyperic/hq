package org.hyperic.hq.measurement.data;

import javax.persistence.QueryHint;

import org.hyperic.hq.measurement.server.session.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.transaction.annotation.Transactional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    @Transactional(readOnly=true)
    @Query("select c from Category c where c.name=?1")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "Category.findByName") })
    Category findByName(String name);

}
