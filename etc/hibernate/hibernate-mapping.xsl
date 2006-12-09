<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >
    <xsl:param name="dialect"/>

    <xsl:output
        method="xml"
        indent="no" 
        doctype-system="http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd"
        doctype-public="-//Hibernate/Hibernate Mapping DTD 3.0//EN"
        cdata-section-elements="meta" />

    <xsl:template match="/ | *">
        <xsl:copy>
            <xsl:apply-templates select="@*" />
            <xsl:apply-templates select="node()" />
        </xsl:copy>
    </xsl:template>

    <!-- we currently use sets for non-inverse many-to-many
         relationships.  Need to add support for other hibernate collection
         types on as needed basis.
         Never use bags on non-inverse many-to-many relationships as
         non-inverse bags are the worst performing collection for adding
         items ot it. -->
    <xsl:template match="*[many-to-many] | *[composite-element]">
        <xsl:copy>
            <xsl:apply-templates select="@*" />
            <xsl:apply-templates select="node()" />
        </xsl:copy>
    </xsl:template>

    <!-- good idea(?) to lazy load binary attributes, can be quite large.
         Hibernate requires proxy byte code
         instrumentation for lazy property loading as well in addtion to
         setting lazy="true". Otherwise, lazy="true" has no effect.
         -->
    <xsl:template match="property[@type='binary']">
        <xsl:copy>
            <xsl:attribute name="lazy">
                <xsl:value-of select="'true'" />
            </xsl:attribute>
            <xsl:apply-templates mode="binary-property" select="@*" />
            <xsl:apply-templates select="node()" />
        </xsl:copy>
    </xsl:template>

    <!-- pass thru all binary property attributes -->
    <xsl:template mode="binary-property" match="@*">
        <xsl:apply-templates select="current()"/>
    </xsl:template>
    <!-- except for the lazy attribute -->
    <xsl:template mode="binary-property" match="@lazy" />

    <xsl:template match="bag[many-to-many]">
      <xsl:copy>
        <xsl:attribute name="inverse">
          <xsl:value-of select="'true'"/>
        </xsl:attribute>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates select="node()" />
      </xsl:copy>
    </xsl:template>

    <xsl:template match="*[one-to-many]">
      <xsl:copy>
        <xsl:attribute name="inverse">
          <xsl:value-of select="'true'"/>
        </xsl:attribute>
        <xsl:attribute name="cascade">
          <xsl:value-of select="'save-update,delete,evict,persist,merge'"/>
        </xsl:attribute>
        <xsl:apply-templates mode="key-mode" select="@*" />
        <xsl:apply-templates mode="key-mode" select="node()" />
      </xsl:copy>
    </xsl:template>

    <xsl:template mode="key-mode" match="key">
      <xsl:copy>
        <xsl:attribute name="on-delete">
          <xsl:value-of select="'cascade'"/>
        </xsl:attribute>
        <xsl:apply-templates select="@*" />
        <xsl:apply-templates select="node()" />
      </xsl:copy>
    </xsl:template>

    <!-- join main processing loop -->
    <xsl:template mode="key-mode" match="node() | @*">
      <xsl:apply-templates select="current()"/>
    </xsl:template>
    <!-- except for these -->
    <xsl:template mode="key-mode" match="@cascade" />

  <!--
    disable as we are using optimistic lock with versioning.
  -->
    <xsl:template match="@select-before-update">
      <xsl:attribute name="{name()}">
        <xsl:value-of select="'false'"/>
      </xsl:attribute>
    </xsl:template>

    <!-- pass thru attributes -->
    <xsl:template match="@*">
      <xsl:copy/>
    </xsl:template>
    <!-- except these ... -->
    <xsl:template match="@dialect | @on-delete | @inverse"/>

    <!-- ignore elements and sql dialects not in context -->
    <xsl:template match="*[@dialect]">
      <xsl:apply-templates mode="process-dialect" select=".">
        <xsl:with-param name="context_dialect" select="@dialect" />
      </xsl:apply-templates>
    </xsl:template>

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
        <xsl:apply-templates select="node()" />
      </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
