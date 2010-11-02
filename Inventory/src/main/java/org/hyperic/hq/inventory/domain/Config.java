package org.hyperic.hq.inventory.domain;

import javax.persistence.Basic;
import javax.persistence.FetchType;
import javax.persistence.Lob;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooEntity
public class Config {
    @Lob
    @Basic(fetch=FetchType.LAZY)
    private byte[] _productResponse;
    @Lob
    @Basic(fetch=FetchType.LAZY)
    private byte[] _controlResponse;
    @Lob
    @Basic(fetch=FetchType.LAZY)
    private byte[] _measurementResponse;
    @Lob
    @Basic(fetch=FetchType.LAZY)
    private byte[] _autoInventoryResponse;
}
