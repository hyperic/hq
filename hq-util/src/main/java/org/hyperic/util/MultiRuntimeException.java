package org.hyperic.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MultiRuntimeException extends RuntimeException {
    
  private static final long serialVersionUID = -7173670617853595611L;
  private static final String NESTED_EXCEPTION_MSG_PREFIX = "--- Nested Exception ---" ;
  
  private final List<Throwable> nestedExceptions;

  public MultiRuntimeException(final Throwable t) {
    this.nestedExceptions = new ArrayList<Throwable>();
    this.nestedExceptions.add(t);
  }//EOM 

  public MultiRuntimeException(final String message, final Throwable t) {
    super(message);
    this.nestedExceptions = new ArrayList<Throwable>();
    this.nestedExceptions.add(t);
  }//EOM 

  public final MultiRuntimeException addThrowable(Throwable t) {
    this.nestedExceptions.add(t);
    return this;
  }//EOM 

  public String getMessage() {
      final String msg = super.getMessage() ; 
      final String origMsg = (msg == null ? "" : msg)  + "\n";
      final StringBuilder builder = new StringBuilder(origMsg).append(this.nestedExceptions.size()).append(" Excpetion(s) have occured:");
      return builder.toString() ; 
  }//EOM 
  
  public String getCompleteMessage() {
    final StringBuilder builder = new StringBuilder(this.getMessage()) ; 
    for (Throwable nested : this.nestedExceptions) {
      builder.append("\n--- Nested Exception ---\n").append(this.getMessage(nested));
    }//EO while there are more nested exceptions
    return builder.toString();
  }//EOM 

  private final String getMessage(final Throwable exception) {
    if ((exception instanceof SQLException)) {
      final StringBuilder builder = new StringBuilder();

      SQLException sqle = (SQLException)exception;
      Throwable t = null;
      do {
        t = sqle;
        builder.append("\n---Next SQL Exception---\n").append(t.getMessage());
      }while (((sqle = sqle.getNextException()) != null) && (sqle != t));
      return builder.toString();
    }return exception.getMessage();
  }//EOM 
  
  public final void printStackTrace(final PrintStream ps) {
      this.printStackTrace(ps, StreamHandlerInterface.printStreamStrategy) ;
  }//EOM 

  public final void printSelfStacktrace(final PrintStream ps) { super.printStackTrace(ps) ; }//EOM
  public final void printSelfStacktrace(final PrintWriter pw) { super.printStackTrace(pw) ; }//EOM 
  
  private final <T> void printStackTrace(final T visitor, StreamHandlerInterface<T> strategy) { 
      synchronized (visitor) {
          
          strategy.printParentStackTrace(visitor, this) ; 
          
          for (Throwable exception : this.nestedExceptions) {
              
              strategy.println(visitor, NESTED_EXCEPTION_MSG_PREFIX) ; 
          
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
    if (multiException == null) {
      if ((t instanceof MultiRuntimeException)) multiException = (MultiRuntimeException)t; else
        multiException = new MultiRuntimeException(t);
    }else multiException.addThrowable(t);

    return multiException;
  }//EOM
  
  private static interface StreamHandlerInterface<T> { 
      
      StreamHandlerInterface<PrintStream> printStreamStrategy = new PrintStreamHandler() ;
      StreamHandlerInterface<PrintWriter> printWriterStrategy = new PrintWriterHandler() ;
      
      void printParentStackTrace(final T visitor, final MultiRuntimeException mre) ; 
      void println(final T visitor, final String line) ;
      void printStackTrace(final T visitor, final Throwable throwable) ; 
  }//EOI StreamHandlerInterface
  
  private static final class PrintStreamHandler implements StreamHandlerInterface<PrintStream> { 

      public final void println(final PrintStream visitor, final String line) {
          visitor.println(line) ; 
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
      
      public final void printStackTrace(final PrintWriter visitor, final Throwable throwable) {
          throwable.printStackTrace(visitor) ; 
      }//EOM 
      
      public final void printParentStackTrace(final PrintWriter visitor, final MultiRuntimeException mre) { 
          mre.printSelfStacktrace(visitor) ; 
      }//EOM 
      
  }//EO inner class PrintWriterHandler
  
  
  public static void main(String[] args) throws Throwable {
      test1() ; 
  }//EOM 
  
  static void test1() { test2() ; }//EOM  
  static void test2() { test3() ; }//EOM
  static void test3() { test4() ; }//EOM
  static void test4() { 
      final Exception exception = new Exception("this is the exception") ; 
      MultiRuntimeException mre = MultiRuntimeException.newMultiRuntimeException(null, exception) ;
      throw mre ;
  }//EOM
  
}//EOC 
