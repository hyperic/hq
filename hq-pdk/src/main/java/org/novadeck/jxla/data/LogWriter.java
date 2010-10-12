package org.novadeck.jxla.data;

import java.io.PrintStream;
import java.io.FileOutputStream;
import org.novadeck.jxla.data.*;

public class LogWriter
{
  private PrintStream out = null;

  public LogWriter()
  {
    try {
      out = new PrintStream ( new FileOutputStream ( "results.out" ) );
    } 
    catch (Exception e)
    {
      return;
    }
  }

  public LogWriter(String filename)
  {
    try {
      out = new PrintStream ( new FileOutputStream ( filename ) );
    } 
    catch (Exception e)
    {
      return;
    }
  }

  public void writeToFile(Line line)
  {
    /* Need to make it possible to get the time. */
    out.println(line.getURI () + "\t" + line.getLogDate () + "\t" + 
                line.getTimeTaken () );
  }
}
    
