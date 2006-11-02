/*
 * Generated file - Do not edit!
 */
package org.hyperic.hq.events.shared;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * Primary key for UserAlert.
 */
public class UserAlertPK
   extends java.lang.Object
   implements java.io.Serializable
{
   private int _hashCode = Integer.MIN_VALUE;
   private StringBuffer _toStringValue = null;

   public Integer id;

   public UserAlertPK()
   {
   }

   public UserAlertPK( Integer id )
   {
      this.id = id;
   }

   public Integer getId()
   {
      return id;
   }

   public void setId(Integer id)
   {
      this.id = id;
      _hashCode = Integer.MIN_VALUE;
   }

   public int hashCode()
   {
      if( _hashCode == Integer.MIN_VALUE )
      {
         if (this.id != null) _hashCode += this.id.hashCode();
      }

      return _hashCode;
   }

   public boolean equals(Object obj)
   {
      if( !(obj instanceof org.hyperic.hq.events.shared.UserAlertPK) )
         return false;

      org.hyperic.hq.events.shared.UserAlertPK pk = (org.hyperic.hq.events.shared.UserAlertPK)obj;
      boolean eq = true;

      if( obj == null )
      {
         eq = false;
      }
      else
      {
         if( this.id == null && ((org.hyperic.hq.events.shared.UserAlertPK)obj).getId() == null )
         {
            eq = true;
         }
         else
         {
            if( this.id == null || ((org.hyperic.hq.events.shared.UserAlertPK)obj).getId() == null )
            {
               eq = false;
            }
            else
            {
               eq = eq && this.id.equals( pk.id );
            }
         }
      }

      return eq;
   }

   /** @return String representation of this pk in the form of [.field1.field2.field3]. */
   public String toString()
   {
      if( _toStringValue == null )
      {
         _toStringValue = new StringBuffer("[.");
         _toStringValue.append(this.id).append('.');
         _toStringValue.append(']');
      }

      return _toStringValue.toString();
   }

}
