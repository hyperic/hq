// ========================================================
// Copyright (c) 2002 Novadeck (France)
// ========================================================
package org.novadeck.jxla.tools;

import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * Defines some constants a pattern compiler and and a pattern matcher used
 *  to parse log lines
 *
 */
public class Constants
{
  public static PatternCompiler COMPILER  = new Perl5Compiler ();
  public static PatternMatcher  MATCHER   = new Perl5Matcher ();
}
