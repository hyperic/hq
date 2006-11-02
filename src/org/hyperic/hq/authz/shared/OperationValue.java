/*
 * xdoclet generated file
 * legacy DTO pattern (targeted for replacement with hibernate pojo)
 */
package org.hyperic.hq.authz.shared;

/**
 * Value object for Operation.
 *
 */
public class OperationValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private String name;
   private boolean nameHasBeenSet = false;
   private Integer id;
   private boolean idHasBeenSet = false;

   private org.hyperic.hq.authz.shared.OperationPK pk;

   public OperationValue()
   {
	  pk = new org.hyperic.hq.authz.shared.OperationPK();
   }

   public OperationValue( String name,Integer id )
   {
	  this.name = name;
	  nameHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  pk = new org.hyperic.hq.authz.shared.OperationPK(this.getId());
   }

   //TODO Cloneable is better than this !
   public OperationValue( OperationValue otherValue )
   {
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;

	  pk = new org.hyperic.hq.authz.shared.OperationPK(this.getId());
   }

   public org.hyperic.hq.authz.shared.OperationPK getPrimaryKey()
   {
	  return pk;
   }

   public String getName()
   {
	  return this.name;
   }

   public void setName( String name )
   {
	  this.name = name;
	  nameHasBeenSet = true;

   }

   public boolean nameHasBeenSet(){
	  return nameHasBeenSet;
   }
   public Integer getId()
   {
	  return this.id;
   }

   public void setId( Integer id )
   {
	  this.id = id;
	  idHasBeenSet = true;

		 pk.setId(id);
   }

   public boolean idHasBeenSet(){
	  return idHasBeenSet;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("name=" + getName() + " " + "id=" + getId());
	  str.append('}');

	  return(str.toString());
   }

   /**
	* A Value object have an identity if its attributes making its Primary Key
	* has all been set.  One object without identity is never equal to any other
	* object.
	*
	* @return true if this instance have an identity.
	*/
   protected boolean hasIdentity()
   {
	  boolean ret = true;
	  ret = ret && idHasBeenSet;
	  return ret;
   }

   public boolean equals(Object other)
   {
	  if ( ! hasIdentity() ) return false;
	  if (other instanceof OperationValue)
	  {
		 OperationValue that = (OperationValue) other;
		 if ( ! that.hasIdentity() ) return false;
		 boolean lEquals = true;
		 if( this.id == null )
		 {
			lEquals = lEquals && ( that.id == null );
		 }
		 else
		 {
			lEquals = lEquals && this.id.equals( that.id );
		 }

		 lEquals = lEquals && isIdentical(that);

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public boolean isIdentical(Object other)
   {
	  if (other instanceof OperationValue)
	  {
		 OperationValue that = (OperationValue) other;
		 boolean lEquals = true;
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }

		 return lEquals;
	  }
	  else
	  {
		 return false;
	  }
   }

   public int hashCode(){
	  int result = 17;
      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

	  return result;
   }

}
