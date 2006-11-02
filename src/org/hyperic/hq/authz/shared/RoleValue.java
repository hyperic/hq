/*
 * xdoclet generated file
 * legacy DTO pattern (targeted for replacement with hibernate pojo)
 */
package org.hyperic.hq.authz.shared;

import java.util.Set;

/**
 * Value object for Role.
 *
 */
public class RoleValue
   extends java.lang.Object
   implements java.io.Serializable
{
   private java.lang.String sortName;
   private boolean sortNameHasBeenSet = false;
   private boolean system;
   private boolean systemHasBeenSet = false;
   private String description;
   private boolean descriptionHasBeenSet = false;
   private String name;
   private boolean nameHasBeenSet = false;
   private Integer id;
   private boolean idHasBeenSet = false;
   private Set OperationValues = new java.util.HashSet();

   private org.hyperic.hq.authz.shared.RolePK pk;

   public RoleValue()
   {
	  pk = new org.hyperic.hq.authz.shared.RolePK();
   }

   public RoleValue( java.lang.String sortName,boolean system,String description,String name,Integer id )
   {
	  this.sortName = sortName;
	  sortNameHasBeenSet = true;
	  this.system = system;
	  systemHasBeenSet = true;
	  this.description = description;
	  descriptionHasBeenSet = true;
	  this.name = name;
	  nameHasBeenSet = true;
	  this.id = id;
	  idHasBeenSet = true;
	  pk = new org.hyperic.hq.authz.shared.RolePK(this.getId());
   }

   //TODO Cloneable is better than this !
   public RoleValue( RoleValue otherValue )
   {
	  this.sortName = otherValue.sortName;
	  sortNameHasBeenSet = true;
	  this.system = otherValue.system;
	  systemHasBeenSet = true;
	  this.description = otherValue.description;
	  descriptionHasBeenSet = true;
	  this.name = otherValue.name;
	  nameHasBeenSet = true;
	  this.id = otherValue.id;
	  idHasBeenSet = true;
	// TODO Clone is better no ?
	  this.OperationValues = otherValue.OperationValues;

	  pk = new org.hyperic.hq.authz.shared.RolePK(this.getId());
   }

   public org.hyperic.hq.authz.shared.RolePK getPrimaryKey()
   {
	  return pk;
   }

   public java.lang.String getSortName()
   {
	  return this.sortName;
   }

   public void setSortName( java.lang.String sortName )
   {
	  this.sortName = sortName;
	  sortNameHasBeenSet = true;

   }

   public boolean sortNameHasBeenSet(){
	  return sortNameHasBeenSet;
   }
   public boolean getSystem()
   {
	  return this.system;
   }

   public void setSystem( boolean system )
   {
	  this.system = system;
	  systemHasBeenSet = true;

   }

   public boolean systemHasBeenSet(){
	  return systemHasBeenSet;
   }
   public String getDescription()
   {
	  return this.description;
   }

   public void setDescription( String description )
   {
	  this.description = description;
	  descriptionHasBeenSet = true;

   }

   public boolean descriptionHasBeenSet(){
	  return descriptionHasBeenSet;
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

   protected Set addedOperationValues = new java.util.HashSet();
   protected Set removedOperationValues = new java.util.HashSet();
   protected Set updatedOperationValues = new java.util.HashSet();

   public Set getAddedOperationValues() { return addedOperationValues; }
   public Set getRemovedOperationValues() { return removedOperationValues; }
   public Set getUpdatedOperationValues() { return updatedOperationValues; }

   public org.hyperic.hq.authz.shared.OperationValue[] getOperationValues()
   {
	  return (org.hyperic.hq.authz.shared.OperationValue[])this.OperationValues.toArray(new org.hyperic.hq.authz.shared.OperationValue[OperationValues.size()]);
   }

   public void addOperationValue(org.hyperic.hq.authz.shared.OperationValue added)
   {
	  this.OperationValues.add(added);
	  if ( ! this.addedOperationValues.contains(added))
		 this.addedOperationValues.add(added);
   }

   public void removeOperationValue(org.hyperic.hq.authz.shared.OperationValue removed)
   {
	  this.OperationValues.remove(removed);
	  this.removedOperationValues.add(removed);
	  if (this.addedOperationValues.contains(removed))
		 this.addedOperationValues.remove(removed);
	  if (this.updatedOperationValues.contains(removed))
		 this.updatedOperationValues.remove(removed);
   }

   public void removeAllOperationValues()
   {
        // DOH. Clear the collection - javier 2/24/03
        this.OperationValues.clear();
   }

   public void updateOperationValue(org.hyperic.hq.authz.shared.OperationValue updated)
   {
	  if ( ! this.updatedOperationValues.contains(updated))
		 this.updatedOperationValues.add(updated);
   }

   public void cleanOperationValue(){
	  this.addedOperationValues = new java.util.HashSet();
	  this.removedOperationValues = new java.util.HashSet();
	  this.updatedOperationValues = new java.util.HashSet();
   }

   public void copyOperationValuesFrom(org.hyperic.hq.authz.shared.RoleValue from)
   {
	  // TODO Clone the List ????
	  this.OperationValues = from.OperationValues;
   }

   public String toString()
   {
	  StringBuffer str = new StringBuffer("{");

	  str.append("sortName=" + getSortName() + " " + "system=" + getSystem() + " " + "description=" + getDescription() + " " + "name=" + getName() + " " + "id=" + getId());
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
	  if (other instanceof RoleValue)
	  {
		 RoleValue that = (RoleValue) other;
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
	  if (other instanceof RoleValue)
	  {
		 RoleValue that = (RoleValue) other;
		 boolean lEquals = true;
		 if( this.sortName == null )
		 {
			lEquals = lEquals && ( that.sortName == null );
		 }
		 else
		 {
			lEquals = lEquals && this.sortName.equals( that.sortName );
		 }
		 lEquals = lEquals && this.system == that.system;
		 if( this.description == null )
		 {
			lEquals = lEquals && ( that.description == null );
		 }
		 else
		 {
			lEquals = lEquals && this.description.equals( that.description );
		 }
		 if( this.name == null )
		 {
			lEquals = lEquals && ( that.name == null );
		 }
		 else
		 {
			lEquals = lEquals && this.name.equals( that.name );
		 }
		 if( this.getOperationValues() == null )
		 {
			lEquals = lEquals && ( that.getOperationValues() == null );
		 }
		 else
		 {
            // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
            // - javier 7/16/03
            java.util.Collection cmr1 = java.util.Arrays.asList(this.getOperationValues());
            java.util.Collection cmr2 = java.util.Arrays.asList(that.getOperationValues());
			// lEquals = lEquals && java.util.Arrays.equals(this.getOperationValues() , that.getOperationValues()) ;
            lEquals = lEquals && cmr1.containsAll(cmr2);
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
      result = 37*result + ((this.sortName != null) ? this.sortName.hashCode() : 0);

      result = 37*result + (system ? 0 : 1);

      result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

      result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

      result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

	  result = 37*result + ((this.getOperationValues() != null) ? this.getOperationValues().hashCode() : 0);
	  return result;
   }

}
