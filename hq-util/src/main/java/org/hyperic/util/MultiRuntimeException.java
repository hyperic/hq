package org.hyperic.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MultiRuntimeException extends RuntimeException {
    
  private static final long serialVersionUID = -7173670617853595611L;
  private static final String NESTED_EXCEPTION_MSG_PREFIX = "--- Next Exception ---" ;
  private static final String CONTEXT_PREFIX = "[Context]: " ;
  
  private final List<ExceptionWrapper> nestedExceptions;

  public MultiRuntimeException() {
      super() ;
      this.nestedExceptions = new ArrayList<ExceptionWrapper>();
  }//EOM 
  
  public MultiRuntimeException(final Throwable t) {
    this() ; 
    this.addThrowable(t) ; 
  }//EOM 

  public MultiRuntimeException(final String message, final Throwable t) {
    super(message);
    this.nestedExceptions = new ArrayList<ExceptionWrapper>();
    this.addThrowable(t) ; 
  }//EOM 

  public final MultiRuntimeException addThrowable(Throwable t) {
    return this.addThrowable(t, null) ; 
  }//EOM 
  
  public final MultiRuntimeException addThrowable(Throwable t, final String message) {
      this.nestedExceptions.add(new ExceptionWrapper(t, message));
      return this;
    }//EOM 
  
  public final boolean isEmpty() { 
      return (this.nestedExceptions == null || nestedExceptions.isEmpty()) ; 
  }//EOM 
  
  public final int size() { return (this.isEmpty() ? 0 : this.nestedExceptions.size()) ;  }//EOM 

  public String getMessage() {
      final String msg = super.getMessage() ; 
      final String origMsg = (msg == null ? "" : msg)  + "\n";
      final StringBuilder builder = new StringBuilder(origMsg).append(this.nestedExceptions.size()).append(" Exception(s) have occured:\n");
      return builder.toString() ; 
  }//EOM 
  
  public String getCompleteMessage(final MessageType enumMessageType) {
    final StringBuilder builder = new StringBuilder() ;
    enumMessageType.appendMessage(this, builder) ; 
    
    final int iLength = this.nestedExceptions.size() ; 
    for(int i=0; i < iLength; i++) { 
        this.getMessage(enumMessageType, this.nestedExceptions.get(i), builder);
        if(i < iLength-1) builder.append("\n--- Next Exception ---\n") ;  
    }//EO while there are more exceptions 

    return builder.toString();
  }//EOM 

  public String toCompleteString() {
      return this.getCompleteMessage(ToString) ;
  }//EOM 
  
  private final StringBuilder getMessage(final MessageType enumMessageType, final ExceptionWrapper wrapper, final StringBuilder builder) {
    final Throwable exception = wrapper.t  ; 
    if(wrapper.message != null) builder.append(CONTEXT_PREFIX).append(wrapper.message).append("\n") ; 
    
    if ((exception instanceof SQLException)) {

      SQLException sqle = (SQLException)exception;
      Throwable t = null;
      do {
        t = sqle;
        enumMessageType.appendMessage(t, builder) ;
                 
        if(((sqle = sqle.getNextException()) != null) && (sqle != t)) builder.append("\n---Next SQL Exception---\n") ; 
        else break ; 
      }while (true);
      
    }else { 
        enumMessageType.appendMessage(exception, builder) ;
    }//EO else if new SQLException
    
    return builder ;
  }//EOM 
  
  public final void printStackTrace(final PrintStream ps) {
      this.printStackTrace(ps, StreamHandlerInterface.printStreamStrategy) ;
  }//EOM 

  public final void printSelfStacktrace(final PrintStream ps) { super.printStackTrace(ps) ; }//EOM
  public final void printSelfStacktrace(final PrintWriter pw) { super.printStackTrace(pw) ; }//EOM 
  
  private final <T> void printStackTrace(final T visitor, StreamHandlerInterface<T> strategy) { 
      synchronized (visitor) {
          
          strategy.printParentStackTrace(visitor, this) ; 
          
          Throwable exception = null ; 

          for (ExceptionWrapper wrapper : this.nestedExceptions) {
              
              strategy.println(visitor, NESTED_EXCEPTION_MSG_PREFIX) ; 
          
              if(wrapper.message != null) strategy.println(visitor, CONTEXT_PREFIX + wrapper.message) ; 
              exception = wrapper.t  ; 
               
              if ((exception instanceof SQLException)){
                  SQLException sqle = (SQLException)exception;
                  Throwable t = null;
                  do {
                    t = sqle;
                    
                    strategy.printStackTrace(visitor, t) ; 
                    
                  }while (((sqle = sqle.getNextException()) != null) && (sqle != t));
                } else {
                    strategy.printStackTrace(visitor, exception) ;
                }//EO else if not sqlexception
              
          }//EO while there are more nested exceptions
      }//EO sync block 
  }//EOM 

  
  public final void printStackTrace(final PrintWriter pw) {
      this.printStackTrace(pw, StreamHandlerInterface.printWriterStrategy) ; 
  }//EOM 
   
  public static final MultiRuntimeException newMultiRuntimeException(MultiRuntimeException multiException, final Throwable t) { 
      return newMultiRuntimeException(multiException, t, null/*message*/) ; 
  }//EOM 
  
  public static final MultiRuntimeException newMultiRuntimeException(MultiRuntimeException multiException, final Throwable t, final String nestedExcpetionMessage) {
    if (multiException == null) {
      if ((t instanceof MultiRuntimeException)) multiException = (MultiRuntimeException)t; else
        multiException = new MultiRuntimeException(t);
    }else multiException.addThrowable(t, nestedExcpetionMessage);

    return multiException;
  }//EOM
  
  private static interface StreamHandlerInterface<T> { 
      
      StreamHandlerInterface<PrintStream> printStreamStrategy = new PrintStreamHandler() ;
      StreamHandlerInterface<PrintWriter> printWriterStrategy = new PrintWriterHandler() ;
      
      void printParentStackTrace(final T visitor, final MultiRuntimeException mre) ; 
      void println(final T visitor, final String line) ;
      void print(final T visitor, final String line) ;
      void printStackTrace(final T visitor, final Throwable throwable) ; 
  }//EOI StreamHandlerInterface
  
  private static final class PrintStreamHandler implements StreamHandlerInterface<PrintStream> { 

      public final void println(final PrintStream visitor, final String line) {
          visitor.println(line) ; 
      }//EOM
      
      public final void print(final PrintStream visitor, final String line) {
          visitor.print(line) ; 
      }//EOM
      
      public final void printStackTrace(final PrintStream visitor, final Throwable throwable) {
          throwable.printStackTrace(visitor) ; 
      }//EOM 
      
      public final void printParentStackTrace(final PrintStream visitor, final MultiRuntimeException mre) { 
          mre.printSelfStacktrace(visitor) ; 
      }//EOM 
      
  }//EO inner class 
  
  private static final class PrintWriterHandler implements StreamHandlerInterface<PrintWriter> { 
      
      public final void println(final PrintWriter visitor, final String line) {
          visitor.println(line) ; 
      }//EOM 
      
      public final void print(final PrintWriter visitor, final String line) {
          visitor.print(line) ; 
      }//EOM 
      
      public final void printStackTrace(final PrintWriter visitor, final Throwable throwable) {
          throwable.printStackTrace(visitor) ; 
      }//EOM 
      
      public final void printParentStackTrace(final PrintWriter visitor, final MultiRuntimeException mre) { 
          mre.printSelfStacktrace(visitor) ; 
      }//EOM 
      
  }//EO inner class PrintWriterHandler
  
  private interface MessageType { 
      StringBuilder appendMessage(final Throwable t, final StringBuilder messageBuilder) ;
  }//EO inner interface MessageType 
  
  final MessageType ToString = new MessageType(){ 
      public final StringBuilder appendMessage(final Throwable t, final StringBuilder messageBuilder) {
          return messageBuilder.append((t instanceof MultiRuntimeException ? 
                 MultiRuntimeException.super.toString() : 
                   t.toString())) ;
      }//EOM 
  };//EO inner class ToString
  
  static final MessageType getMessage = new MessageType(){ 
      public final StringBuilder appendMessage(final Throwable t, final StringBuilder messageBuilder) {
          return messageBuilder.append(t.getMessage()) ; 
      }//EOM 
  };//EO inner class getMessage
  
  private static final class ExceptionWrapper { 
      private Throwable t; 
      private String message ; 
      
      public ExceptionWrapper(final Throwable t, final String message) { 
          this.t = t ; 
          this.message = message ; 
      }//EOM 
  }//EO inner class ExceptionWrapper
  
  
  /*public static void main(String[] args) throws Throwable {
      final StringWriter sw = new StringWriter() ; 
      final PrintWriter writer = new PrintWriter(sw) ; 
      try{ 
          test1() ;
      }catch(Throwable t) { 
          System.out.println("--------------------------------------------------------------------");
          System.out.println("toString");
          System.out.println("--------------------------------------------------------------------");
          System.out.println(((MultiRuntimeException)t).toString());
          
          System.out.println("--------------------------------------------------------------------");
          System.out.println("toCompleteString");
          System.out.println("--------------------------------------------------------------------");
          System.out.println(((MultiRuntimeException)t).toCompleteString());
          
          System.out.println("--------------------------------------------------------------------");
          System.out.println("getMessage");
          System.out.println("--------------------------------------------------------------------");
          System.out.println(((MultiRuntimeException)t).getCompleteMessage(getMessage));
          
          System.out.println("--------------------------------------------------------------------");
          System.out.println("printStackTrace");
          System.out.println("--------------------------------------------------------------------");
          t.printStackTrace(writer) ;
          System.out.println(sw.toString());
          
          
          Thread.currentThread().sleep(300) ; 
          t.printStackTrace() ; 
      }//EO catch block 
  }//EOM 
  
  static void test1() { test2() ; }//EOM  
  static void test2() { test3() ; }//EOM
  static void test3() { test4() ; }//EOM
  static void test4() { 
      final Exception exception = new Exception("this is the exception") ; 
      MultiRuntimeException mre = MultiRuntimeException.newMultiRuntimeException(null, exception) ;

      final SQLException sqle = new SQLException("second") ; 
      sqle.setNextException(new SQLException("second-nested")); 
      
      mre.addThrowable(sqle) ;
      mre.addThrowable(new IllegalStateException("third"), "this is the message 3") ;
      mre.addThrowable(new EncryptionOperationNotPossibleException("fourth"), "this is the message 4") ;
      throw mre ; 
  }//EOM
*/  
}//EOC 
