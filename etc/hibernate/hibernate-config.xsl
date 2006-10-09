<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >
    <xsl:param name="dialect"/>
    <xsl:param name="properties"/>

    <xsl:output
        method="xml"
        indent="yes" 
        doctype-system="http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd"
        doctype-public="-//Hibernate/Hibernate Configuration DTD 3.0//EN" />

    <xsl:template match="/ | *">
        <xsl:copy>
            <xsl:apply-templates select="@*" />
            <xsl:apply-templates select="*" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="session-factory">
      <xsl:copy>
        <xsl:apply-templates mode="properties" select="document($properties)"/>
        <xsl:apply-templates select="*"/>
      </xsl:copy>
    </xsl:template>

    <!-- ok weed out irrelevant dialects -->
    <xsl:template match="*[@dialect]">
      <xsl:apply-templates mode="process-dialect" select=".">
        <xsl:with-param name="context_dialect" select="@dialect" />
      </xsl:apply-templates>
    </xsl:template>

    <!-- we are ony interested in hibernate properties -->
    <xsl:template match="property[not(starts-with(@name, 'hibernate.'))]" priority="2.0" />

    <!-- pass thru all attributes ... -->
    <xsl:template match="@*">
      <xsl:copy/>
    </xsl:template>
    <!-- except these ... -->
    <xsl:template match="@dialect"/>

    <!-- END: main processing loop -->

    <!-- ignore dialect not in context -->
    <xsl:template mode="process-dialect" match="*">
      <xsl:param name="context_dialect" select="@dialect" />
      <xsl:choose>
        <xsl:when test="normalize-space($context_dialect) = $dialect">
          <xsl:apply-templates mode="context-dialect" select ="." />
        </xsl:when>
        <xsl:when test="contains($context_dialect, ',')">
          <xsl:apply-templates mode="process-dialect" select=".">
            <xsl:with-param name="context_dialect" select="substring-before($context_dialect, ',')" />
          </xsl:apply-templates>
          <xsl:apply-templates mode="process-dialect" select=".">
            <xsl:with-param name="context_dialect" select="substring-after($context_dialect, ',')" />
          </xsl:apply-templates>
        </xsl:when>
        <xsl:otherwise />
      </xsl:choose>
    </xsl:template>

    <!-- pass thru the context node and process rest of the children -->
    <xsl:template mode="context-dialect" match="*">
      <xsl:copy>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates select="*" />
      </xsl:copy>
    </xsl:template>

    <!-- process hibernate.properties xml file -->
    <xsl:template mode="properties" match="properties">
      <xsl:variable name="session_factory_name"
                    select="normalize-space(property[@name='hibernate.session_factory_name']/@value)" />
      <xsl:if test="$session_factory_name">
        <xsl:attribute name="name">
          <xsl:value-of select="$session_factory_name"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates mode="properties" select="property[not(starts-with(@name,'mapping.'))]" />
      <xsl:apply-templates mode="mapping" select="property[starts-with(@name,'mapping.')]" />
    </xsl:template>

    <xsl:template mode="properties" match="property">
      <property name="{@name}">
        <xsl:value-of select="@value"/>
      </property>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hq.server.datasource']" priority="3.0">
      <xsl:if test="normalize-space(@value)">
        <property name="connection.datasource">
          <xsl:value-of select="@value"/>
        </property>
      </xsl:if>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hq.jdbc.url']" priority="3.0">
      <xsl:if test="not(normalize-space(../property[@name='hq.server.datasource']/@value))">
        <property name="connection.url">
          <xsl:value-of select="@value"/>
        </property>
      </xsl:if>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hq.jdbc.driver']" priority="3.0">
      <xsl:if test="not(normalize-space(../property[@name='hq.server.datasource']/@value))">
        <property name="connection.driver_class">
          <xsl:value-of select="@value"/>
        </property>
      </xsl:if>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hq.jdbc.user']" priority="3.0">
      <property name="connection.username">
        <xsl:value-of select="@value"/>
      </property>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hq.jdbc.password']" priority="3.0">
      <property name="connection.password">
        <xsl:value-of select="@value"/>
      </property>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hibernate.transaction.manager_lookup_class']" priority="3.0">
      <xsl:if test="normalize-space(@value)">
        <property name="{@name}">
          <xsl:value-of select="@value"/>
        </property>
      </xsl:if>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hq.jta.UserTransaction']" priority="3.0">
      <xsl:if test="normalize-space(@value)">
        <property name="jta.UserTransaction">
          <xsl:value-of select="@value"/>
        </property>
      </xsl:if>
    </xsl:template>

    <xsl:template mode="properties" match="property[@name='hibernate.session_factory_name']" priority="2.0" />
    <xsl:template mode="properties" match="property[not(starts-with(@name, 'hibernate.'))]" priority="2.0" />

    <xsl:template mode="mapping" match="*">
      <mapping resource="{@value}"/>
    </xsl:template>
    <!-- END: process hibernate.properties xml file -->

</xsl:stylesheet>
