package org.novadeck.jxla.data;

import org.novadeck.jxla.data.*;

public class LogElem
{
  private String URI;
  private String LogDate;
  private double TimeTaken;
  private Long status;

  public LogElem(Line line)
  {
    URI = line.getURI();
    LogDate = line.getLogDate();
    TimeTaken = line.getTimeTaken();
    status = line.getStatus();
  }

  public String getURI()
  {
    return URI;
  }

  public String getLogDate()
  {
    return LogDate;
  }

  public double getTimeTaken()
  {
    return TimeTaken;
  }

  public Long getStatus()
  {
    return status;
  }
}
    
