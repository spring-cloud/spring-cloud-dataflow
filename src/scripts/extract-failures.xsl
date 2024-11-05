<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output media-type="text/plain"/>
    <xsl:template match="/">
        <xsl:for-each select="testsuite/testcase">
            <xsl:if test="(count(rerunError) + count(error)) > 0">
                <xsl:text xml:space="preserve">&#xA;</xsl:text>
                <xsl:value-of select="@classname"/><xsl:text>: </xsl:text><xsl:value-of select="@name"/><xsl:text xml:space="preserve">&#xA;</xsl:text>
                <xsl:for-each select="error">
                    <xsl:text xml:space="preserve">    </xsl:text><xsl:value-of select="@message"/>
                    <xsl:text xml:space="preserve">&#xA;</xsl:text>
                </xsl:for-each>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>