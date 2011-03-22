package org.hyperic.hq.galert.data;

import java.util.Collection;

import org.springframework.transaction.annotation.Transactional;

public interface GalertAuxLogRepositoryCustom {

    @Transactional
    void resetAuxType(Collection<Integer> measurementIds);
}
