/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SequenceGenerator;
import org.hibernate.type.Type;
import org.hyperic.hibernate.dialect.HQDialect;

/**
 * The ComboGenerator dispatches to either a sequence-based generator
 * or a HQMultipleHiLoPerTableGenerator, depending on whether or not the
 * DB supports sequences.  
 */
public class ComboGenerator 
	implements PersistentIdentifierGenerator, Configurable 
{
	private static final Log _log = LogFactory.getLog(ComboGenerator.class); 

    // The delegate implements PersistentIdentifierGenerator as well 
    // as Configurable
    private PersistentIdentifierGenerator _delegate;
    
    public void configure(Type type, Properties params, Dialect d) 
        throws MappingException 
    {
        if (_log.isDebugEnabled())
            _log.debug("Configuring ComboGenerator for dialect [" + d + "]");
        
        HQDialect hqDialect = (HQDialect)d;
        if (hqDialect.usesSequenceGenerator()) {
            _delegate = new SequenceGenerator();
        } else {
            _delegate = new HQMultipleHiLoPerTableGenerator();
            
            // Table containing the sequences
            params.put(HQMultipleHiLoPerTableGenerator.ID_TABLE, "HQ_SEQUENCE");
            
            // Name of the column containing the 'sequence' name
            params.put(HQMultipleHiLoPerTableGenerator.PK_COLUMN_NAME,
                       "seq_name");

            // Name of the column containing the value for the sequence
            params.put(HQMultipleHiLoPerTableGenerator.VALUE_COLUMN_NAME,
                       "seq_val");
            
            // How many IDs do we increment and cache?
            params.put(HQMultipleHiLoPerTableGenerator.MAX_LO, "100");

            // What is the initial 'hi' value?
            params.put(HQMultipleHiLoPerTableGenerator.INITIAL_HI, "100");
        }
        ((Configurable)_delegate).configure(type, params, d);
        
    }

    public Object generatorKey() {
        return _delegate.generatorKey();
    }
    
    public String[] sqlCreateStrings(Dialect dialect) 
        throws HibernateException 
    {
        return _delegate.sqlCreateStrings(dialect);
    }
    
    public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
        return _delegate.sqlDropStrings(dialect);
    }
    
    public Serializable generate(SessionImplementor session, Object object) 
        throws HibernateException 
    {
        return _delegate.generate(session, object);
    }
}
