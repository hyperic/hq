<?xml version="1.0" encoding="utf-8"?>

    <!-- generate ANI SLQ statements from HQ data.xml -->
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

    <xsl:output
        method="text"
        indent="no"  />

    <xsl:template match="/ | *">
      <xsl:apply-templates select="*" />
    </xsl:template>
  
    <xsl:template match="data">
      <xsl:variable name="update">
        <xsl:apply-templates mode="update-check" select="@*"/>
      </xsl:variable>
      <xsl:apply-templates mode="insert" select="."/>
      <xsl:if test="$update = '1'">
        <!-- not sure so generte both update and insert statement -->
        <xsl:apply-templates mode="update" select="."/>
      </xsl:if>
    </xsl:template>

    <!-- generate insert statement -->
    <xsl:template mode="insert" match="*">
      <xsl:text>insert into </xsl:text>
      <xsl:value-of select="parent::table/@name"/>
      <xsl:text> (</xsl:text>
      <xsl:apply-templates mode="insert-columns" select="@*"/>
      <xsl:text>) values (</xsl:text>
      <xsl:apply-templates mode="insert-values" select="@*"/>
      <xsl:text>);&#10;</xsl:text>
    </xsl:template>

    <xsl:template mode="insert-columns" match="@*">
      <xsl:if test="position() &gt; 1">
        <xsl:value-of select="', '"/>
      </xsl:if>
      <xsl:value-of select="name()" />
    </xsl:template>

    <xsl:template mode="insert-values" match="@*">
      <xsl:if test="position() &gt; 1">
        <xsl:value-of select="', '"/>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="normalize-space(current()) = ''">
          <xsl:value-of select="'null'"/>
        </xsl:when>
        <xsl:when test="starts-with(current(), '%')">
          <xsl:value-of select="substring-after(current(), ':')"/>
        </xsl:when>
        <xsl:when test="string(number(current())) = 'NaN'">
          <xsl:text>'</xsl:text>
          <xsl:value-of select="current()"/>
          <xsl:text>'</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="number(current())" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:template>

    <!-- generate update statement -->
    <xsl:template mode="update" match="*">
      <xsl:text>update </xsl:text>
      <xsl:value-of select="parent::table/@name"/>
      <xsl:text> set </xsl:text>
      <xsl:apply-templates mode="update-set" select="@*"/>
      <xsl:text> where </xsl:text>
      <xsl:apply-templates mode="update-where" select="@*"/>
      <xsl:text>;&#10;</xsl:text>
    </xsl:template>

    <xsl:template mode="update-set" match="@*">
      <xsl:if test="starts-with(current(), '%')">
        <xsl:value-of select="name()"/>
        <xsl:value-of select="concat('=', substring-after(current(), ':'))"/>
        <xsl:if test="position() != last()">
          <xsl:text>, </xsl:text>
        </xsl:if>
      </xsl:if>
    </xsl:template>

    <xsl:template mode="update-where" match="@*">
      <xsl:if test="not(starts-with(current(), '%'))">
        <xsl:value-of select="name()"/>
        <xsl:value-of select="concat('=',current())"/>
      </xsl:if>

    </xsl:template>

    <xsl:template mode="update-check" match="@*">
      <xsl:if test="not(starts-with(current(), '%'))">
        <xsl:value-of select="position()"/>
      </xsl:if>
    </xsl:template>

</xsl:stylesheet>
