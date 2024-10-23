<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:param name="file" select="'unknown'"/>
    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>
    <xsl:template match="/testsuite">
        <xsl:apply-templates select="testcase"/>
    </xsl:template>
    <xsl:template match="testcase">
        <xsl:element name="testcase">
            <xsl:attribute name="file">
                <xsl:value-of select="$file"/>
            </xsl:attribute>
            <xsl:attribute name="classname">
                <xsl:value-of select="@classname"/>
            </xsl:attribute>
            <xsl:attribute name="name">
                <xsl:value-of select="@name"/>
            </xsl:attribute>
            <xsl:attribute name="time">
                <xsl:value-of select="@time"/>
            </xsl:attribute>
            <xsl:copy-of select="skipped"/>
            <xsl:copy-of select="error"/>
            <xsl:copy-of select="system-out"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="*"/>
</xsl:stylesheet>
