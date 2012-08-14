package org.hyperic.tools.ant.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.DataType;
import org.hyperic.tools.dbmigrate.TableProcessor.TablesContainer;

public class BigTablesTestTask extends Task{
    
    private BigTables bigTables ;  
    
    public String bigTableRefs ; 
    
    public final void setTableRefs(final String tableRefs) { 
        this.bigTableRefs = tableRefs ; 
    }//EOM 
    
    public final void addConfiguredBigTables(BigTables bigTables) {
        
        if(bigTables.isReference()) {
            Project project = null ;
            String refId = null ; 
            
            do{ 
                project = bigTables.getProject() ; 
                if(project == null) project = this.getProject() ;
                
                refId = bigTables.getRefid().getRefId() ; 
                
                bigTables = (BigTables) project.getReference(refId)  ; 
                
                if(bigTables == null) {
                    this.log("Tables Container reference " + refId + " did not exist, skippping.", Project.MSG_VERBOSE) ;
                    return ; 
                }//EO if tablesContainer was null
            }while(bigTables.isReference()) ;
        }//EO if reference 
        
        System.out.println("adding bigTables container " + bigTables);
        if(bigTables == null) return ; 
        if(this.bigTables == null) this.bigTables = bigTables ;
        else this.bigTables.container.addAll(bigTables.container) ;
    }//EOM 
    
    private final void initBigTables() throws BuildException {
        final Project project = this.getProject() ; 
        
        BigTables bigTablesContainer = null ; 
        for(String tableRef : bigTableRefs.split(",")) { 
            
            bigTablesContainer = (BigTables) project.getReference(tableRef)  ;
            if(bigTablesContainer == null) {
                this.log("Tables Container reference " + tableRef + " did not exist, skippping.", Project.MSG_VERBOSE) ;
                return ; 
            }//EO if tablesContainer was null
            else { 
                if(this.bigTables == null) this.bigTables = bigTablesContainer ; 
                else this.bigTables.container.addAll(bigTablesContainer.container) ;
            }//EO else if ref exists 
            
        }//EO while there are more references 
    }//EOM 
    
    public final void execute() {
        System.out.println("TASK_NAME IS " + this.getTaskName());
        this.initBigTables();
        System.out.println("----------- here");
        for(BigTable bigTable : this.bigTables.container) { 
            System.out.println(bigTable);
        }//EO while 
    }//EOM 
    
    public static final class BigTables extends DataType{ 
        private List<BigTable> container = new ArrayList<BigTable>() ;
        
        public final void addConfiguredBigTable(final BigTable bigTable) {
            this.container.add(bigTable) ; 
            System.out.println(this.toString()) ;   
        }//EOM
        
        @Override
        public String toString() { return this.container.toString() ; }//EOM 
    }//EOM 

    public static final class BigTable { 
        
        private String name ; 
        private int noOfPartitions ; 
        private String partitionColumn ; 
        
        public final void setName(final String tableName) { 
            this.name = tableName ; 
        }//EOM 
        
        public final void setNoOfPartitions(final int noOfPartitions) { 
            this.noOfPartitions = noOfPartitions ; 
        }//EOM 
        
        public final void setPartitionColumn(final String partitionColumn) { 
            this.partitionColumn = partitionColumn ; 
        }//EOM 

        @Override
        public String toString() {
            return "BigTable [tableName=" + name + ", noOfPartitions=" + noOfPartitions + ", partitionColumn="
                    + partitionColumn + "]";
        }//EOM
        
    }//EO inner class BigTable 
}//EOC 
